package com.divafinance.feature.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divafinance.core.domain.usecase.feed.GenerateDailyInsightUseCase
import com.divafinance.core.domain.usecase.feed.GetFeedPostsUseCase
import com.divafinance.core.model.FeedPost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeedUiState(
    val isGeneratingInsight: Boolean = false,
    val insightGenerated: Boolean = false,
)

class FeedViewModel(
    private val getFeedPostsUseCase: GetFeedPostsUseCase,
    private val generateDailyInsightUseCase: GenerateDailyInsightUseCase,
) : ViewModel() {

    val feedPosts: StateFlow<List<FeedPost>> = getFeedPostsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init {
        generateInsight()
    }

    fun generateInsight() {
        if (_uiState.value.isGeneratingInsight) return
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingInsight = true) }
            try {
                generateDailyInsightUseCase()
                _uiState.update { it.copy(isGeneratingInsight = false, insightGenerated = true) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isGeneratingInsight = false) }
            }
        }
    }
}
