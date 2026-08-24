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
import com.divafinance.core.data.repository.CustomCategoryRepository
import com.divafinance.core.data.repository.PersonRepository
import com.divafinance.core.domain.engine.BillSplitEngine
import com.divafinance.core.domain.engine.CardRecommendation
import com.divafinance.core.domain.engine.SplitMethod
import com.divafinance.core.domain.engine.SplitParticipant
import com.divafinance.core.domain.engine.SplitResult
import com.divafinance.core.domain.usecase.people.SaveSplitTransactionUseCase
import com.divafinance.core.domain.usecase.people.SplitShareInput
import com.divafinance.core.model.Person
import com.divafinance.core.data.repository.SettingsRepository
import com.divafinance.core.domain.usecase.cards.GetAllCardsUseCase
import com.divafinance.core.domain.usecase.cards.GetBestCardForCategoryUseCase
import com.divafinance.core.domain.engine.NearbyPlace
import com.divafinance.core.domain.usecase.feed.PostTransactionToFeedUseCase
import com.divafinance.core.domain.usecase.location.SuggestNearbyPlacesUseCase
import com.divafinance.core.domain.usecase.transactions.AddTransactionUseCase
import com.divafinance.core.domain.usecase.transactions.DeleteTransactionUseCase
import com.divafinance.core.domain.usecase.transactions.PredictCategoryUseCase
import com.divafinance.core.domain.usecase.transactions.SuggestMerchantsUseCase
import com.divafinance.core.model.CreditCard
import com.divafinance.core.model.CustomCategory
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
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** How many category chips the sheet offers before the user has to expand the full list. */
private const val SUGGESTED_CATEGORY_COUNT = 5

