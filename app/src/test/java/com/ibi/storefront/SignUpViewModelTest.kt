package com.ibi.storefront

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.ibi.storefront.core.model.AuthRepository
import com.ibi.storefront.core.model.AuthState
import com.ibi.storefront.core.model.UserSession
import com.ibi.storefront.feature.auth.SignUpAction
import com.ibi.storefront.feature.auth.SignUpViewModel
import io.mockk.every
import io.mockk.mockk
import javax.crypto.Cipher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignUpViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context = mockk<Context>().apply {
        every { getString(R.string.validation_username_required) } returns "Username is required"
        every { getString(R.string.validation_password_required) } returns "Password is required"
        every { getString(R.string.validation_confirm_password_required) } returns "Confirm your password"
        every { getString(R.string.validation_password_mismatch) } returns "Passwords do not match"
        every { getString(R.string.validation_password_min_length, any<Int>()) } answers {
            "Password must be at least ${secondArg<Int>()} characters"
        }
        every { getString(R.string.error_duplicate_username) } returns "That username is already in use"
        every { getString(R.string.error_sign_up_failed) } returns "Unable to create the account right now"
    }

    @Test
    fun submit_withMismatchedPasswords_setsConfirmPasswordError() = runTest {
        val viewModel = SignUpViewModel(
            context = context,
            authRepository = FakeAuthRepository(),
        )

        viewModel.onAction(SignUpAction.UsernameChanged("tester"))
        viewModel.onAction(SignUpAction.PasswordChanged("Password123"))
        viewModel.onAction(SignUpAction.ConfirmPasswordChanged("Password124"))
        viewModel.onAction(SignUpAction.Submit)

        assertThat(viewModel.uiState.value.confirmPasswordError).isEqualTo("Passwords do not match")
    }

    @Test
    fun submit_withDuplicateUsername_setsSignUpError() = runTest {
        val viewModel = SignUpViewModel(
            context = context,
            authRepository = FakeAuthRepository(registerResult = Result.failure(IllegalArgumentException("Username already exists"))),
        )

        viewModel.onAction(SignUpAction.UsernameChanged("senior"))
        viewModel.onAction(SignUpAction.PasswordChanged("Password123"))
        viewModel.onAction(SignUpAction.ConfirmPasswordChanged("Password123"))
        viewModel.onAction(SignUpAction.Submit)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.signUpError).isEqualTo("That username is already in use")
    }

    @Test
    fun submit_withValidInput_clearsLoadingState() = runTest {
        val viewModel = SignUpViewModel(
            context = context,
            authRepository = FakeAuthRepository(),
        )

        viewModel.onAction(SignUpAction.UsernameChanged("tester"))
        viewModel.onAction(SignUpAction.PasswordChanged("Password123"))
        viewModel.onAction(SignUpAction.ConfirmPasswordChanged("Password123"))
        viewModel.onAction(SignUpAction.Submit)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.signUpError).isNull()
        assertThat(viewModel.uiState.value.isLoading).isFalse()
    }

    @Test
    fun submit_whileLoading_ignoresDuplicateRequest() = runTest {
        val repository = FakeAuthRepository(delayMillis = 1_000)
        val viewModel = SignUpViewModel(
            context = context,
            authRepository = repository,
        )

        viewModel.onAction(SignUpAction.UsernameChanged("tester"))
        viewModel.onAction(SignUpAction.PasswordChanged("Password123"))
        viewModel.onAction(SignUpAction.ConfirmPasswordChanged("Password123"))
        viewModel.onAction(SignUpAction.Submit)
        viewModel.onAction(SignUpAction.Submit)
        advanceTimeBy(1_000)
        advanceUntilIdle()

        assertThat(repository.registerCalls).isEqualTo(1)
    }

    private class FakeAuthRepository(
        private val registerResult: Result<UserSession> = Result.success(UserSession("session", "tester", false)),
        private val delayMillis: Long = 0,
    ) : AuthRepository {
        override val authState: Flow<AuthState> = MutableStateFlow(AuthState.LoggedOut)
        var registerCalls: Int = 0
            private set

        override suspend fun register(username: String, password: String): Result<UserSession> {
            registerCalls += 1
            if (delayMillis > 0) {
                delay(delayMillis)
            }
            return registerResult
        }

        override suspend fun login(username: String, password: String): Result<UserSession> {
            return Result.failure(IllegalStateException("not supported"))
        }

        override suspend fun logout() = Unit

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
