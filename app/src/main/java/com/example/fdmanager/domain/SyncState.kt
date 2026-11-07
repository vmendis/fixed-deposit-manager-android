package com.example.fdmanager.domain

/**
 * #3 Sync-state indicator. Since #10 these states are REAL under a cloud session
 * (FirestoreFdRepository snapshot metadata); the FakeSyncRepository still drives them
 * for the local store and the #3 design-review triggers.
 * State machine:
 * - SAVED: no pending sync (server-acknowledged under cloud)
 * - SYNCING: a write is pending upload
 * - OFFLINE: serving from the on-device cache
 * - ERROR: last sync failed, show Snackbar with Retry (chip shows OFFLINE or SAVED with error flag)
 */
enum class SyncState(val label: String, val description: String) {
    SAVED("Saved", "All changes saved"),
    SYNCING("Syncing…", "Syncing to cloud…"),
    OFFLINE("Offline", "Offline — changes saved on device"),
    ERROR("Sync failed", "Last sync failed — tap Retry");

    companion object {
        fun fromName(name: String): SyncState {
            return entries.find { it.name == name } ?: SAVED
        }
    }
}
