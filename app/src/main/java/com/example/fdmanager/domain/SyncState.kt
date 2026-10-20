package com.example.fdmanager.domain

/**
 * #3 Sync-state indicator — mock states for design review.
 * Real Firebase sync will replace FakeSyncRepository without layout changes.
 * State machine:
 * - SAVED: local data saved, no pending sync (default)
 * - SYNCING: actively syncing to cloud (mocked via delay)
 * - OFFLINE: device offline, local only
 * - ERROR: last sync failed, show Snackbar with Retry (chip shows OFFLINE or SAVED with error flag)
 */
enum class SyncState(val label: String, val description: String) {
    SAVED("Saved", "All changes saved locally"),
    SYNCING("Syncing…", "Syncing to cloud (mock)"),
    OFFLINE("Offline", "Local only — offline mode"),
    ERROR("Sync failed", "Last sync failed — tap Retry");

    companion object {
        fun fromName(name: String): SyncState {
            return entries.find { it.name == name } ?: SAVED
        }
    }
}
