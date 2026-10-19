package com.example.fdmanager.data

import com.example.fdmanager.domain.AuthValidation
import kotlinx.coroutines.flow.StateFlow

data class AuthState(
    val isLoggedIn: Boolean = false,
    val email: String? = null
)

interface AuthRepository {
    val authState: StateFlow<AuthState>
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signUp(email: String, password: String, confirmPassword: String): Result<Unit>
    suspend fun sendPasswordReset(email: String): Result<Unit>
    suspend fun signOut()
}

object FakeAuthRepository : AuthRepository {

    private val _state = SettingsStore.state

    override val authState: StateFlow<AuthState> = mapStateFlow(_state) { s ->
        AuthState(isLoggedIn = s.isMockLoggedIn, email = s.mockEmail)
    }

    override suspend fun signIn(email: String, password: String): Result<Unit> {
        val emailErr = AuthValidation.validateEmail(email)
        if (emailErr != null) return Result.failure(IllegalArgumentException(emailErr))
        val passErr = AuthValidation.validatePassword(password)
        if (passErr != null) return Result.failure(IllegalArgumentException(passErr))
        SettingsStore.setMockLoggedIn(email)
        return Result.success(Unit)
    }

    override suspend fun signUp(email: String, password: String, confirmPassword: String): Result<Unit> {
        val emailErr = AuthValidation.validateEmail(email)
        if (emailErr != null) return Result.failure(IllegalArgumentException(emailErr))
        val passErr = AuthValidation.validatePassword(password)
        if (passErr != null) return Result.failure(IllegalArgumentException(passErr))
        val confirmErr = AuthValidation.validateConfirm(password, confirmPassword)
        if (confirmErr != null) return Result.failure(IllegalArgumentException(confirmErr))
        SettingsStore.setMockLoggedIn(email)
        return Result.success(Unit)
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        val emailErr = AuthValidation.validateEmail(email)
        if (emailErr != null) return Result.failure(IllegalArgumentException(emailErr))
        return Result.success(Unit)
    }

    override suspend fun signOut() {
        SettingsStore.clearMockLoggedIn()
    }
}

private fun <T, R> mapStateFlow(
    source: StateFlow<T>,
    transform: (T) -> R
): StateFlow<R> {
    return object : StateFlow<R> {
        override val value: R get() = transform(source.value)
        override val replayCache: List<R> get() = listOf(value)
        override suspend fun collect(collector: kotlinx.coroutines.flow.FlowCollector<R>): Nothing {
            source.collect { t -> collector.emit(transform(t)) }
        }
    }
}
