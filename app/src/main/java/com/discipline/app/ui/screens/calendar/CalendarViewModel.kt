package com.discipline.app.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.discipline.app.data.model.DailyScore
import com.discipline.app.data.repository.AuthRepository
import com.discipline.app.data.repository.HabitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

enum class CalendarViewMode {
    MONTH,
    WEEK,
    YEAR
}

data class CalendarUiState(
    val selectedYearMonth: YearMonth = YearMonth.now(),
    val viewMode: CalendarViewMode = CalendarViewMode.MONTH,
    val monthScores: Map<String, DailyScore> = emptyMap(),
    val isLoading: Boolean = false
)

class CalendarViewModel(
    private val habitRepository: HabitRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState(isLoading = true))
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadMonthData(_uiState.value.selectedYearMonth)
    }

    fun setViewMode(mode: CalendarViewMode) {
        _uiState.value = _uiState.value.copy(viewMode = mode)
    }

    fun nextMonth() {
        val next = _uiState.value.selectedYearMonth.plusMonths(1)
        loadMonthData(next)
    }

    fun previousMonth() {
        val prev = _uiState.value.selectedYearMonth.minusMonths(1)
        loadMonthData(prev)
    }

    fun loadMonthData(yearMonth: YearMonth) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, selectedYearMonth = yearMonth)
            val uid = authRepository.currentUser.value?.uid ?: "local_user"
            val scores = habitRepository.getDailyScoresForMonth(uid, yearMonth)
            val scoreMap = scores.associateBy { it.date }
            _uiState.value = _uiState.value.copy(
                selectedYearMonth = yearMonth,
                monthScores = scoreMap,
                isLoading = false
            )
        }
    }
}
