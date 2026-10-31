package com.example.fdmanager.domain

/**
 * #9 Firebase Auth — session gate policy, pure logic (Tier1-testable).
 *
 * Decision record (roadmap #9 discussion, 2026-10-31):
 * - The gate applies to ACCOUNT-HOLDERS ONLY. A user who never created an account
 *   keeps full local access — the opt-in principle from the pre-Firebase decisions
 *   is not reversed by auth arriving.
 * - Sign-out HIDES data screens (records stay on the device, still behind the
 *   PIN/biometric app lock). Nothing is wiped by signing out.
 */
object SessionGate {

    /**
     * @param hasAccountOnDevice true once a real (non-mock) account was created or
     *        signed in on this device — persisted across restarts.
     * @param isLoggedIn current auth session state.
     * @return true when data screens must demand sign-in.
     */
    fun isLocked(hasAccountOnDevice: Boolean, isLoggedIn: Boolean): Boolean =
        hasAccountOnDevice && !isLoggedIn
}
