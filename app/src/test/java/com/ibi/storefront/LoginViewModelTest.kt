package com.ibi.storefront

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.ibi.storefront.core.model.AuthRepository
import com.ibi.storefront.core.model.AuthState
import com.ibi.storefront.core.model.UserSession
import com.ibi.storefront.feature.auth.LoginAction
import com.ibi.storefront.feature.auth.LoginViewModel
import io.mockk.every
import io.mockk.mockk
import javax.crypto.Cipher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context = mockk<Context>().apply {
        every { getString(R.string.validation_username_required) } returns "Username is required"
        every { getString(R.string.validation_password_required) } returns "Password is required"
        every { getString(R.string.error_invalid_credentials) } returns "Invalid username or password"
    }

    @Test
    fun submit_withEmptyUsername_setsUsernameError() = runTest {
        val viewModel = LoginViewModel(
            context = context,
            authRepository = FakeAuthRepository(),
        )

        viewModel.onAction(LoginAction.PasswordChanged("Android@123"))
        viewModel.onAction(LoginAction.Submit)

        assertThat(viewModel.uiState.value.usernameError).isEqualTo("Username is required")
    }

    @Test
    fun submit_withEmptyPassword_setsPasswordError() = runTest {
        val viewModel = LoginViewModel(
            context = context,
            authRepository = FakeAuthRepository(),
        )

        viewModel.onAction(LoginAction.UsernameChanged("senior"))
        viewModel.onAction(LoginAction.Submit)

        assertThat(viewModel.uiState.value.passwordError).isEqualTo("Password is required")
    }

    @Test
    fun submit_withInvalidCredentials_setsLoginError() = runTest {
        val viewModel = LoginViewModel(
            context = context,
            authRepository = FakeAuthRepository(loginSucceeds = false),
        )

        viewModel.onAction(LoginAction.UsernameChanged("wrong"))
        viewModel.onAction(LoginAction.PasswordChanged("bad"))
        viewModel.onAction(LoginAction.Submit)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.loginError).isEqualTo("Invalid username or password")
        assertThat(viewModel.uiState.value.isLoading).isFalse()
    }

    @Test
    fun submit_withValidCredentials_clearsLoadingAndError() = runTest {
        val viewModel = LoginViewModel(
            context = context,
            authRepository = FakeAuthRepository(loginSucceeds = true),
        )

        viewModel.onAction(LoginAction.UsernameChanged("senior"))
        viewModel.onAction(LoginAction.PasswordChanged("Android@123"))
        viewModel.onAction(LoginAction.Submit)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.loginError).isNull()
        assertThat(viewModel.uiState.value.isLoading).isFalse()
    }

    private class FakeAuthRepository(
        private val loginSucceeds: Boolean = true,
    ) : AuthRepository {
        override val authState: Flow<AuthState> = MutableStateFlow(AuthState.LoggedOut)

        override suspend fun register(username: String, password: String): Result<UserSession> {
            return Result.failure(IllegalStateException("not supported"))
        }

        override suspend fun login(username: String, password: String): Result<UserSession> {
            return if (loginSucceeds) {
                Result.success(UserSession("session", username, false))
            } else {
                Result.failure(IllegalArgumentException("bad credentials"))
            }
        }

        override suspend fun logout() = Unit

        override suspend fun lockSessionIfNeeded() = Unit

        override suspend fun createBiometricEnrollmentCipher(): Result<Cipher> {
            return Result.failure(IllegalStateException("not supported"))
        }

        override suspend fun completeBiometricEnrollment(authenticatedCipher: Cipher): Result<Unit> {
            return Result.failure(IllegalStateException("not supported"))
        }

        override suspend fun enableBiometric(enabled: Boolean) = Unit

        override suspend fun createBiometricUnlockCipher(): Result<Cipher> {
            return Result.failure(IllegalStateException("not supported"))
        }

        override suspend fun unlockSession(authenticatedCipher: Cipher): Result<UserSession> {
            return Result.failure(IllegalStateException("not supported"))
        }
    }
}
