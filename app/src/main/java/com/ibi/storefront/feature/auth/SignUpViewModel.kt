package com.ibi.storefront.feature.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ibi.storefront.R
import com.ibi.storefront.core.model.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignUpUiState(
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val usernameError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val signUpError: String? = null,
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
)

sealed interface SignUpAction {
    data class UsernameChanged(val value: String) : SignUpAction
    data class PasswordChanged(val value: String) : SignUpAction
    data class ConfirmPasswordChanged(val value: String) : SignUpAction
    data object TogglePasswordVisibility : SignUpAction
    data object ToggleConfirmPasswordVisibility : SignUpAction
    data object Submit : SignUpAction
}

@HiltViewModel
class SignUpViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun onAction(action: SignUpAction) {
        when (action) {
            is SignUpAction.UsernameChanged -> _uiState.update {
                it.copy(username = action.value, usernameError = null, signUpError = null)
            }
            is SignUpAction.PasswordChanged -> _uiState.update {
                it.copy(password = action.value, passwordError = null, signUpError = null)
            }
            is SignUpAction.ConfirmPasswordChanged -> _uiState.update {
                it.copy(confirmPassword = action.value, confirmPasswordError = null, signUpError = null)
            }
            SignUpAction.TogglePasswordVisibility -> _uiState.update {
                it.copy(isPasswordVisible = !it.isPasswordVisible)
            }
            SignUpAction.ToggleConfirmPasswordVisibility -> _uiState.update {
                it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible)
            }
            SignUpAction.Submit -> submit()
        }
    }

    private fun submit() {
        val state = _uiState.value
        if (state.isLoading) return

        val usernameError = if (state.username.isBlank()) {
            context.getString(R.string.validation_username_required)
        } else {
            null
        }
        val passwordError = when {
            state.password.isBlank() -> context.getString(R.string.validation_password_required)
            state.password.length < MIN_PASSWORD_LENGTH -> context.getString(
                R.string.validation_password_min_length,
                MIN_PASSWORD_LENGTH,
            )
            else -> null
        }
        val confirmPasswordError = when {
            state.confirmPassword.isBlank() -> context.getString(R.string.validation_confirm_password_required)
            state.password != state.confirmPassword -> context.getString(R.string.validation_password_mismatch)
            else -> null
        }
        if (usernameError != null || passwordError != null || confirmPasswordError != null) {
            _uiState.update {
                it.copy(
                    usernameError = usernameError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmPasswordError,
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, signUpError = null) }
        viewModelScope.launch {
            val result = authRepository.register(state.username.trim(), state.password)
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(isLoading = false, signUpError = null)
                } else {
                    val errorMessageRes = if (result.exceptionOrNull()?.message == "Username already exists") {
                        R.string.error_duplicate_username
                    } else {
                        R.string.error_sign_up_failed
                    }
                    it.copy(isLoading = false, signUpError = context.getString(errorMessageRes))
                }
            }
        }
    }

    private companion object {
        const val MIN_PASSWORD_LENGTH = 8
    }
}
