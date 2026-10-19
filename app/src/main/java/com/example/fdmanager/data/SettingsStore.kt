package com.example.fdmanager.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val label: String) {
    SYSTEM("System default"),
    LIGHT("Light"),
    DARK("Dark")
}

data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val notifyLeadDays: Int = 5,
    val biometricEnabled: Boolean = false,
    val hasSeenOnboarding: Boolean = false,
    val isDemoMode: Boolean = false,
    val isMockLoggedIn: Boolean = false,
    val mockEmail: String? = null,
    val syncStateName: String = "SAVED",
    val lastSyncError: String? = null
)

object SettingsStore {
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _state.value = _state.value.copy(themeMode = mode)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(notificationsEnabled = enabled)
    }

    fun setNotifyLeadDays(days: Int) {
        _state.value = _state.value.copy(notifyLeadDays = days)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(biometricEnabled = enabled)
    }

    fun setHasSeenOnboarding(seen: Boolean) {
        _state.value = _state.value.copy(hasSeenOnboarding = seen)
    }

    fun setDemoMode(enabled: Boolean) {
        _state.value = _state.value.copy(isDemoMode = enabled)
    }

    fun setMockLoggedIn(email: String) {
        _state.value = _state.value.copy(isMockLoggedIn = true, mockEmail = email.trim())
    }

    fun clearMockLoggedIn() {
        _state.value = _state.value.copy(isMockLoggedIn = false, mockEmail = null)
    }

    fun setSyncStateName(name: String) {
        _state.value = _state.value.copy(syncStateName = name)
    }

    fun setLastSyncError(error: String?) {
        _state.value = _state.value.copy(lastSyncError = error)
    }
}
