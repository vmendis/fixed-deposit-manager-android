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
    val notifyLeadDays: Int = 5,      // spec default: alert 5 days before maturity
    val biometricEnabled: Boolean = false
)

/** In-memory stand-in for per-user preferences (would live in Firestore/user doc later). */
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
}
