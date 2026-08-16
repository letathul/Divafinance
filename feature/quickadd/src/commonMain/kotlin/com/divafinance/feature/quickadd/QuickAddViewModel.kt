package com.divafinance.feature.quickadd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divafinance.core.common.ExpressionEvaluator
import com.divafinance.core.common.Coordinates
import com.divafinance.core.common.LocationSource
import com.divafinance.core.common.UuidGenerator
import com.divafinance.core.common.roundToCents
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
    /** Off until the user turns it on. Location is never captured silently. */
    val locationEnabled: Boolean = false,
    val isLocatingNow: Boolean = false,
    /** Captured position, if any. The name is freely editable afterwards. */
    val location: LocationTag? = null,
    val locationName: String = "",
    /** Premium: shops from the user's own history near [location]. */
    val nearbyPlaces: List<NearbyPlace> = emptyList(),
    val locationUnavailable: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
) {
    /** Running total shown while typing; tolerates a dangling operator. */
    val previewAmount: Double? get() = ExpressionEvaluator.preview(expression)?.roundToCents()

    /** The amount that would actually be saved, or null if the entry is not valid. */
    val committedAmount: Double?
        get() = ExpressionEvaluator.evaluate(expression)?.roundToCents()?.takeIf { it > 0.0 }

    val canSave: Boolean get() = committedAmount != null && !isSaving

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
            val predicted = predictCategoryUseCase(limit = SUGGESTED_CATEGORY_COUNT)
            val recentMerchants = suggestMerchantsUseCase()
            _uiState.update {
                it.copy(
                    selectedCardId = defaultCardId,
                    suggestedCategories = predicted,
                    category = predicted.firstOrNull() ?: SpendingCategory.OTHER,
                    merchantSuggestions = recentMerchants,
                )
            }
        }
    }

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
     * Turns location capture on or off for this entry.
     *
     * [permissionGranted] is what the caller learned from the OS prompt. A denial turns
     * the toggle straight back off rather than leaving it on and silently capturing
     * nothing, so the switch always reflects reality.
     */
    fun onLocationToggled(enabled: Boolean, permissionGranted: Boolean) {
        if (!enabled || !permissionGranted) {
            _uiState.update {
                it.copy(
                    locationEnabled = false,
                    isLocatingNow = false,
                    location = null,
                    locationName = "",
                    nearbyPlaces = emptyList(),
                    locationUnavailable = enabled && !permissionGranted,
                )
            }
            return
        }

        _uiState.update {
            it.copy(locationEnabled = true, isLocatingNow = true, locationUnavailable = false)
        }
        captureLocation()
    }

    private fun captureLocation() {
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            val coordinates = runCatching { locationSource.currentCoordinates() }.getOrNull()
            if (coordinates == null) {
                // No fix, no provider, or permission revoked between prompt and read.
                _uiState.update {
                    it.copy(isLocatingNow = false, locationUnavailable = true, locationEnabled = false)
                }
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
                    // Never clobber a name the user has already typed.
                    locationName = it.locationName.ifBlank { described.orEmpty() },
                )
            }

            val places = runCatching { suggestNearbyPlacesUseCase(tag) }.getOrDefault(emptyList())
            _uiState.update { it.copy(nearbyPlaces = places) }
        }
    }

    /** The captured name is a suggestion, not a fact — it stays editable. */
    fun onLocationNameChange(name: String) =
        _uiState.update { it.copy(locationName = name) }

    /**
     * Accepting a nearby shop fills in the merchant too, and re-runs prediction, since a
     * known shop is the strongest category signal available.
     */
    fun onNearbyPlacePicked(place: NearbyPlace) {
        _uiState.update {
            it.copy(
                locationName = place.name,
                merchantName = place.name,
                location = it.location ?: place.location,
            )
        }
        refreshSuggestions()
    }

    fun reset() {
        _uiState.value = QuickAddUiState(cards = cards.value)
        _saved.value = null
    }

    fun consumeSaved() {
        _saved.value = null
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
            val transaction = Transaction(
                id = UuidGenerator.generate(),
                accountId = defaultAccountId(),
                cardId = state.selectedCardId,
                amount = amount,
                currency = setting(UserSettings.KEY_BASE_CURRENCY) ?: "USD",
                category = state.category,
                merchantName = state.merchantName.ifBlank { null },
                note = state.note.ifBlank { null },
                date = dateFor(state.day, now.toLocalDateTime(TimeZone.currentSystemDefault()).date),
                type = state.type,
                location = state.locationForSaving(),
                createdAt = now,
            )

            try {
                addTransactionUseCase(transaction)
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
            _uiState.value = QuickAddUiState(cards = cards.value)
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
