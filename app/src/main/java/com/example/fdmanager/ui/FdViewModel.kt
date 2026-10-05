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

/** Thin ViewModel over the in-memory repositories — no backend wiring in this mock. */
class FdViewModel : ViewModel() {

    private val repo = FdRepository.get()

    val fds: StateFlow<List<FixedDeposit>> = repo.fds
    val settings: StateFlow<SettingsState> = SettingsStore.state

    // Issue #25 — process auto-renewals that came due while the app was closed (session start).
    init {
        repo.autoRenewDue()
    }

    // --- FD operations ---
    fun addFd(fd: FixedDeposit): String = repo.add(fd)
    fun updateFd(fd: FixedDeposit) = repo.update(fd)
    fun softDelete(id: String) = repo.softDelete(id)
    fun restore(id: String) = repo.restore(id)
    fun deleteForever(id: String) = repo.deleteForever(id)
    fun renew(id: String, option: RenewOption? = null): String = repo.renew(id, option)
    fun resetDemoData() = repo.reset(SampleData.seed(LocalDate.now()))

    // --- Settings ---
    fun setThemeMode(mode: ThemeMode) = SettingsStore.setThemeMode(mode)
    fun setNotificationsEnabled(enabled: Boolean) = SettingsStore.setNotificationsEnabled(enabled)
    fun setNotifyLeadDays(days: Int) = SettingsStore.setNotifyLeadDays(days)
    fun setBiometricEnabled(enabled: Boolean) = SettingsStore.setBiometricEnabled(enabled)
}
