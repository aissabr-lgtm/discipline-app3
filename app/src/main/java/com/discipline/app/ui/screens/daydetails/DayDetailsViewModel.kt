package com.discipline.app.ui.screens.daydetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.discipline.app.data.model.DailyReflection
import com.discipline.app.data.model.DailyScore
import com.discipline.app.data.model.Habit
import com.discipline.app.data.model.HabitLog
import com.discipline.app.data.repository.AuthRepository
import com.discipline.app.data.repository.HabitRepository
import com.discipline.app.domain.HabitScheduleEvaluator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DayDetailsUiState(
    val date: String = "",
    val dailyScore: DailyScore? = null,
    val completedHabits: List<Pair<Habit, HabitLog?>> = emptyList(),
    val incompleteScheduledHabits: List<Pair<Habit, HabitLog?>> = emptyList(),
    val unscheduledHabits: List<Habit> = emptyList(),
    val reflection: DailyReflection? = null,
    val reflectionText: String = "",
    val moodRating: Int = 0,
    val isSavingReflection: Boolean = false,
    val isLoading: Boolean = false
)

class DayDetailsViewModel(
    private val habitRepository: HabitRepository,
    private val authRepository: AuthRepository,
    private val dateStr: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(DayDetailsUiState(date = dateStr, isLoading = true))
    val uiState: StateFlow<DayDetailsUiState> = _uiState.asStateFlow()

    init {
        loadDayData()
    }

    fun loadDayData() {
        viewModelScope.launch {
            val uid = authRepository.currentUser.value?.uid ?: "local_user"
            val localDate = LocalDate.parse(dateStr)

            val allHabits = habitRepository.getHabitById("") // dummy to warm up
            val habitsList = habitRepository.observeAllHabits(uid)
            val logsMap = habitRepository.getLogsForDate(uid, dateStr)
            val score = habitRepository.getDailyScoreForDate(uid, dateStr)
            val reflection = habitRepository.getReflection(uid, dateStr)

            // Split habits
            habitsList.collect { habits ->
                val scheduled = mutableListOf<Habit>()
                val unscheduled = mutableListOf<Habit>()

                for (h in habits) {
                    if (HabitScheduleEvaluator.isScheduledOnDate(h, localDate)) {
                        scheduled.add(h)
                    } else {
                        unscheduled.add(h)
                    }
                }

                val completed = mutableListOf<Pair<Habit, HabitLog?>>()
                val incomplete = mutableListOf<Pair<Habit, HabitLog?>>()

                for (h in scheduled) {
                    val log = logsMap[h.id]
                    val isDone = when (h.type) {
                        com.discipline.app.data.model.HabitType.YES_NO -> log?.isCompleted == true
                        com.discipline.app.data.model.HabitType.MEASURABLE -> (log?.progressValue ?: 0.0) >= h.targetValue
                    }
                    if (isDone) {
                        completed.add(h to log)
                    } else {
                        incomplete.add(h to log)
                    }
                }

                _uiState.value = _uiState.value.copy(
                    date = dateStr,
                    dailyScore = score,
                    completedHabits = completed,
                    incompleteScheduledHabits = incomplete,
                    unscheduledHabits = unscheduled,
                    reflection = reflection,
                    reflectionText = reflection?.reflectionText ?: "",
                    moodRating = reflection?.moodRating ?: 0,
                    isLoading = false
                )
            }
        }
    }

    fun updateReflectionText(text: String) {
        _uiState.value = _uiState.value.copy(reflectionText = text)
    }

    fun updateMood(rating: Int) {
        _uiState.value = _uiState.value.copy(moodRating = rating)
    }

    fun saveReflection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSavingReflection = true)
            val uid = authRepository.currentUser.value?.uid ?: "local_user"
            val ref = DailyReflection(
                date = dateStr,
                ownerUid = uid,
                reflectionText = _uiState.value.reflectionText,
                moodRating = _uiState.value.moodRating,
                updatedAt = System.currentTimeMillis()
            )
            habitRepository.saveReflection(ref)
            _uiState.value = _uiState.value.copy(reflection = ref, isSavingReflection = false)
        }
    }
}
