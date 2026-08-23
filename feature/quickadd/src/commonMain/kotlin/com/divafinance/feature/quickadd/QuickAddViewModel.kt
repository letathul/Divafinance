package com.divafinance.feature.quickadd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divafinance.core.common.ExpressionEvaluator
import com.divafinance.core.common.Coordinates
import com.divafinance.core.common.LocationSource
import com.divafinance.core.common.UuidGenerator
import com.divafinance.core.common.roundToCents
import com.divafinance.core.common.toFixed
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
import com.divafinance.core.model.Currency
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

/**
 * What the sheet is asking the user about location, if anything.
 *
 * One value, not two. This was `CHOICE` (capture always or on tap?) followed by
 * `RATIONALE` (why we want it) as separate dialogs; `LocationBlock` now answers both with
 * a single consent card, and rendered the same card for either value — a distinction the
 * UI could not express.
 */
enum class LocationPrompt {
    /** Why we want the position, shown before the OS prompt rather than instead of it. */
    CONSENT,
}

data class QuickAddUiState(
    val expression: String = "",
    /**
     * Whether the amount sheet is up. The keypad lives behind the amount rather than
     * under it: everything else on this screen has a usable default, so the form is
     * short enough to read whole once the pad is out of the way.
     */
    val calculatorOpen: Boolean = false,
    /** What the entry is denominated in. Seeded from the base currency each time. */
    val currency: String = Currency.USD.code,
    val currencyPickerOpen: Boolean = false,
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
    /**
     * Whether the location block under the detail chips is showing. The chip is the whole
     * entry point, so this doubles as "the user has asked for location on this entry".
     */
    val showLocation: Boolean = false,
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
    /**
     * How many other people share the bill when nobody has been named — the quick
     * "split three ways" path. Naming people takes over from it, so this only ever
     * describes heads the app has no person record for.
     */
    val splitWithCount: Int = 0,
    val peopleSuggestions: List<Person> = emptyList(),
    val isSaving: Boolean = false,
    val error: String? = null,
) {
    /** Running total shown while typing; tolerates a dangling operator. */
    val previewAmount: Double? get() = ExpressionEvaluator.preview(expression)?.roundToCents()

    /** The symbol and name the amount and its picker are labelled with. */
    val currencyInfo: Currency get() = Currency.fromCode(currency)

    /** Capture on every entry, rather than only when the chip is tapped. */
    val autoCaptureLocation: Boolean get() = locationCaptureMode == LocationCaptureMode.ALWAYS

    /**
     * Whether the consent card is what the location block should show.
     *
     * Consent is two questions — do you want this at all, and will the OS allow it — and
     * the block asks them as one card, because to the user they are the same question.
     */
    val locationNeedsConsent: Boolean
        get() = showLocation && location == null && !locationPermissionGranted && !isLocatingNow

    /** The amount that would actually be saved, or null if the entry is not valid. */
    val committedAmount: Double?
        get() = ExpressionEvaluator.evaluate(expression)?.roundToCents()?.takeIf { it > 0.0 }

    val canSave: Boolean get() = committedAmount != null && !isSaving && (!splitEnabled || split != null)

    /**
     * How many people other than the payer are on this bill, named or not. The single
     * number the selector shows, and the one figure the two split paths agree on.
     */
    val splitOthers: Int
        get() = if (!splitEnabled) 0 else maxOf(splitWith.size, splitWithCount)

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
                    splitParticipants(),
                tipPercent = tipPercent,
            )
        }

    /**
     * Everyone but the payer. Named people win outright: an unnamed head is a share the
     * user simply does not want counted as their own, and there is nobody to owe it, so
     * the two kinds are never mixed in one bill.
     */
    private fun splitParticipants(): List<SplitParticipant> =
        if (splitWith.isNotEmpty()) {
            splitWith.map { SplitParticipant(it.personId, it.name) }
        } else {
            // Numbered from 2 because the payer is person 1 on the bill.
            (1..splitWithCount).map { SplitParticipant(personId = null, name = "Person ${it + 1}") }
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
            val baseCurrency = setting(UserSettings.KEY_BASE_CURRENCY) ?: Currency.USD.code
            val captureMode = locationCaptureMode()
            val predicted = predictCategoryUseCase(limit = SUGGESTED_CATEGORY_COUNT)
            val recentMerchants = suggestMerchantsUseCase()
            _uiState.update {
                it.copy(
                    selectedCardId = defaultCardId,
                    currency = baseCurrency,
                    suggestedCategories = predicted,
                    category = predicted.firstOrNull() ?: SpendingCategory.OTHER,
                    merchantSuggestions = recentMerchants,
                    locationCaptureMode = captureMode,
                    // Capturing without being asked has to be visible, or the entry
                    // acquires a place the user never saw it acquire.
                    showLocation = it.showLocation || captureMode == LocationCaptureMode.ALWAYS,
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

    /** `(` and `)`. The evaluator reads "2(3+4)" as multiplication, so no rule is needed here. */
    fun onGroup(char: Char) = appendToExpression(char.toString())

    /**
     * Folds the running result back into the expression, so the next key continues from
     * the total rather than from the sum that produced it. A no-op while the expression
     * is unfinished — there is nothing to fold in yet.
     */
    fun onEquals() {
        val result = ExpressionEvaluator.evaluate(_uiState.value.expression)?.roundToCents() ?: return
        val text = if (result % 1.0 == 0.0) result.toLong().toString() else result.toFixed(2)
        _uiState.update { it.copy(expression = text, error = null) }
    }

    fun onToggleCalculator() =
        _uiState.update { it.copy(calculatorOpen = !it.calculatorOpen, currencyPickerOpen = false) }

    fun onCalculatorDismissed() = _uiState.update { it.copy(calculatorOpen = false) }

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

    fun onToggleCurrencyPicker() =
        _uiState.update { it.copy(currencyPickerOpen = !it.currencyPickerOpen) }

    /**
     * Denominates this one entry. The base currency is left alone — a trip abroad should
     * not re-label every report on the way home.
     */
    fun onCurrencyChange(code: String) =
        _uiState.update { it.copy(currency = code, currencyPickerOpen = false) }

    fun onCardChange(cardId: String?) = _uiState.update { it.copy(selectedCardId = cardId) }

    fun onDayChange(day: QuickAddDay) = _uiState.update { it.copy(day = day) }

    // --- location -----------------------------------------------------------

    /**
     * The place line was tapped. Which of the three things happens depends only on what
     * has already been settled, so the same tap works as a first-run opt-in, as a
     * permission request, and as a refresh once a fix is already on screen.
     */
    fun onWhereTapped() {
        // Without permission, show the consent card — it is the rationale the OS prompt
        // cannot give. `onLocationAllowed` carries on from there and records the opt-in,
        // so the never-asked and permission-refused cases need no separate handling.
        if (_uiState.value.locationPermissionGranted) {
            beginCapture()
        } else {
            _uiState.update { it.copy(locationPrompt = LocationPrompt.CONSENT) }
        }
    }

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

    /**
     * The location chip, which is the whole entry point for location on this screen.
     *
     * Turning it on with permission already held reads the position straight away —
     * asking again for something already granted is a step for its own sake. Without
     * permission the block shows the consent card instead, and [onLocationAllowed] is
     * what carries on from there.
     */
    fun onLocationChipToggled() {
        val state = _uiState.value
        if (state.showLocation) {
            _uiState.update { it.copy(showLocation = false, locationPrompt = null) }
            return
        }
        _uiState.update { it.copy(showLocation = true) }
        if (state.location == null && state.locationPermissionGranted) beginCapture()
    }

    /**
     * The consent card's Allow.
     *
     * One tap settles both questions the two-dialog flow asks separately: the card is
     * itself the rationale the OS prompt cannot give, so accepting it records the opt-in
     * and launches the system prompt in the same move. [ON_TAP] is the recorded default
     * — capturing on every entry is opt-in from the toggle below, not from here.
     */
    fun onLocationAllowed() {
        val existing = _uiState.value.locationCaptureMode
        if (existing == null) {
            viewModelScope.launch {
                settingsRepository.set(
                    UserSettings.KEY_LOCATION_CAPTURE_MODE,
                    LocationCaptureMode.ON_TAP.name,
                )
            }
        }
        _uiState.update {
            it.copy(
                showLocation = true,
                locationCaptureMode = existing ?: LocationCaptureMode.ON_TAP,
                locationPrompt = null,
                permissionRequestNonce = it.permissionRequestNonce + 1,
            )
        }
    }

    /**
     * The consent card's Not now. Nothing is recorded: declining once is not a standing
     * answer, and the chip is there to be tapped again whenever it is wanted.
     */
    fun onLocationDeclined() =
        _uiState.update { it.copy(showLocation = false, locationPrompt = null) }

    /**
     * The auto-capture toggle under a captured place. Writes straight through rather than
     * going via [onLocationAllowed], which would re-request a permission already granted.
     */
    fun onAutoCaptureChanged(enabled: Boolean) {
        val mode = if (enabled) LocationCaptureMode.ALWAYS else LocationCaptureMode.ON_TAP
        viewModelScope.launch {
            settingsRepository.set(UserSettings.KEY_LOCATION_CAPTURE_MODE, mode.name)
        }
        _uiState.update { it.copy(locationCaptureMode = mode) }
    }

    /**
     * Takes the place off this entry entirely.
     *
     * Cancels any read still in flight, or a fix landing a moment later would put back the
     * place the user just removed. `locationUnavailable` clears with it: nothing failed
     * here, so the line goes back to asking rather than apologising.
     */
    fun onLocationCleared() {
        locationJob?.cancel()
        _uiState.update {
            it.copy(
                isLocatingNow = false,
                showLocation = false,
                location = null,
                locationName = "",
                locationNameEdited = false,
                nearbyPlaces = emptyList(),
                locationUnavailable = false,
            )
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
            currency = previous.currency,
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
                it.copy(
                    splitEnabled = false,
                    splitWith = emptyList(),
                    splitWithCount = 0,
                    tipPercent = 0.0,
                    error = null,
                )
            }
        }
        if (enabled) loadPeopleSuggestions()
    }

    /**
     * The quick path: [others] people share the bill, and none of them need naming.
     *
     * Zero turns splitting off entirely. Picking a number that does not match the named
     * list replaces it, because the number is the more recent statement of intent — but
     * picking the number the named list already adds up to leaves those names alone.
     */
    fun onSplitCountChange(others: Int) {
        val count = others.coerceAtLeast(0)
        if (count == 0) {
            onSplitToggled(false)
            return
        }
        _uiState.update { state ->
            state.copy(
                splitEnabled = true,
                splitWithCount = count,
                splitWith = if (state.splitWith.size == count) state.splitWith else emptyList(),
                error = null,
            )
        }
        loadPeopleSuggestions()
    }

    fun onTipPercentChange(percent: Double) =
        _uiState.update { it.copy(tipPercent = percent.coerceAtLeast(0.0)) }

    /** Adding the same person twice would double their share, so names are deduplicated. */
    fun onAddSplitPerson(name: String, personId: String? = null) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return

        _uiState.update { state ->
            val alreadyThere = state.splitWith.any { it.name.equals(trimmed, ignoreCase = true) }
            if (alreadyThere) {
                state
            } else {
                // The named list is now the whole bill, so the count follows it rather
                // than leaving unnamed heads behind that nobody could be billed for.
                val people = state.splitWith + SplitPerson(personId, trimmed)
                state.copy(splitWith = people, splitWithCount = people.size)
            }
        }
    }

    fun onRemoveSplitPerson(name: String) = _uiState.update { state ->
        val people = state.splitWith.filterNot { it.name.equals(name, ignoreCase = true) }
        state.copy(splitWith = people, splitWithCount = people.size)
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
                currency = state.currency,
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
                if (split != null && state.splitWith.isNotEmpty()) {
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
                    // Either no split at all, or one with nobody named: unnamed heads owe
                    // nothing back, so `othersShare` alone keeps their portion out of the
                    // user's spending without inventing people to hold a debt.
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
