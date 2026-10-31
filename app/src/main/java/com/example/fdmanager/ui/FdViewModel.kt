package com.example.fdmanager.ui

import androidx.lifecycle.ViewModel
import com.example.fdmanager.data.FdRepository
import com.example.fdmanager.data.SampleData
import com.example.fdmanager.data.SettingsState
import com.example.fdmanager.data.SettingsStore
import com.example.fdmanager.data.ThemeMode
import com.example.fdmanager.data.model.FixedDeposit
import com.example.fdmanager.data.model.RenewOption
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

class FdViewModel : ViewModel() {

    private val repo = FdRepository.get()
    // #9 — real FirebaseAuthRepository when configured, fake otherwise (CI, tests, json-less builds)
    private val authRepo get() = com.example.fdmanager.data.AuthRepositoryProvider.get()
    private val syncRepo = com.example.fdmanager.data.FakeSyncRepository

    val fds: StateFlow<List<FixedDeposit>> = repo.fds
    val settings: StateFlow<SettingsState> = SettingsStore.state
    val authState = authRepo.authState
    val syncState = syncRepo.syncState
    val lastSyncError = syncRepo.lastError

    init {
        repo.autoRenewDue()
    }

    fun addFd(fd: FixedDeposit): String = repo.add(fd)
    fun updateFd(fd: FixedDeposit) = repo.update(fd)
    fun softDelete(id: String) = repo.softDelete(id)
    fun restore(id: String) = repo.restore(id)
    fun deleteForever(id: String) = repo.deleteForever(id)
    fun renew(id: String, option: RenewOption? = null): String = repo.renew(id, option)
    fun resetDemoData() = repo.reset(SampleData.seed(LocalDate.now()))

    fun toggleDemoMode(enabled: Boolean) {
        SettingsStore.setDemoMode(enabled)
        repo.setDemoMode(enabled, SampleData.seed(LocalDate.now()))
    }

    fun clearAllFds() = repo.clearAll()

    /**
     * #5 trust UX — apply the chosen wipe categories to the settings store.
     * Deposits are cleared separately by the caller (only when that box was ticked), so this
     * never removes data behind the user's back.
     */
    fun applyDataWipe(selection: com.example.fdmanager.domain.WipeSelection) {
        val next = com.example.fdmanager.domain.DataWipe.applyTo(SettingsStore.state.value, selection)
        SettingsStore.replaceWith(next)
    }

    fun setThemeMode(mode: ThemeMode) = SettingsStore.setThemeMode(mode)
    fun setNotificationsEnabled(enabled: Boolean) = SettingsStore.setNotificationsEnabled(enabled)
    fun setNotifyLeadDays(days: Int) = SettingsStore.setNotifyLeadDays(days)
    fun setBiometricEnabled(enabled: Boolean) = SettingsStore.setBiometricEnabled(enabled)
    fun setHasSeenOnboarding(seen: Boolean) = SettingsStore.setHasSeenOnboarding(seen)
    fun setDemoMode(enabled: Boolean) {
        toggleDemoMode(enabled)
    }

    // #9 — names kept for UI stability; behind them sits the provider-selected repository.
    suspend fun mockSignIn(email: String, password: String): Result<Unit> = authRepo.signIn(email, password)
    suspend fun mockSignUp(email: String, password: String, confirm: String): Result<Unit> = authRepo.signUp(email, password, confirm)
    suspend fun mockSendReset(email: String): Result<Unit> = authRepo.sendPasswordReset(email)
    fun mockSignOut() {
        val repo = authRepo
        kotlinx.coroutines.runBlocking { repo.signOut() }
        SettingsStore.clearMockLoggedIn() // harmless on real path; required on fake path
    }

    /**
     * #9 — Google Sign-In. Fake path: instant mock session (tests/CI, 0 quota).
     * Real path: Credential Manager UI → ID token → Firebase credential sign-in.
     * @param activityContext must be an Activity context (Credential Manager shows UI).
     */
    suspend fun signInWithGoogle(activityContext: android.content.Context): Result<Unit> {
        return if (com.example.fdmanager.data.AuthRepositoryProvider.isUsingFake) {
            authRepo.signInWithGoogleIdToken("fake-token")
        } else {
            com.example.fdmanager.data.GoogleCredentialHelper.fetchGoogleIdToken(activityContext)
                .fold(
                    onSuccess = { token -> authRepo.signInWithGoogleIdToken(token) },
                    onFailure = { Result.failure(it) }
                )
        }
    }

    /** #9 — true when the real Firebase backend is live (drives footer labels). */
    fun isAuthBackendReal(): Boolean = !com.example.fdmanager.data.AuthRepositoryProvider.isUsingFake

    // #3 sync-state — mock, 0 quota, swappable to FirebaseSyncRepository Phase 2
    fun setSyncState(state: com.example.fdmanager.domain.SyncState) {
        syncRepo.setState(state)
        SettingsStore.setSyncStateName(state.name)
    }

    suspend fun triggerSync(): Result<Unit> {
        val res = syncRepo.sync()
        if (res.isFailure) {
            SettingsStore.setLastSyncError(syncRepo.lastError.value)
        } else {
            SettingsStore.setLastSyncError(null)
        }
        return res
    }

    suspend fun retrySync(): Result<Unit> {
        val res = syncRepo.retry()
        SettingsStore.setLastSyncError(null)
        return res
    }

    fun simulateSyncError() {
        syncRepo.simulateError()
        SettingsStore.setLastSyncError(syncRepo.lastError.value)
        SettingsStore.setSyncStateName(com.example.fdmanager.domain.SyncState.ERROR.name)
    }

    fun simulateOffline() {
        syncRepo.simulateOffline()
        SettingsStore.setSyncStateName(com.example.fdmanager.domain.SyncState.OFFLINE.name)
        SettingsStore.setLastSyncError(null)
    }

    fun clearSyncError() {
        syncRepo.setError(null)
        syncRepo.setState(com.example.fdmanager.domain.SyncState.SAVED)
        SettingsStore.setLastSyncError(null)
        SettingsStore.setSyncStateName(com.example.fdmanager.domain.SyncState.SAVED.name)
    }

    // #4 loading & error debug — mock, 0 quota, toggled by debug switch for review
    fun setLoadingDebug(enabled: Boolean) = SettingsStore.setLoadingDebug(enabled)
    fun setErrorDebug(enabled: Boolean) = SettingsStore.setErrorDebug(enabled)
    fun setDebugErrorMessage(message: String) = SettingsStore.setDebugErrorMessage(message)
    fun clearLoadingErrorDebug() = SettingsStore.clearLoadingErrorDebug()
}
