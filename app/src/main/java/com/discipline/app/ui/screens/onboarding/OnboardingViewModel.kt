package com.discipline.app.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.discipline.app.data.model.Habit
import com.discipline.app.data.model.HabitCategory
import com.discipline.app.data.model.HabitType
import com.discipline.app.data.repository.AuthRepository
import com.discipline.app.data.repository.HabitRepository
import com.discipline.app.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class StarterHabitItem(
    val title: String,
    val description: String,
    val category: HabitCategory,
    val type: HabitType,
    val target: Double,
    val unit: String,
    val isSelected: Boolean = false
)

data class OnboardingUiState(
    val currentStep: Int = 0,
    val starterHabits: List<StarterHabitItem> = listOf(
        StarterHabitItem("Read 10 pages", "Read a book every day", HabitCategory.STUDY, HabitType.MEASURABLE, 10.0, "pages", isSelected = true),
        StarterHabitItem("Drink 2 litres of water", "Stay hydrated throughout the day", HabitCategory.HEALTH, HabitType.MEASURABLE, 2.0, "litres", isSelected = true),
        StarterHabitItem("Exercise for 30 minutes", "Daily physical fitness routine", HabitCategory.FITNESS, HabitType.MEASURABLE, 30.0, "min", isSelected = false),
        StarterHabitItem("Sleep before 11 PM", "Healthy circadian sleep routine", HabitCategory.HEALTH, HabitType.YES_NO, 1.0, "", isSelected = false),
        StarterHabitItem("Practice Deep Work", "Focused study without distractions", HabitCategory.PRODUCTIVITY, HabitType.YES_NO, 1.0, "", isSelected = false)
    ),
    val isCompleted: Boolean = false
)

class OnboardingViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val habitRepository: HabitRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun nextStep() {
        _uiState.value = _uiState.value.copy(currentStep = _uiState.value.currentStep + 1)
    }

    fun previousStep() {
        if (_uiState.value.currentStep > 0) {
            _uiState.value = _uiState.value.copy(currentStep = _uiState.value.currentStep - 1)
        }
    }

    fun toggleStarterHabit(index: Int) {
        val list = _uiState.value.starterHabits.toMutableList()
        val item = list[index]
        list[index] = item.copy(isSelected = !item.isSelected)
        _uiState.value = _uiState.value.copy(starterHabits = list)
    }

    fun completeOnboarding(includeStarterHabits: Boolean) {
        viewModelScope.launch {
            val uid = authRepository.currentUser.value?.uid ?: "local_user"
            if (includeStarterHabits) {
                val selected = _uiState.value.starterHabits.filter { it.isSelected }
                for (item in selected) {
                    val habit = Habit(
                        id = UUID.randomUUID().toString(),
                        ownerUid = uid,
                        title = item.title,
                        description = item.description,
                        category = item.category,
                        type = item.type,
                        targetValue = item.target,
                        unit = item.unit,
                        scheduledDays = setOf(1, 2, 3, 4, 5, 6, 7),
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    habitRepository.saveHabit(habit)
                }
            }
            preferencesRepository.setOnboardingCompleted(true)
            _uiState.value = _uiState.value.copy(isCompleted = true)
        }
    }
}
