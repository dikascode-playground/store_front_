package com.ibi.storefront.feature.auth

import android.content.Context
import com.ibi.storefront.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ibi.storefront.core.model.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val usernameError: String? = null,
    val passwordError: String? = null,
    val loginError: String? = null,
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
)

sealed interface LoginAction {
    data class UsernameChanged(val value: String) : LoginAction
    data class PasswordChanged(val value: String) : LoginAction
    data object TogglePasswordVisibility : LoginAction
    data object Submit : LoginAction
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.UsernameChanged -> _uiState.update {
                it.copy(username = action.value, usernameError = null, loginError = null)
            }
            is LoginAction.PasswordChanged -> _uiState.update {
                it.copy(password = action.value, passwordError = null, loginError = null)
            }
            LoginAction.TogglePasswordVisibility -> _uiState.update {
                it.copy(isPasswordVisible = !it.isPasswordVisible)
            }
            LoginAction.Submit -> submit()
        }
    }

    private fun submit() {
        val state = _uiState.value
        if (state.isLoading) return
        val usernameError = if (state.username.isBlank()) context.getString(R.string.validation_username_required) else null
        val passwordError = if (state.password.isBlank()) context.getString(R.string.validation_password_required) else null
        if (usernameError != null || passwordError != null) {
            _uiState.update {
                it.copy(usernameError = usernameError, passwordError = passwordError)
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, loginError = null) }
        viewModelScope.launch {
            val result = authRepository.login(state.username.trim(), state.password)
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(isLoading = false, loginError = null)
                } else {
                    it.copy(isLoading = false, loginError = context.getString(R.string.error_invalid_credentials))
                }
            }
        }
    }
}
