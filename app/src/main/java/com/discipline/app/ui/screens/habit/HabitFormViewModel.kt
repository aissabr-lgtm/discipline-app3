package com.discipline.app.ui.screens.habit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.discipline.app.data.model.Habit
import com.discipline.app.data.model.HabitCategory
import com.discipline.app.data.model.HabitType
import com.discipline.app.data.repository.AuthRepository
import com.discipline.app.data.repository.HabitRepository
import com.discipline.app.notification.NotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class HabitFormUiState(
    val habitId: String? = null,
    val title: String = "",
    val description: String = "",
    val category: HabitCategory = HabitCategory.PERSONAL,
    val customCategory: String = "",
    val type: HabitType = HabitType.YES_NO,
    val targetValueText: String = "1",
    val unit: String = "",
    val scheduledDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),
    val hasReminder: Boolean = false,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val isPaused: Boolean = false,
    val isArchived: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

class HabitFormViewModel(
    private val habitRepository: HabitRepository,
    private val authRepository: AuthRepository,
    private val notificationScheduler: NotificationScheduler,
    private val initialHabitId: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitFormUiState(habitId = initialHabitId))
    val uiState: StateFlow<HabitFormUiState> = _uiState.asStateFlow()

    init {
        if (!initialHabitId.isNullOrBlank()) {
            loadHabit(initialHabitId)
        }
    }

    private fun loadHabit(id: String) {
        viewModelScope.launch {
            val habit = habitRepository.getHabitById(id) ?: return@launch
            _uiState.value = HabitFormUiState(
                habitId = habit.id,
                title = habit.title,
                description = habit.description,
                category = habit.category,
                customCategory = habit.customCategory ?: "",
                type = habit.type,
                targetValueText = if (habit.targetValue % 1.0 == 0.0) habit.targetValue.toInt().toString() else habit.targetValue.toString(),
                unit = habit.unit,
                scheduledDays = habit.scheduledDays,
                hasReminder = habit.hasReminder,
                reminderHour = habit.reminderHour ?: 9,
                reminderMinute = habit.reminderMinute ?: 0,
                isPaused = habit.isPaused,
                isArchived = habit.isArchived
            )
        }
    }

    fun setTitle(value: String) { _uiState.value = _uiState.value.copy(title = value, errorMessage = null) }
    fun setDescription(value: String) { _uiState.value = _uiState.value.copy(description = value) }
    fun setCategory(category: HabitCategory) { _uiState.value = _uiState.value.copy(category = category) }
    fun setCustomCategory(value: String) { _uiState.value = _uiState.value.copy(customCategory = value) }
    fun setType(type: HabitType) { _uiState.value = _uiState.value.copy(type = type) }
    fun setTargetValue(value: String) { _uiState.value = _uiState.value.copy(targetValueText = value) }
    fun setUnit(value: String) { _uiState.value = _uiState.value.copy(unit = value) }
    fun toggleDay(dayOfWeek: Int) {
        val current = _uiState.value.scheduledDays.toMutableSet()
        if (current.contains(dayOfWeek)) {
            if (current.size > 1) { // must keep at least 1 day
                current.remove(dayOfWeek)
            }
        } else {
            current.add(dayOfWeek)
        }
        _uiState.value = _uiState.value.copy(scheduledDays = current)
    }
    fun selectAllDays() {
        _uiState.value = _uiState.value.copy(scheduledDays = setOf(1, 2, 3, 4, 5, 6, 7))
    }
    fun setHasReminder(enabled: Boolean) { _uiState.value = _uiState.value.copy(hasReminder = enabled) }
    fun setReminderTime(hour: Int, minute: Int) {
        _uiState.value = _uiState.value.copy(reminderHour = hour, reminderMinute = minute)
    }
    fun setPaused(paused: Boolean) { _uiState.value = _uiState.value.copy(isPaused = paused) }
    fun setArchived(archived: Boolean) { _uiState.value = _uiState.value.copy(isArchived = archived) }

    fun saveHabit() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Habit title cannot be empty")
            return
        }

        val targetNum = state.targetValueText.toDoubleOrNull()
        if (state.type == HabitType.MEASURABLE && (targetNum == null || targetNum <= 0.0)) {
            _uiState.value = state.copy(errorMessage = "Measurable target must be greater than 0")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val uid = authRepository.currentUser.value?.uid ?: "local_user"

            val habit = Habit(
                id = state.habitId ?: UUID.randomUUID().toString(),
                ownerUid = uid,
                title = state.title.trim(),
                description = state.description.trim(),
                category = state.category,
                customCategory = if (state.category == HabitCategory.CUSTOM) state.customCategory.trim() else null,
                type = state.type,
                targetValue = if (state.type == HabitType.YES_NO) 1.0 else (targetNum ?: 1.0),
                unit = if (state.type == HabitType.YES_NO) "" else state.unit.trim(),
                scheduledDays = state.scheduledDays,
                reminderHour = if (state.hasReminder) state.reminderHour else null,
                reminderMinute = if (state.hasReminder) state.reminderMinute else null,
                isPaused = state.isPaused,
                isArchived = state.isArchived,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            habitRepository.saveHabit(habit)
            if (habit.hasReminder) {
                notificationScheduler.scheduleHabitReminder(habit)
            } else {
                notificationScheduler.cancelHabitReminder(habit.id)
            }

            _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true)
        }
    }

    fun deleteHabit() {
        val id = _uiState.value.habitId ?: return
        viewModelScope.launch {
            val uid = authRepository.currentUser.value?.uid ?: "local_user"
            notificationScheduler.cancelHabitReminder(id)
            habitRepository.deleteHabit(id, uid)
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }
}
