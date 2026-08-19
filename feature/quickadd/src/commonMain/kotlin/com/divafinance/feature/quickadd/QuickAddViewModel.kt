package com.divafinance.feature.quickadd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divafinance.core.common.ExpressionEvaluator
import com.divafinance.core.common.Coordinates
import com.divafinance.core.common.LocationSource
import com.divafinance.core.common.UuidGenerator
import com.divafinance.core.common.roundToCents
import com.divafinance.core.common.toMajorUnits
import com.divafinance.core.common.toMinorUnits
import com.divafinance.core.data.repository.PersonRepository
import com.divafinance.core.domain.engine.BillSplitEngine
import com.divafinance.core.domain.engine.SplitParticipant
import com.divafinance.core.domain.engine.SplitResult
import com.divafinance.core.domain.usecase.people.SaveSplitTransactionUseCase
import com.divafinance.core.domain.usecase.people.SplitShareInput
import com.divafinance.core.model.Person
import com.divafinance.core.data.repository.SettingsRepository
import com.divafinance.core.domain.usecase.cards.GetAllCardsUseCase
import com.divafinance.core.domain.engine.NearbyPlace
import com.divafinance.core.domain.usecase.feed.PostTransactionToFeedUseCase
import com.divafinance.core.domain.usecase.location.SuggestNearbyPlacesUseCase
import com.divafinance.core.domain.usecase.transactions.AddTransactionUseCase
import com.divafinance.core.domain.usecase.transactions.DeleteTransactionUseCase
import com.divafinance.core.domain.usecase.transactions.PredictCategoryUseCase
import com.divafinance.core.domain.usecase.transactions.SuggestMerchantsUseCase
import com.divafinance.core.model.CreditCard
import com.divafinance.core.model.LocationTag
import com.divafinance.core.model.Transaction
import com.divafinance.core.model.UserSettings
import com.divafinance.core.model.enums.LocationCaptureMode
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** How many category chips the sheet offers before the user has to expand the full list. */
private const val SUGGESTED_CATEGORY_COUNT = 5

/** Which day the entry is booked against. Backdating further is the full form's job. */
enum class QuickAddDay(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
}

/** What the sheet is asking the user about location, if anything. */
enum class LocationPrompt {
    /** First run only: capture on every entry, or only when the place line is tapped. */
    CHOICE,

    /** Why we want the position, shown before the OS prompt rather than instead of it. */
    RATIONALE,
}

data class QuickAddUiState(
    val expression: String = "",
    val type: TransactionType = TransactionType.DEBIT,
    val category: SpendingCategory = SpendingCategory.OTHER,
    val suggestedCategories: List<SpendingCategory> = emptyList(),
    val showAllCategories: Boolean = false,
    val merchantName: String = "",
    val merchantSuggestions: List<String> = emptyList(),
    val note: String = "",
    val showDetails: Boolean = false,
    val selectedCardId: String? = null,
    val cards: List<CreditCard> = emptyList(),
    val day: QuickAddDay = QuickAddDay.TODAY,
    /** Set once the user picks a category, after which prediction stops overriding it. */
    val categoryPickedManually: Boolean = false,
    /** Null until the user has been asked; nothing is captured while it is null. */
    val locationCaptureMode: LocationCaptureMode? = null,
    /** The dialog currently in front of the sheet, if any. */
    val locationPrompt: LocationPrompt? = null,
    /** What the last OS prompt answered. Drives whether the rationale is shown again. */
    val locationPermissionGranted: Boolean = false,
    /**
     * Bumped whenever the OS prompt should be launched. The launcher belongs to the
     * screen — Android needs an Activity result contract — so this is how a ViewModel
     * decision reaches it, as a value rather than an event the screen could miss.
     */
    val permissionRequestNonce: Int = 0,
    val isLocatingNow: Boolean = false,
    /** Captured position, if any. The name is freely editable afterwards. */
    val location: LocationTag? = null,
    val locationName: String = "",
    /** Set once the user edits the name, after which a re-read stops overwriting it. */
    val locationNameEdited: Boolean = false,
    /** Premium: shops from the user's own history near [location]. */
    val nearbyPlaces: List<NearbyPlace> = emptyList(),
    val locationUnavailable: Boolean = false,
    /** Off by default; the keypad amount becomes the bill subtotal once on. */
    val splitEnabled: Boolean = false,
    val tipPercent: Double = 0.0,
    /** Other people on the bill. The payer is implicit and always included. */
    val splitWith: List<SplitPerson> = emptyList(),
    val peopleSuggestions: List<Person> = emptyList(),
    val isSaving: Boolean = false,
    val error: String? = null,
) {
    /** Running total shown while typing; tolerates a dangling operator. */
    val previewAmount: Double? get() = ExpressionEvaluator.preview(expression)?.roundToCents()

    /** The amount that would actually be saved, or null if the entry is not valid. */
    val committedAmount: Double?
        get() = ExpressionEvaluator.evaluate(expression)?.roundToCents()?.takeIf { it > 0.0 }

    val canSave: Boolean get() = committedAmount != null && !isSaving && (!splitEnabled || split != null)

    /**
     * The live breakdown, or null when the split cannot be computed.
     *
     * Derived rather than stored so it can never disagree with the amount, the tip or the
     * participant list.
     */
    val split: SplitResult?
        get() {
            if (!splitEnabled) return null
            val subtotal = committedAmount ?: return null
            return BillSplitEngine().split(
                subtotalMinor = subtotal.toMinorUnits(),
                // Index 0 is the payer, which is what makes them absorb the odd penny.
                participants = listOf(SplitParticipant(personId = null, name = "You")) +
                    splitWith.map { SplitParticipant(it.personId, it.name) },
                tipPercent = tipPercent,
            )
        }

    /** What the card is actually charged: the bill plus tip. */
    val splitTotal: Double? get() = split?.totalMinor?.toMajorUnits()

    /** The user's own consumption, which is all that reaches spending reports. */
    val splitOwnShare: Double? get() = split?.payerShareMinor?.toMajorUnits()

    /**
     * What gets persisted: the captured fix, with whatever name the user settled on.
     *
     * A name without coordinates is deliberately not storable. `LocationTag` requires both,
     * the mapper drops rows missing either, and `selectWithLocation` filters on non-null
     * coordinates — so synthesising `(0.0, 0.0)` to carry a bare name would put a false
     * point in the Atlantic on the spending map. The name field is only offered once a fix
     * exists, so there is nothing to lose here.
     */
    fun locationForSaving(): LocationTag? {
        val fix = location ?: return null
        return fix.copy(name = locationName.trim().takeIf { it.isNotEmpty() } ?: fix.name)
    }
}

