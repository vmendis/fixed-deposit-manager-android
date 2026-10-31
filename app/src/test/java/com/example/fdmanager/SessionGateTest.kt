package com.example.fdmanager

import com.example.fdmanager.domain.SessionGate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * #9 — session-gate policy, Tier1.
 * Decision record: gate applies to account-holders only; never-signed-in users are never locked.
 */
class SessionGateTest {

    @Test
    fun neverSignedInUserIsNeverLocked() {
        assertFalse(SessionGate.isLocked(hasAccountOnDevice = false, isLoggedIn = false))
    }

    @Test
    fun signedInAccountHolderIsNotLocked() {
        assertFalse(SessionGate.isLocked(hasAccountOnDevice = true, isLoggedIn = true))
    }

    @Test
    fun signedOutAccountHolderIsLocked() {
        assertTrue(SessionGate.isLocked(hasAccountOnDevice = true, isLoggedIn = false))
    }

    @Test
    fun loggedInWithoutAccountFlagIsNotLocked() {
        // Transitional state (e.g. fake/mock session): logged in but no persisted account flag.
        assertFalse(SessionGate.isLocked(hasAccountOnDevice = false, isLoggedIn = true))
    }
}
