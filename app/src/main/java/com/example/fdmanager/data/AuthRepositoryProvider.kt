package com.example.fdmanager.data

import android.content.Context
import com.google.firebase.FirebaseApp

/**
 * #9 Firebase Auth — chooses the real or fake [AuthRepository] exactly once per process.
 *
 * Selection rule:
 * - [forceFakeForTests] set (androidTest @BeforeClass) → fake, always. Existing Tier2
 *   flows keep using mock sessions with ZERO Firebase traffic even on machines where
 *   google-services.json exists.
 * - Firebase configured (json present at build → FirebaseApp auto-initialized) → real.
 * - Otherwise (CI, fresh clones) → fake. The app is fully usable either way.
 */
object AuthRepositoryProvider {

    @Volatile
    var forceFakeForTests: Boolean = false

    @Volatile
    private var instance: AuthRepository? = null

    /** Call once from MainActivity.onCreate before any ViewModel is created. */
    fun init(context: Context) {
        if (instance != null) return
        instance = if (!forceFakeForTests && FirebaseApp.getApps(context).isNotEmpty()) {
            FirebaseAuthRepository()
        } else {
            FakeAuthRepository
        }
    }

    val isUsingFake: Boolean
        get() = get() === FakeAuthRepository

    fun get(): AuthRepository = if (forceFakeForTests) FakeAuthRepository else (instance ?: FakeAuthRepository)
}
