package com.discipline.app.ui.screens.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.discipline.app.data.model.DailyScore
import com.discipline.app.data.model.Habit
import com.discipline.app.data.model.HabitLog
import com.discipline.app.data.repository.AuthRepository
import com.discipline.app.data.repository.HabitRepository
import com.discipline.app.domain.HabitScheduleEvaluator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TodayUiState(
    val date: LocalDate = LocalDate.now(),
    val scheduledHabits: List<Habit> = emptyList(),
    val logsByHabitId: Map<String, HabitLog> = emptyMap(),
    val dailyScore: DailyScore? = null,
    val isLoading: Boolean = false
)

class TodayViewModel(
    private val habitRepository: HabitRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val currentDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<TodayUiState> = authRepository.currentUser
        .flatMapLatest { user ->
            val uid = user?.uid ?: "local_user"
            currentDate.flatMapLatest { date ->
                val dateStr = date.toString()
                combine(
                    habitRepository.observeActiveHabits(uid),
                    habitRepository.observeLogsForDate(uid, dateStr),
                    habitRepository.observeDailyScore(uid, dateStr)
                ) { habits, logs, score ->
                    val scheduled = HabitScheduleEvaluator.filterScheduledHabits(habits, date)
                    TodayUiState(
                        date = date,
                        scheduledHabits = scheduled,
                        logsByHabitId = logs,
                        dailyScore = score,
                        isLoading = false
                    )
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TodayUiState(isLoading = true)
        )

    fun toggleYesNoHabit(habitId: String) {
        viewModelScope.launch {
            val uid = authRepository.currentUser.value?.uid ?: "local_user"
            val dateStr = currentDate.value.toString()
            habitRepository.toggleYesNoHabit(habitId, dateStr, uid)
        }
    }

    fun updateHabitProgress(habitId: String, newProgress: Double) {
        viewModelScope.launch {
            val uid = authRepository.currentUser.value?.uid ?: "local_user"
            val dateStr = currentDate.value.toString()
            val habit = habitRepository.getHabitById(habitId) ?: return@launch
            val isCompleted = newProgress >= habit.targetValue
            habitRepository.updateHabitProgress(habitId, dateStr, newProgress, isCompleted, uid)
        }
    }
}
