package com.example.fdmanager

import com.example.fdmanager.domain.SyncState
import org.junit.Assert.*
import org.junit.Test

class SyncStateTest {

    @Test
    fun syncStateValues() {
        assertEquals(4, SyncState.entries.size)
        assertTrue(SyncState.entries.any { it.name == "SAVED" })
        assertTrue(SyncState.entries.any { it.name == "SYNCING" })
        assertTrue(SyncState.entries.any { it.name == "OFFLINE" })
        assertTrue(SyncState.entries.any { it.name == "ERROR" })
    }

    @Test
    fun fromNameReturnsCorrect() {
        assertEquals(SyncState.SAVED, SyncState.fromName("SAVED"))
        assertEquals(SyncState.SYNCING, SyncState.fromName("SYNCING"))
        assertEquals(SyncState.OFFLINE, SyncState.fromName("OFFLINE"))
        assertEquals(SyncState.ERROR, SyncState.fromName("ERROR"))
        assertEquals(SyncState.SAVED, SyncState.fromName("UNKNOWN"))
    }

    @Test
    fun labelsArePresent() {
        assertEquals("Saved", SyncState.SAVED.label)
        assertEquals("Syncing…", SyncState.SYNCING.label)
        assertEquals("Offline", SyncState.OFFLINE.label)
        assertEquals("Sync failed", SyncState.ERROR.label)
    }

    @Test
    fun descriptionsPresent() {
        assertTrue(SyncState.SAVED.description.isNotBlank())
        assertTrue(SyncState.SYNCING.description.isNotBlank())
        assertTrue(SyncState.OFFLINE.description.isNotBlank())
        assertTrue(SyncState.ERROR.description.isNotBlank())
    }
}
