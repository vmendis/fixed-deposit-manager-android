package com.example.fdmanager.domain

/**
 * #4 Loading & error state patterns — shared state machine
 * Used by Home, list, detail, calendar; toggled by debug switch for review
 * Mock resolves instantly, but UI must handle Loading / Error / Idle consistently
 * 0 quota, swappable to real Firebase loading states in Phase 2
 */
enum class LoadingState(val label: String, val description: String) {
    IDLE("Idle", "Data loaded"),
    LOADING("Loading", "Fetching FDs..."),
    ERROR("Error", "Failed to load");

    companion object {
        fun fromName(name: String): LoadingState {
            return entries.find { it.name == name.uppercase() } ?: IDLE
        }
    }
}

data class UiState<T>(
    val loadingState: LoadingState = LoadingState.IDLE,
    val data: T? = null,
    val errorMessage: String? = null
) {
    val isLoading: Boolean get() = loadingState == LoadingState.LOADING
    val isError: Boolean get() = loadingState == LoadingState.ERROR
    val isIdle: Boolean get() = loadingState == LoadingState.IDLE
}