/** Today, in the device's zone. Read per construction so a session crossing midnight is right. */
internal fun todayDate(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

/** How the bill is divided between the people on it. */
enum class SplitMode(val label: String) {
    EQUALLY("Equally"),
    BY_AMOUNT("By amount"),
    BY_PERCENT("By percent"),
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
    val merchantName: String = "",
    val merchantSuggestions: List<String> = emptyList(),
    val note: String = "",
    val showDetails: Boolean = false,
    val selectedCardId: String? = null,
    val cards: List<CreditCard> = emptyList(),
    /**
     * The day the entry is booked against. Any past date, chosen from the calendar sheet;
     * the future is refused because a transaction that has not happened is not a record.
     */
    val date: LocalDate = todayDate(),
    val dateSheetOpen: Boolean = false,
    /** Which month the calendar is showing. Independent of [date] so paging can browse. */
    val displayedMonth: LocalDate = todayDate(),
    /** Set once the user picks a category, after which prediction stops overriding it. */
    val categoryPickedManually: Boolean = false,
    /** The user's own categories, offered alongside the built-in twelve. */
    val customCategories: List<CustomCategory> = emptyList(),
    /**
     * The custom category picked, if any. [category] still carries its parent, so this is
     * purely what the entry is *labelled* with.
     */
    val customCategoryId: String? = null,
    /**
     * The best card for this category and amount, computed on-device from the user's own
     * cards. Null when there are no cards, or none that can take the charge.
     */
    val bestCard: CardRecommendation? = null,
    /** Free-text labels. The ghost note row is where they are added. */
    val tags: List<String> = emptyList(),
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
    /**
     * Whether the contextual consent sheet is up. Distinct from [locationPrompt], which is
     * the inline card inside the location block; this one gates the first save.
     */
    val locationSheetOpen: Boolean = false,
    /**
     * Whether this entry has already put the consent sheet in front of a save. Set on
     * either answer, so declining costs one interruption rather than one per save.
     */
    val locationAsked: Boolean = false,
    /**
     * Whether the platform can do location at all. False until [QuickAddViewModel.onOpened]
     * has asked, so nothing is ever interrupted by a question about a capability that may
     * not exist.
     */
    val locationSupported: Boolean = false,
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
    val splitSheetOpen: Boolean = false,
    val splitMode: SplitMode = SplitMode.EQUALLY,
    /**
     * Who actually paid, or null for the user. Someone else paying inverts the debt and
     * takes the charge off the user's card entirely — see [SaveSplitTransactionUseCase].
     */
    val splitPaidBy: SplitPerson? = null,
    /**
     * Positional per-person figures for the two manual modes, index 0 being the user.
     * Empty until the user edits one, at which point the whole list is seeded from the
     * even split so there is never a half-filled allocation.
     */
    val splitCustomAmounts: List<Double> = emptyList(),
    val splitPercents: List<Double> = emptyList(),
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

    /**
     * Whether the first save should stop and ask about location.
     *
     * Only when the user has never been asked at all: a stored capture mode, a granted
     * permission, a fix already on the entry, or a platform that cannot do this are all
     * answers, and none of them is worth interrupting a save to re-ask.
     */
    val shouldAskForLocation: Boolean
        get() = locationSupported &&
            !locationAsked &&
            locationCaptureMode == null &&
            location == null &&
            !locationPermissionGranted &&
            !locationUnavailable

    /** The amount that would actually be saved, or null if the entry is not valid. */
    val committedAmount: Double?
        get() = ExpressionEvaluator.evaluate(expression)?.roundToCents()?.takeIf { it > 0.0 }

    val canSave: Boolean get() = committedAmount != null && !isSaving && (!splitEnabled || split != null)

    /** The label the date pill shows: the two recent days by name, anything else by date. */
    fun dateLabel(today: LocalDate = todayDate()): String = when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> "${date.day} ${MonthAbbreviations[date.month.number - 1]}"
    }

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
            val engine = BillSplitEngine()
            val subtotalMinor = subtotal.toMinorUnits()
            val people = allParticipants()

            // The even split is computed first in every mode, and not only as the answer
            // for EQUALLY: the manual modes need the tip-inclusive total to reconcile
            // against, and deriving it here rather than recomputing the tip keeps one
            // rounding path for the figure the card is actually charged.
            val even = engine.split(
                subtotalMinor = subtotalMinor,
                participants = people,
                tipPercent = tipPercent,
                payerIndex = payerIndex,
            ) ?: return null
            if (splitMode == SplitMode.EQUALLY) return even

            return when (splitMode) {
                SplitMode.BY_AMOUNT -> {
                    val amounts = splitCustomAmounts
                    if (amounts.size != people.size) return null
                    engine.split(
                        subtotalMinor = subtotalMinor,
                        participants = people,
                        tipPercent = tipPercent,
                        // Amounts that do not add up to the total are rejected by the
                        // engine, which is exactly what keeps a half-allocated bill
                        // out of the ledger — `canSave` goes false and the sheet says so.
                        method = SplitMethod.ByExactAmounts(amounts.map { it.toMinorUnits() }),
                        payerIndex = payerIndex,
                    )
                }
                SplitMode.BY_PERCENT -> {
                    val percents = splitPercents
                    if (percents.size != people.size) return null
                    if (abs(percents.sum() - 100.0) > PERCENT_TOLERANCE) return null
                    // Percentages are weights, not amounts: handing them to ByShares gets
                    // the engine's largest-remainder allocation, so the shares still add
                    // back up to the exact total instead of drifting a penny per person.
                    engine.split(
                        subtotalMinor = subtotalMinor,
                        participants = people.mapIndexed { index, participant ->
                            participant.copy(weight = (percents[index] * 100.0).roundToInt())
                        },
                        tipPercent = tipPercent,
                        method = SplitMethod.ByShares,
                        payerIndex = payerIndex,
                    )
                }
                SplitMode.EQUALLY -> even
            }
        }

    /** Index 0 is always the user; the rest are the other people on the bill. */
    internal fun allParticipants(): List<SplitParticipant> =
        listOf(SplitParticipant(personId = null, name = "You")) + splitParticipants()

    /**
     * Where the payer sits in [allParticipants]. Zero — the user — unless someone else
     * paid, and zero again if the chosen payer is no longer on the bill.
     */
    val payerIndex: Int
        get() {
            val paidBy = splitPaidBy ?: return 0
            val index = splitWith.indexOfFirst {
                it.name.equals(paidBy.name, ignoreCase = true)
            }
            return if (index >= 0) index + 1 else 0
        }

    /** True when the bill was paid by someone other than the user. */
    val paidByOther: Boolean get() = splitEnabled && payerIndex != 0

    /** The even per-person figures, used to seed a manual allocation. */
    fun evenShares(): List<Double> =
        split?.shares?.map { it.amountMinor.toMajorUnits() } ?: emptyList()

    /**
     * What has actually been allocated so far, against what it must reach.
     *
     * An even split is trivially balanced — the engine did the arithmetic — but the design
     * still shows the running check in every mode, so the reassurance is the same wherever
     * the user is rather than appearing only once something can go wrong. Null until there
     * is an amount to divide.
     */
    val allocation: SplitAllocation?
        get() {
            if (!splitEnabled) return null
            val subtotal = committedAmount ?: return null
            val people = allParticipants()
            val total = BillSplitEngine().split(
                subtotalMinor = subtotal.toMinorUnits(),
                participants = people,
                tipPercent = tipPercent,
                payerIndex = payerIndex,
            )?.totalMinor?.toMajorUnits() ?: return null

            return when (splitMode) {
                SplitMode.BY_AMOUNT -> SplitAllocation(
                    allocated = splitCustomAmounts.sum().roundToCents(),
                    target = total,
                    people = people.size,
                    isPercent = false,
                )
                SplitMode.BY_PERCENT -> SplitAllocation(
                    allocated = splitPercents.sum().roundToCents(),
                    target = 100.0,
                    people = people.size,
                    isPercent = true,
                )
                SplitMode.EQUALLY -> SplitAllocation(
                    allocated = total,
                    target = total,
                    people = people.size,
                    isPercent = false,
                )
            }
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
    val splitOwnShare: Double? get() = split?.ownShareMinor?.toMajorUnits()

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

/**
 * The running "$57.80 of $57.80 allocated" check the manual split modes show.
 *
 * [isPercent] decides how it is rendered, not how it is computed — both modes are the same
 * question of whether the parts add up to the whole.
 */
data class SplitAllocation(
    val allocated: Double,
    val target: Double,
    val people: Int,
    val isPercent: Boolean,
) {
    val isBalanced: Boolean get() = abs(allocated - target) < PERCENT_TOLERANCE
    val remaining: Double get() = (target - allocated).roundToCents()
}

/** Half a percent / half a cent — the shares are rounded, so exact equality is too strict. */
internal const val PERCENT_TOLERANCE = 0.005

internal val MonthAbbreviations = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

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
    private val getBestCardForCategoryUseCase: GetBestCardForCategoryUseCase,
    private val personRepository: PersonRepository,
    private val customCategoryRepository: CustomCategoryRepository,
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

    /** Cancelled on each category/amount change so only the latest ranking lands. */
    private var bestCardJob: Job? = null

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
            val custom = runCatching { customCategoryRepository.getAll().first() }
                .getOrDefault(emptyList())
            val supported = runCatching { locationSource.isAvailable() }.getOrDefault(false)
            val today = todayDate()
            _uiState.update {
                it.copy(
                    selectedCardId = defaultCardId,
                    currency = baseCurrency,
                    suggestedCategories = predicted,
                    category = predicted.firstOrNull() ?: SpendingCategory.OTHER,
                    merchantSuggestions = recentMerchants,
                    customCategories = custom,
                    locationSupported = supported,
                    // Re-read rather than carried: a sheet left open across midnight would
                    // otherwise go on booking entries against yesterday.
                    date = today,
                    displayedMonth = today,
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
            refreshBestCard()
        }
    }

    /**
     * Ranks the user's own cards for the category and amount on screen.
     *
     * Entirely on-device — [GetBestCardForCategoryUseCase] reads the cards and reward rules
     * the user entered themselves. No bank connection, no network call, nothing leaves.
     */
    private fun refreshBestCard() {
        bestCardJob?.cancel()
        bestCardJob = viewModelScope.launch {
            val state = _uiState.value
            if (state.type != TransactionType.DEBIT) {
                _uiState.update { it.copy(bestCard = null) }
                return@launch
            }
            val amount = state.committedAmount ?: 0.0
            val ranked = runCatching { getBestCardForCategoryUseCase(state.category, amount) }
                .getOrDefault(emptyList())
            _uiState.update { it.copy(bestCard = ranked.firstOrNull()) }
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

    fun onTypeChange(type: TransactionType) {
        _uiState.update {
            // A card only makes sense for money going out.
            it.copy(type = type, selectedCardId = if (type == TransactionType.CREDIT) null else it.selectedCardId)
        }
        refreshBestCard()
    }

    /** Picking a built-in clears any custom label that was on the entry. */
    fun onCategoryChange(category: SpendingCategory) {
        _uiState.update {
            it.copy(category = category, customCategoryId = null, categoryPickedManually = true)
        }
        refreshBestCard()
    }

    /**
     * Picking one of the user's own categories. [category] takes its parent, so rewards,
     * prediction and every report keep working on the built-in twelve.
     */
    fun onCustomCategoryChange(custom: CustomCategory) {
        _uiState.update {
            it.copy(
                category = custom.parent,
                customCategoryId = custom.id,
                categoryPickedManually = true,
            )
        }
        refreshBestCard()
    }

    /** Creates a category and selects it in one move — the picker has no other purpose. */
    fun onCreateCustomCategory(name: String, iconKey: String, colorHex: String, parent: SpendingCategory) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val created = CustomCategory(
                id = UuidGenerator.generate(),
                name = trimmed,
                iconKey = iconKey,
                colorHex = colorHex,
                parent = parent,
                createdAt = Clock.System.now(),
            )
            runCatching { customCategoryRepository.insert(created) }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message ?: "Couldn't add that category") }
                    return@launch
                }
            _uiState.update {
                it.copy(
                    customCategories = it.customCategories + created,
                    category = created.parent,
                    customCategoryId = created.id,
                    categoryPickedManually = true,
                )
            }
            refreshBestCard()
        }
    }

    /** The card the best-card chip is offering. Selecting it is an ordinary card change. */
    fun onUseBestCard() {
        val best = _uiState.value.bestCard ?: return
        onCardChange(best.card.id)
    }

    // --- tags ----------------------------------------------------------------

    /** Deduplicated case-insensitively: "Work" and "work" are one label, not two. */
    fun onAddTag(tag: String) {
        val trimmed = tag.trim().replace("\n", " ")
        if (trimmed.isEmpty()) return
        _uiState.update { state ->
            if (state.tags.any { it.equals(trimmed, ignoreCase = true) }) state
            else state.copy(tags = state.tags + trimmed)
        }
    }

    fun onRemoveTag(tag: String) = _uiState.update { state ->
        state.copy(tags = state.tags.filterNot { it.equals(tag, ignoreCase = true) })
    }

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

    // --- date ----------------------------------------------------------------

    fun onDateSheetOpened() = _uiState.update {
        it.copy(dateSheetOpen = true, displayedMonth = it.date, calculatorOpen = false)
    }

    fun onDateSheetDismissed() = _uiState.update { it.copy(dateSheetOpen = false) }

    fun onDisplayedMonthChange(month: LocalDate) =
        _uiState.update { it.copy(displayedMonth = month) }

    /**
     * Selecting closes the sheet in the same move — a date needs no confirming, and a
     * second tap to agree with the first is a step with nothing in it.
     *
     * A future date is ignored rather than clamped: the calendar greys those days out, so
     * this only guards a caller that did not.
     */
    fun onDateChange(date: LocalDate) {
        if (date > todayDate()) return
        _uiState.update { it.copy(date = date, dateSheetOpen = false) }
    }

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
     * The consent sheet's Allow, shown in front of the first save. Opts in and captures,
     * then lets the save it interrupted go through — the fix lands on the entry if it
     * arrives in time and the entry saves regardless if it does not, because location is
     * an enrichment and must never be the reason an entry is lost.
     */
    fun onLocationSheetAllowed() {
        _uiState.update { it.copy(locationSheetOpen = false) }
        onLocationAllowed()
        save()
    }

    /**
     * The consent sheet's Not now: the save it interrupted goes straight through.
     *
     * Unlike the location block's own Not now, this answer is **recorded**. That card is
     * shown because the user tapped the chip and can be tapped again; this sheet arrives
     * unasked in front of a save, so re-raising it on the next entry would be nagging.
     * Settings can still turn it back on.
     */
    fun onLocationSheetDeclined() {
        viewModelScope.launch {
            settingsRepository.set(
                UserSettings.KEY_LOCATION_CAPTURE_MODE,
                LocationCaptureMode.NEVER.name,
            )
        }
        _uiState.update {
            it.copy(locationSheetOpen = false, locationCaptureMode = LocationCaptureMode.NEVER)
        }
        save()
    }

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
            customCategories = previous.customCategories,
            locationCaptureMode = previous.locationCaptureMode,
            locationPermissionGranted = previous.locationPermissionGranted,
            locationSupported = previous.locationSupported,
            permissionRequestNonce = previous.permissionRequestNonce,
            // Carried for the same reason the capture mode is: being asked once is enough,
            // and a fresh blank entry is not a new reason to ask.
            locationAsked = previous.locationAsked,
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
                    splitMode = SplitMode.EQUALLY,
                    splitPaidBy = null,
                    splitCustomAmounts = emptyList(),
                    splitPercents = emptyList(),
                    splitSheetOpen = false,
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

    fun onSplitSheetOpened() {
        _uiState.update { it.copy(splitSheetOpen = true, calculatorOpen = false) }
        if (!_uiState.value.splitEnabled) onSplitToggled(true)
    }

    fun onSplitSheetDismissed() = _uiState.update { it.copy(splitSheetOpen = false) }

    /**
     * Switching mode seeds the manual lists from the even split, so the sheet always opens
     * already balanced and the user adjusts from a correct allocation rather than from
     * zeros they have to make add up before anything works.
     */
    fun onSplitModeChange(mode: SplitMode) = _uiState.update { state ->
        val people = state.allParticipants().size
        when (mode) {
            SplitMode.EQUALLY -> state.copy(
                splitMode = mode,
                splitCustomAmounts = emptyList(),
                splitPercents = emptyList(),
            )
            SplitMode.BY_AMOUNT -> state.copy(
                splitMode = mode,
                splitCustomAmounts = state.evenShares().takeIf { it.size == people }
                    ?: List(people) { 0.0 },
            )
            SplitMode.BY_PERCENT -> state.copy(
                splitMode = mode,
                splitPercents = evenPercents(people),
            )
        }
    }

    /** One person's figure in whichever manual mode is live. Index 0 is the user. */
    fun onSplitShareChange(index: Int, value: Double) = _uiState.update { state ->
        val safe = value.coerceAtLeast(0.0)
        when (state.splitMode) {
            SplitMode.BY_AMOUNT -> {
                val amounts = state.splitCustomAmounts.toMutableList()
                if (index !in amounts.indices) return@update state
                amounts[index] = safe
                state.copy(splitCustomAmounts = amounts)
            }
            SplitMode.BY_PERCENT -> {
                val percents = state.splitPercents.toMutableList()
                if (index !in percents.indices) return@update state
                percents[index] = safe
                state.copy(splitPercents = percents)
            }
            SplitMode.EQUALLY -> state
        }
    }

    /**
     * Chooses who paid. Null is the user.
     *
     * A bill someone else paid is not a charge on any of the user's cards, so the card
     * selection is dropped here rather than at save time — leaving a card selected in the
     * UI while silently ignoring it would be the screen disagreeing with the record.
     */
    fun onSplitPayerChange(person: SplitPerson?) = _uiState.update { state ->
        state.copy(
            splitPaidBy = person,
            selectedCardId = if (person == null) state.selectedCardId else null,
        )
    }

    /** Even percentages that still sum to exactly 100, the remainder going to the payer. */
    private fun evenPercents(people: Int): List<Double> {
        if (people <= 0) return emptyList()
        val each = (10_000.0 / people).toLong()
        val base = MutableList(people) { each / 100.0 }
        val allocated = each * people
        base[0] = ((each + (10_000L - allocated)) / 100.0)
        return base
    }

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
                state.copy(
                    splitWith = people,
                    splitWithCount = people.size,
                    // Positional lists cannot survive the bill changing length.
                    splitCustomAmounts = emptyList(),
                    splitPercents = emptyList(),
                    splitMode = SplitMode.EQUALLY,
                )
            }
        }
    }

    fun onRemoveSplitPerson(name: String) = _uiState.update { state ->
        val people = state.splitWith.filterNot { it.name.equals(name, ignoreCase = true) }
        state.copy(
            splitWith = people,
            splitWithCount = people.size,
            // Removing whoever was paying hands the bill back to the user rather than
            // leaving a payer who is no longer on it.
            splitPaidBy = state.splitPaidBy?.takeIf { paid ->
                people.any { it.name.equals(paid.name, ignoreCase = true) }
            },
            // The lists are positional, so a shorter bill invalidates them outright.
            splitCustomAmounts = emptyList(),
            splitPercents = emptyList(),
            splitMode = if (state.splitMode == SplitMode.EQUALLY) state.splitMode else SplitMode.EQUALLY,
        )
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

        // Location is asked for here rather than on launch, and only once: the first save
        // is the first moment the request has any context to justify it. `locationAsked`
        // is set whether the answer is yes or no, so a decline never gates a second save.
        if (state.shouldAskForLocation) {
            _uiState.update {
                it.copy(locationSheetOpen = true, locationAsked = true, error = null)
            }
            return
        }

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
                // A bill someone else paid never touched the user's card, so it must not
                // raise a card balance — `AddTransactionUseCase` would if a card were set.
                cardId = if (state.paidByOther) null else state.selectedCardId,
                amount = chargedAmount,
                currency = state.currency,
                category = state.category,
                merchantName = state.merchantName.ifBlank { null },
                note = state.note.ifBlank { null },
                date = state.date,
                type = state.type,
                location = state.locationForSaving(),
                createdAt = now,
                othersShare = othersShare,
                tags = state.tags,
                customCategoryId = state.customCategoryId,
            )

            try {
                if (split != null && state.paidByOther) {
                    // Someone else paid: the only debt worth recording is the user's own
                    // share, owed to them. What the rest of the table owes the payer is
                    // between them and the payer.
                    val payer = split.shares.getOrNull(state.payerIndex)?.participant
                    saveSplitTransactionUseCase(
                        transaction = transaction,
                        shares = emptyList(),
                        payer = SplitShareInput(
                            personId = payer?.personId,
                            name = payer?.name.orEmpty(),
                            amount = (transaction.amount - transaction.othersShare).roundToCents(),
                        ),
                    )
                } else if (split != null && state.splitWith.isNotEmpty()) {
                    // Drops the user at index 0; only other people become debts.
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

}

/** kotlinx-datetime has no `minusDays` on LocalDate in this version. */
private fun LocalDate.minusDays(days: Int): LocalDate =
    LocalDate.fromEpochDays(toEpochDays() - days)
