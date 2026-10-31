package com.example.fdmanager.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * #9 Firebase Auth — the app's FIRST persisted preference store.
 *
 * Everything else in the app is still in-memory (real data persistence is roadmap #10).
 * This exists because the session-gate decision ("gate account-holders only") needs ONE
 * fact to survive restarts: "was a real account ever used on this device?". Without
 * persistence, a signed-out account-holder would look identical to a never-signed-in
 * user after a restart and the gate would silently vanish.
 *
 * Only the REAL FirebaseAuthRepository ever sets the flag. The fake/mock path never
 * touches it, so every existing Tier2 test (fake sessions) is unaffected.
 */
object DevicePrefs {

    private const val PREFS = "fd_device_prefs"
    private const val KEY_HAS_ACCOUNT = "has_account_on_device"

    private var prefs: SharedPreferences? = null

    private val _hasAccountOnDevice = MutableStateFlow(false)
    val hasAccountOnDevice: StateFlow<Boolean> = _hasAccountOnDevice.asStateFlow()

    /** Call once from MainActivity.onCreate, before any auth decision is made. */
    fun init(context: Context) {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        _hasAccountOnDevice.value = p.getBoolean(KEY_HAS_ACCOUNT, false)
    }

    fun setHasAccountOnDevice(value: Boolean) {
        _hasAccountOnDevice.value = value
        prefs?.edit()?.putBoolean(KEY_HAS_ACCOUNT, value)?.apply()
    }
}
