package com.discipline.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.discipline.app.data.model.ThemeMode
import com.discipline.app.data.model.UserPreferences
import com.discipline.app.data.remote.SupabaseService
import com.discipline.app.data.repository.AuthRepository
import com.discipline.app.data.repository.AuthUser
import com.discipline.app.data.repository.ExportImportRepository
import com.discipline.app.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val currentUser: AuthUser? = null,
    val isSupabaseConfigured: Boolean = false,
    val supabaseUrl: String = "",
    val supabaseAnonKey: String = "",
    val exportDataJson: String? = null,
    val statusMessage: String? = null,
    val isProcessing: Boolean = false
)

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val authRepository: AuthRepository,
    private val exportImportRepository: ExportImportRepository,
    private val supabaseService: SupabaseService
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            isSupabaseConfigured = supabaseService.isConfigured,
            supabaseUrl = supabaseService.supabaseUrl,
            supabaseAnonKey = supabaseService.supabaseAnonKey
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.userPreferencesFlow.collect { prefs ->
                _uiState.value = _uiState.value.copy(preferences = prefs)
            }
        }
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.value = _uiState.value.copy(
                    currentUser = user,
                    isSupabaseConfigured = supabaseService.isConfigured,
                    supabaseUrl = supabaseService.supabaseUrl,
                    supabaseAnonKey = supabaseService.supabaseAnonKey
                )
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setStreakThreshold(threshold: Int) {
        viewModelScope.launch {
            preferencesRepository.setStreakThreshold(threshold)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setNotificationsEnabled(enabled)
        }
    }

    fun updateSupabaseConfig(url: String, anonKey: String) {
        supabaseService.supabaseUrl = url.trim()
        supabaseService.supabaseAnonKey = anonKey.trim()
        _uiState.value = _uiState.value.copy(
            isSupabaseConfigured = supabaseService.isConfigured,
            supabaseUrl = supabaseService.supabaseUrl,
            supabaseAnonKey = supabaseService.supabaseAnonKey,
            statusMessage = "Supabase configuration updated"
        )
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.value = _uiState.value.copy(statusMessage = "Signed out successfully")
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            val result = authRepository.deleteAccount()
            _uiState.value = _uiState.value.copy(
                isProcessing = false,
                statusMessage = if (result.isSuccess) "Account deleted" else "Failed to delete account: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
            )
        }
    }

    fun exportData() {
        viewModelScope.launch {
            val uid = authRepository.currentUser.value?.uid ?: "local_user"
            val json = exportImportRepository.exportDataToJson(uid)
            _uiState.value = _uiState.value.copy(exportDataJson = json)
        }
    }

    fun clearExportData() {
        _uiState.value = _uiState.value.copy(exportDataJson = null)
    }

    fun importData(jsonString: String, replaceExisting: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            val uid = authRepository.currentUser.value?.uid ?: "local_user"
            val result = exportImportRepository.importDataFromJson(jsonString, uid, replaceExisting)
            _uiState.value = _uiState.value.copy(
                isProcessing = false,
                statusMessage = if (result.isSuccess) "Successfully imported ${result.getOrNull()} items" else "Import failed: ${result.exceptionOrNull()?.message}"
            )
        }
    }

    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }
}
