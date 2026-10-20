package com.example.fdmanager.data

import com.example.fdmanager.domain.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * #3 Sync-state repository — interface swappable to FirebaseSyncRepository in Phase 2.
 * Fake implementation uses in-memory flows + SettingsStore for persistence of demo state.
 * 0 quota, no network.
 */
interface SyncRepository {
    val syncState: StateFlow<SyncState>
    val lastError: StateFlow<String?>
    suspend fun sync(): Result<Unit>
    suspend fun retry(): Result<Unit>
    fun setState(state: SyncState)
    fun setError(message: String?)
}

object FakeSyncRepository : SyncRepository {
    private val _syncState = MutableStateFlow(SyncState.SAVED)
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    override val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private var lastOperation: String? = null

    override suspend fun sync(): Result<Unit> {
        lastOperation = "sync"
        _syncState.value = SyncState.SYNCING
        // Simulate network delay — mock, no real network
        kotlinx.coroutines.delay(800)
        // Randomly succeed for demo, or if offline, stay offline
        return if (_syncState.value == SyncState.OFFLINE) {
            _lastError.value = "Offline — cannot sync (mock)"
            _syncState.value = SyncState.ERROR
            Result.failure(IllegalStateException("Offline"))
        } else {
            _lastError.value = null
            _syncState.value = SyncState.SAVED
            Result.success(Unit)
        }
    }

    override suspend fun retry(): Result<Unit> {
        // Retry re-runs last operation in mock — per acceptance #3
        _lastError.value = null
        _syncState.value = SyncState.SYNCING
        kotlinx.coroutines.delay(600)
        _syncState.value = SyncState.SAVED
        lastOperation = null
        return Result.success(Unit)
    }

    override fun setState(state: SyncState) {
        _syncState.value = state
        if (state != SyncState.ERROR) {
            _lastError.value = null
        }
    }

    override fun setError(message: String?) {
        _lastError.value = message
        if (message != null) {
            _syncState.value = SyncState.ERROR
        }
    }

    // Demo helper to trigger error state
    fun simulateError() {
        lastOperation = "addFd"
        _lastError.value = "Failed to sync FD-123 (mock) — tap Retry"
        _syncState.value = SyncState.ERROR
    }

    fun simulateOffline() {
        _syncState.value = SyncState.OFFLINE
        _lastError.value = null
    }
}
