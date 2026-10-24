package com.discipline.app.ui.screens.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.discipline.app.data.repository.AuthRepository
import com.discipline.app.data.repository.HabitRepository
import com.discipline.app.data.repository.PreferencesRepository
import com.discipline.app.data.repository.StatisticsData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class StatisticsUiState(
    val data: StatisticsData? = null,
    val streakThreshold: Int = 80,
    val isLoading: Boolean = false
)

class StatisticsViewModel(
    private val habitRepository: HabitRepository,
    private val authRepository: AuthRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState(isLoading = true))
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        loadStatistics()
    }

    fun loadStatistics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val uid = authRepository.currentUser.value?.uid ?: "local_user"
            val threshold = preferencesRepository.userPreferencesFlow.first().streakThreshold
            val stats = habitRepository.getStatistics(uid, threshold)
            _uiState.value = StatisticsUiState(
                data = stats,
                streakThreshold = threshold,
                isLoading = false
            )
        }
    }
}