/** Someone else on a split bill. [personId] is null until they are matched or created. */
data class SplitPerson(
    val personId: String?,
    val name: String,
)

/** Emitted once per successful save so the sheet can offer an undo. */
data class QuickAddSaved(
    val transactionId: String,
    val amount: Double,
    val category: SpendingCategory,
)

/**
 * Owns the quick-add sheet.
 *
 * Separate from `TransactionsViewModel` deliberately: that one is hoisted at NavHost scope
 * and shares a single form state between the list screen and the full add screen, relying
 * on the caller to call `resetForm()` first. The sheet is reachable from anywhere, so it
 * owns its state and clears it itself.
 */
class QuickAddViewModel(
    private val addTransactionUseCase: AddTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val predictCategoryUseCase: PredictCategoryUseCase,
    private val suggestMerchantsUseCase: SuggestMerchantsUseCase,
    getAllCardsUseCase: GetAllCardsUseCase,
    private val postTransactionToFeedUseCase: PostTransactionToFeedUseCase,
    private val suggestNearbyPlacesUseCase: SuggestNearbyPlacesUseCase,
    private val saveSplitTransactionUseCase: SaveSplitTransactionUseCase,
    private val personRepository: PersonRepository,
    private val locationSource: LocationSource,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickAddUiState())
    val uiState: StateFlow<QuickAddUiState> = _uiState.asStateFlow()

    private val _saved = MutableStateFlow<QuickAddSaved?>(null)
    val saved: StateFlow<QuickAddSaved?> = _saved.asStateFlow()

    /** Cancelled on each keystroke so only the latest merchant query lands. */
    private var suggestionJob: Job? = null

    /** Cancelled when the toggle flips, so an abandoned fix cannot land later. */
    private var locationJob: Job? = null

    private val cards: StateFlow<List<CreditCard>> = getAllCardsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            cards.collect { list -> _uiState.update { it.copy(cards = list) } }
        }
    }

    /**
     * Refreshes the parts of the sheet that depend on stored state. Called when the sheet
     * opens rather than in `init` so a default changed in settings takes effect without
     * the ViewModel being recreated.
     */
    fun onOpened() {
        viewModelScope.launch {
            val defaultCardId = setting(UserSettings.KEY_DEFAULT_CARD_ID)
            val captureMode = locationCaptureMode()
            val predicted = predictCategoryUseCase(limit = SUGGESTED_CATEGORY_COUNT)
            val recentMerchants = suggestMerchantsUseCase()
            _uiState.update {
                it.copy(
                    selectedCardId = defaultCardId,
                    suggestedCategories = predicted,
                    category = predicted.firstOrNull() ?: SpendingCategory.OTHER,
                    merchantSuggestions = recentMerchants,
                    locationCaptureMode = captureMode,
                    // ALWAYS was chosen explicitly, so opening the sheet is the ask. The
                    // prompt is silent once permission is held, which it is by then —
                    // the choice dialog walks through the rationale and the OS prompt.
                    permissionRequestNonce = if (captureMode == LocationCaptureMode.ALWAYS) {
                        it.permissionRequestNonce + 1
                    } else {
                        it.permissionRequestNonce
                    },
                )
            }
        }
    }

    /** Unparseable values are treated as never-asked rather than as a capture default. */
    private suspend fun locationCaptureMode(): LocationCaptureMode? =
        setting(UserSettings.KEY_LOCATION_CAPTURE_MODE)
            ?.let { stored -> LocationCaptureMode.entries.firstOrNull { it.name == stored } }

    // --- keypad ------------------------------------------------------------

    fun onDigit(char: Char) = appendToExpression(char.toString())

    fun onOperator(symbol: Char) = appendToExpression(symbol.toString())

    fun onBackspace() {
        _uiState.update { it.copy(expression = it.expression.dropLast(1), error = null) }
    }

    fun onClear() {
        _uiState.update { it.copy(expression = "", error = null) }
    }

    private fun appendToExpression(text: String) {
        _uiState.update { it.copy(expression = it.expression + text, error = null) }
    }

    // --- form ---------------------------------------------------------------

    fun onTypeChange(type: TransactionType) = _uiState.update {
        // A card only makes sense for money going out.
        it.copy(type = type, selectedCardId = if (type == TransactionType.CREDIT) null else it.selectedCardId)
    }

    fun onCategoryChange(category: SpendingCategory) =
        _uiState.update { it.copy(category = category, categoryPickedManually = true) }

    fun onToggleAllCategories() =
        _uiState.update { it.copy(showAllCategories = !it.showAllCategories) }

    fun onToggleDetails() = _uiState.update { it.copy(showDetails = !it.showDetails) }

    /**
     * Typing a merchant both narrows the autocomplete list and re-runs the category
     * prediction, since a known shop is by far the strongest signal available.
     */
    fun onMerchantChange(name: String) {
        _uiState.update { it.copy(merchantName = name) }
        refreshSuggestions()
    }

    /** Accepting a suggestion should behave exactly like having typed it in full. */
    fun onMerchantSuggestionPicked(name: String) {
        _uiState.update { it.copy(merchantName = name) }
        refreshSuggestions()
    }

    /**
     * Re-runs prediction against what has been entered so far. The user's own pick is
     * never overridden — only the offered chips move.
     */
    private fun refreshSuggestions() {
        suggestionJob?.cancel()
        suggestionJob = viewModelScope.launch {
            val state = _uiState.value
            val merchant = state.merchantName.ifBlank { null }
            val predicted = predictCategoryUseCase(
                merchantName = merchant,
                amount = state.committedAmount,
                limit = SUGGESTED_CATEGORY_COUNT,
            )
            val merchants = suggestMerchantsUseCase(state.merchantName)
            _uiState.update {
                it.copy(
                    suggestedCategories = predicted,
                    merchantSuggestions = merchants,
                    // Only move the selection while the user has not made one of their own.
                    category = if (it.categoryPickedManually) {
                        it.category
                    } else {
                        predicted.firstOrNull() ?: it.category
                    },
                )
            }
        }
    }

    fun onNoteChange(note: String) = _uiState.update { it.copy(note = note) }

    fun onCardChange(cardId: String?) = _uiState.update { it.copy(selectedCardId = cardId) }

    fun onDayChange(day: QuickAddDay) = _uiState.update { it.copy(day = day) }

    // --- location -----------------------------------------------------------

    /**
     * The place line was tapped. Which of the three things happens depends only on what
     * has already been settled, so the same tap works as a first-run opt-in, as a
     * permission request, and as a refresh once a fix is already on screen.
     */
    fun onWhereTapped() {
        val state = _uiState.value
        when {
            // Never been asked. Consent to the idea comes before consent to the OS prompt.
            state.locationCaptureMode == null ->
                _uiState.update { it.copy(locationPrompt = LocationPrompt.CHOICE) }

            // Explain before the system dialog, which cannot say why we are asking.
            !state.locationPermissionGranted ->
                _uiState.update { it.copy(locationPrompt = LocationPrompt.RATIONALE) }

            else -> beginCapture()
        }
    }

    /**
     * Records the first-run answer and moves straight on to the rationale — the user has
     * just said they want this, so stopping to make them tap the line again would be a
     * step for its own sake.
     */
    fun onLocationCaptureModeChosen(mode: LocationCaptureMode) {
        viewModelScope.launch {
            settingsRepository.set(UserSettings.KEY_LOCATION_CAPTURE_MODE, mode.name)
        }
        _uiState.update {
            it.copy(locationCaptureMode = mode, locationPrompt = LocationPrompt.RATIONALE)
        }
    }

    /** The user read why we want it. The OS prompt is the screen's to launch. */
    fun onLocationRationaleAccepted() {
        _uiState.update {
            it.copy(
                locationPrompt = null,
                permissionRequestNonce = it.permissionRequestNonce + 1,
            )
        }
    }

    /** Backing out of either dialog leaves the entry exactly as it was. */
    fun onLocationPromptDismissed() = _uiState.update { it.copy(locationPrompt = null) }

    /**
     * What the OS prompt answered. A refusal is recorded rather than retried: the next tap
     * shows the rationale again, which is the only honest thing left to offer once the
     * system has stopped prompting.
     */
    fun onLocationPermissionResult(granted: Boolean) {
        if (!granted) {
            _uiState.update {
                it.copy(
                    locationPermissionGranted = false,
                    isLocatingNow = false,
                    locationUnavailable = true,
                )
            }
            return
        }
        _uiState.update { it.copy(locationPermissionGranted = true) }
        beginCapture()
    }

    private fun beginCapture() {
        _uiState.update { it.copy(isLocatingNow = true, locationUnavailable = false) }
        captureLocation()
    }

    private fun captureLocation() {
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            val coordinates = runCatching { locationSource.currentCoordinates() }.getOrNull()
            if (coordinates == null) {
                // No fix, no provider, or permission revoked between prompt and read. Any
                // earlier fix is kept — a failed refresh is no reason to lose one.
                _uiState.update { it.copy(isLocatingNow = false, locationUnavailable = true) }
                return@launch
            }

            // Reverse geocoding is best-effort — plenty of devices have no backend for it,
            // and a position without a name is still worth keeping.
            val described = runCatching { locationSource.describe(coordinates) }.getOrNull()
            val tag = LocationTag(coordinates.latitude, coordinates.longitude, described)

            _uiState.update {
                it.copy(
                    isLocatingNow = false,
                    location = tag,
                    // A re-read at a new address should rename the entry, but never over
                    // a name the user chose themselves.
                    locationName = if (it.locationNameEdited) it.locationName
                    else described.orEmpty(),
                )
            }

            val places = runCatching { suggestNearbyPlacesUseCase(tag) }.getOrDefault(emptyList())
            _uiState.update { it.copy(nearbyPlaces = places) }
        }
    }

    /** The captured name is a suggestion, not a fact — it stays editable. */
    fun onLocationNameChange(name: String) =
        _uiState.update { it.copy(locationName = name, locationNameEdited = true) }

    /**
     * Accepting a nearby shop fills in the merchant too, and re-runs prediction, since a
     * known shop is the strongest category signal available.
     */
    fun onNearbyPlacePicked(place: NearbyPlace) {
        _uiState.update {
            it.copy(
                locationName = place.name,
                locationNameEdited = true,
                merchantName = place.name,
                location = it.location ?: place.location,
            )
        }
        refreshSuggestions()
    }

    fun reset() {
        _uiState.value = clearedState()
        _saved.value = null
    }

    /**
     * A blank entry that keeps what the user has settled rather than the entry's own data.
     *
     * The capture mode and the granted permission are decisions about the app, not about
     * this expense, and `permissionRequestNonce` has to stay monotonic — restarting it at
     * zero would make the screen's launcher fire on a value it has already handled.
     */
    private fun clearedState(): QuickAddUiState {
        val previous = _uiState.value
        return QuickAddUiState(
            cards = cards.value,
            locationCaptureMode = previous.locationCaptureMode,
            locationPermissionGranted = previous.locationPermissionGranted,
            permissionRequestNonce = previous.permissionRequestNonce,
        )
    }

    fun consumeSaved() {
        _saved.value = null
    }

    // --- split ---------------------------------------------------------------

    /**
     * Turns splitting on or off. The keypad amount becomes the bill subtotal, and the tip
     * is added on top of it — so the figure on the keypad stays the one from the receipt.
     */
    fun onSplitToggled(enabled: Boolean) {
        _uiState.update {
            if (enabled) {
                it.copy(splitEnabled = true, error = null)
            } else {
                it.copy(splitEnabled = false, splitWith = emptyList(), tipPercent = 0.0, error = null)
            }
        }
        if (enabled) loadPeopleSuggestions()
    }

    fun onTipPercentChange(percent: Double) =
        _uiState.update { it.copy(tipPercent = percent.coerceAtLeast(0.0)) }

    /** Adding the same person twice would double their share, so names are deduplicated. */
    fun onAddSplitPerson(name: String, personId: String? = null) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return

        _uiState.update { state ->
            val alreadyThere = state.splitWith.any { it.name.equals(trimmed, ignoreCase = true) }
            if (alreadyThere) state
            else state.copy(splitWith = state.splitWith + SplitPerson(personId, trimmed))
        }
    }

    fun onRemoveSplitPerson(name: String) = _uiState.update { state ->
        state.copy(splitWith = state.splitWith.filterNot { it.name.equals(name, ignoreCase = true) })
    }

    private fun loadPeopleSuggestions() {
        viewModelScope.launch {
            val people = runCatching { personRepository.getActive().first() }.getOrDefault(emptyList())
            _uiState.update { it.copy(peopleSuggestions = people) }
        }
    }

    // --- save / undo --------------------------------------------------------

    fun save() {
        val state = _uiState.value
        val amount = state.committedAmount
        if (amount == null) {
            _uiState.update { it.copy(error = "Enter an amount greater than zero") }
            return
        }
        if (state.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            val now = Clock.System.now()
            val split = state.split

            // A split is charged the whole bill including tip; `othersShare` is what keeps
            // the other people's portions out of the user's own spending totals.
            val chargedAmount = split?.totalMinor?.toMajorUnits() ?: amount
            val othersShare = split?.othersShareMinor?.toMajorUnits() ?: 0.0

            val transaction = Transaction(
                id = UuidGenerator.generate(),
                accountId = defaultAccountId(),
                cardId = state.selectedCardId,
                amount = chargedAmount,
                currency = setting(UserSettings.KEY_BASE_CURRENCY) ?: "USD",
                category = state.category,
                merchantName = state.merchantName.ifBlank { null },
                note = state.note.ifBlank { null },
                date = dateFor(state.day, now.toLocalDateTime(TimeZone.currentSystemDefault()).date),
                type = state.type,
                location = state.locationForSaving(),
                createdAt = now,
                othersShare = othersShare,
            )

            try {
                if (split != null) {
                    // Drops the payer at index 0; only other people become debts.
                    val shares = split.shares.drop(1).map { share ->
                        SplitShareInput(
                            personId = share.participant.personId,
                            name = share.participant.name,
                            amount = share.amountMinor.toMajorUnits(),
                        )
                    }
                    saveSplitTransactionUseCase(transaction, shares)
                } else {
                    addTransactionUseCase(transaction)
                }
            } catch (e: Exception) {
                // Previously this was unguarded, which left the form stuck on "Saving...".
                _uiState.update {
                    it.copy(isSaving = false, error = e.message ?: "Couldn't save that entry")
                }
                return@launch
            }

            // The feed is a nicety; failing to post must not lose the transaction.
            runCatching { postTransactionToFeedUseCase(transaction) }

            _saved.value = QuickAddSaved(transaction.id, transaction.amount, transaction.category)
            _uiState.value = clearedState()
            onOpened()
        }
    }

    /**
     * Reverses the last save.
     *
     * Goes through [DeleteTransactionUseCase] rather than the repository so the card
     * balance that [AddTransactionUseCase] raised is lowered again — deleting the row on
     * its own would leave the card permanently overstated.
     */
    fun undo() {
        val saved = _saved.value ?: return
        _saved.value = null
        viewModelScope.launch {
            runCatching { deleteTransactionUseCase(saved.transactionId) }
        }
    }

    // --- helpers ------------------------------------------------------------

    private suspend fun setting(key: String): String? =
        settingsRepository.get(key)?.takeIf { it.isNotBlank() }

    /**
     * Falls back to the id the old code hardcoded. `InitializeDatabaseUseCase` creates a
     * row with that id on launch, so it resolves to a real account either way.
     */
    private suspend fun defaultAccountId(): String =
        setting(UserSettings.KEY_DEFAULT_ACCOUNT_ID) ?: "default"

    private fun dateFor(day: QuickAddDay, today: LocalDate): LocalDate = when (day) {
        QuickAddDay.TODAY -> today
        QuickAddDay.YESTERDAY -> today.minusDays(1)
    }

}

/** kotlinx-datetime has no `minusDays` on LocalDate in this version. */
private fun LocalDate.minusDays(days: Int): LocalDate =
    LocalDate.fromEpochDays(toEpochDays() - days)
