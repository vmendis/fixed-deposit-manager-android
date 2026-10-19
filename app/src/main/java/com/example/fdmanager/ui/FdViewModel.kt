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
    private val authRepo = com.example.fdmanager.data.FakeAuthRepository

    val fds: StateFlow<List<FixedDeposit>> = repo.fds
    val settings: StateFlow<SettingsState> = SettingsStore.state
    val authState = authRepo.authState

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

    fun setThemeMode(mode: ThemeMode) = SettingsStore.setThemeMode(mode)
    fun setNotificationsEnabled(enabled: Boolean) = SettingsStore.setNotificationsEnabled(enabled)
    fun setNotifyLeadDays(days: Int) = SettingsStore.setNotifyLeadDays(days)
    fun setBiometricEnabled(enabled: Boolean) = SettingsStore.setBiometricEnabled(enabled)
    fun setHasSeenOnboarding(seen: Boolean) = SettingsStore.setHasSeenOnboarding(seen)
    fun setDemoMode(enabled: Boolean) {
        toggleDemoMode(enabled)
    }

    suspend fun mockSignIn(email: String, password: String): Result<Unit> = authRepo.signIn(email, password)
    suspend fun mockSignUp(email: String, password: String, confirm: String): Result<Unit> = authRepo.signUp(email, password, confirm)
    suspend fun mockSendReset(email: String): Result<Unit> = authRepo.sendPasswordReset(email)
    fun mockSignOut() {
        SettingsStore.clearMockLoggedIn()
    }
}
