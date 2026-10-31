package com.example.fdmanager.data

import com.example.fdmanager.domain.AuthValidation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * #9 Firebase Auth — the real implementation behind [AuthRepository].
 *
 * Selected by [AuthRepositoryProvider] ONLY when Firebase is configured
 * (google-services.json present at build time). CI and json-less builds never
 * construct this class.
 *
 * - Session persistence & token refresh: handled natively by the Firebase SDK —
 *   the signed-in state survives app restarts without any code here.
 * - Client-side validation runs BEFORE any network call (same messages as the
 *   fake path, so UI behavior and tests stay consistent).
 * - On first successful sign-in/up, [DevicePrefs.setHasAccountOnDevice] records
 *   that this device has an account — that persisted fact drives the session gate.
 */
class FirebaseAuthRepository(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) : AuthRepository {

    private val _state = MutableStateFlow(snapshot())
    override val authState: StateFlow<AuthState> = _state.asStateFlow()

    init {
        auth.addAuthStateListener { _state.value = snapshot() }
    }

    private fun snapshot() = AuthState(
        isLoggedIn = auth.currentUser != null,
        email = auth.currentUser?.email
    )

    override suspend fun signIn(email: String, password: String): Result<Unit> {
        AuthValidation.validateEmail(email)?.let { return Result.failure(IllegalArgumentException(it)) }
        AuthValidation.validatePassword(password)?.let { return Result.failure(IllegalArgumentException(it)) }
        return runAuthTask { auth.signInWithEmailAndPassword(email.trim(), password) }
    }

    override suspend fun signUp(email: String, password: String, confirmPassword: String): Result<Unit> {
        AuthValidation.validateEmail(email)?.let { return Result.failure(IllegalArgumentException(it)) }
        AuthValidation.validatePassword(password)?.let { return Result.failure(IllegalArgumentException(it)) }
        AuthValidation.validateConfirm(password, confirmPassword)?.let { return Result.failure(IllegalArgumentException(it)) }
        return runAuthTask { auth.createUserWithEmailAndPassword(email.trim(), password) }
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        AuthValidation.validateEmail(email)?.let { return Result.failure(IllegalArgumentException(it)) }
        return runAuthTask { auth.sendPasswordResetEmail(email.trim()) }
    }

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit> {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return runAuthTask { auth.signInWithCredential(credential) }
    }

    override suspend fun signOut() {
        auth.signOut()
        // DevicePrefs.hasAccountOnDevice stays TRUE — sign-out hides, never forgets
        // (decision: gate account-holders; records remain on device).
    }

    private suspend fun <T> runAuthTask(block: () -> Task<T>): Result<Unit> {
        return try {
            block().awaitResult()
            DevicePrefs.setHasAccountOnDevice(true)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(IllegalStateException(userMessage(e), e))
        }
    }

    /** Minimal Task→coroutine bridge — avoids adding kotlinx-coroutines-play-services. */
    private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                @Suppress("UNCHECKED_CAST")
                cont.resume(task.result as T)
            } else {
                val e = task.exception ?: IllegalStateException("Unknown auth failure")
                if (cont.isActive) cont.cancel(e)
            }
        }
    }

    private fun userMessage(e: Exception): String = when ((e.cause as? FirebaseAuthException ?: e as? FirebaseAuthException)?.errorCode) {
        "ERROR_INVALID_CREDENTIAL", "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND" ->
            "Email or password is incorrect"
        "ERROR_EMAIL_ALREADY_IN_USE" -> "An account with this email already exists"
        "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts — try again later"
        "ERROR_NETWORK_REQUEST_FAILED" -> "No connection — check your network"
        else -> e.cause?.message ?: e.message ?: "Authentication failed"
    }
}
