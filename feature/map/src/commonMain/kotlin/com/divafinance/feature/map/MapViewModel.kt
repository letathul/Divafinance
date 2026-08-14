package com.divafinance.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divafinance.core.domain.usecase.location.GetSpendingByLocationUseCase
import com.divafinance.core.domain.usecase.location.TagTransactionLocationUseCase
import com.divafinance.core.model.LocationTag
import com.divafinance.core.model.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LocationSpending(
    val name: String,
    val location: LocationTag,
    val transactions: List<Transaction>,
    val totalAmount: Double,
)

data class MapUiState(
    val locationGroups: List<LocationSpending> = emptyList(),
    val selectedGroup: LocationSpending? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showTagDialog: Boolean = false,
    val tagTransactionId: String? = null,
)

class MapViewModel(
    private val getSpendingByLocation: GetSpendingByLocationUseCase,
    private val tagTransactionLocation: TagTransactionLocationUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        loadSpendingByLocation()
    }

    fun loadSpendingByLocation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val grouped = getSpendingByLocation()
                val locationGroups = grouped.mapNotNull { (name, transactions) ->
                    val firstLocation = transactions.firstNotNullOfOrNull { it.location }
                        ?: return@mapNotNull null
                    LocationSpending(
                        name = name,
                        location = firstLocation,
                        transactions = transactions,
                        totalAmount = transactions.sumOf { it.amount },
                    )
                }.sortedByDescending { it.totalAmount }
                _uiState.value = _uiState.value.copy(
                    locationGroups = locationGroups,
                    isLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load location data",
                )
            }
        }
    }

    fun selectGroup(group: LocationSpending?) {
        _uiState.value = _uiState.value.copy(selectedGroup = group)
    }

    fun showTagDialog(transactionId: String) {
        _uiState.value = _uiState.value.copy(
            showTagDialog = true,
            tagTransactionId = transactionId,
        )
    }

    fun dismissTagDialog() {
        _uiState.value = _uiState.value.copy(
            showTagDialog = false,
            tagTransactionId = null,
        )
    }

    fun tagTransaction(locationName: String, latitude: Double, longitude: Double) {
        val transactionId = _uiState.value.tagTransactionId ?: return
        viewModelScope.launch {
            tagTransactionLocation(
                transactionId,
                LocationTag(
                    latitude = latitude,
                    longitude = longitude,
                    name = locationName,
                ),
            )
            dismissTagDialog()
            loadSpendingByLocation()
        }
    }
}
