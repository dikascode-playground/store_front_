package com.ibi.storefront.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ibi.storefront.R
import com.ibi.storefront.core.model.AppLanguage
import com.ibi.storefront.core.model.AppTheme
import com.ibi.storefront.core.model.AuthRepository
import com.ibi.storefront.core.model.AuthState
import com.ibi.storefront.core.model.SettingsRepository
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import javax.crypto.Cipher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppUiState(
    val authState: AuthState = AuthState.LoggedOut,
    val theme: AppTheme = AppTheme.Light,
    val language: AppLanguage = AppLanguage.English,
    val isReady: Boolean = false,
    val requestBiometricPrompt: Boolean = false,
    val biometricPromptMode: BiometricPromptMode? = null,
    val biometricMessage: String? = null,
)

enum class BiometricPromptMode {
    Enrollment,
    Unlock,
}

data class BiometricPromptRequest(
    val mode: BiometricPromptMode,
    val cipher: Cipher,
)

@HiltViewModel
class AppViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val requestBiometricPrompt = MutableStateFlow(false)
    private val biometricPromptMode = MutableStateFlow<BiometricPromptMode?>(null)
    private val biometricMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AppUiState> = combine(
        authRepository.authState,
        settingsRepository.settings,
        requestBiometricPrompt,
        biometricPromptMode,
        biometricMessage,
    ) { authState, settings, shouldPrompt, promptMode, message ->
        AppUiState(
            authState = authState,
            theme = settings.theme,
            language = settings.language,
            isReady = true,
            requestBiometricPrompt = shouldPrompt,
            biometricPromptMode = promptMode,
            biometricMessage = message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppUiState(),
    )

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    fun lockBiometricSessionIfNeeded() {
        viewModelScope.launch {
            authRepository.lockSessionIfNeeded()
        }
    }

    fun requestBiometricUnlock() {
        biometricPromptMode.value = BiometricPromptMode.Unlock
        requestBiometricPrompt.value = true
        biometricMessage.value = null
    }

    fun requestBiometricEnrollment() {
        biometricPromptMode.value = BiometricPromptMode.Enrollment
        requestBiometricPrompt.value = true
        biometricMessage.value = null
    }

    fun onBiometricPromptConsumed() {
        requestBiometricPrompt.value = false
    }

    suspend fun prepareBiometricPrompt(): BiometricPromptRequest? {
        val mode = biometricPromptMode.value ?: return null
        val result = when (mode) {
            BiometricPromptMode.Enrollment -> authRepository.createBiometricEnrollmentCipher()
            BiometricPromptMode.Unlock -> authRepository.createBiometricUnlockCipher()
        }
        return result.fold(
            onSuccess = { cipher -> BiometricPromptRequest(mode = mode, cipher = cipher) },
            onFailure = {
                biometricMessage.value = context.getString(R.string.biometric_unavailable_message)
                requestBiometricPrompt.value = false
                biometricPromptMode.value = null
                null
            },
        )
    }

    fun completeBiometricAuthentication(mode: BiometricPromptMode, authenticatedCipher: Cipher) {
        viewModelScope.launch {
            val result = when (mode) {
                BiometricPromptMode.Enrollment -> authRepository.completeBiometricEnrollment(authenticatedCipher)
                BiometricPromptMode.Unlock -> authRepository.unlockSession(authenticatedCipher)
            }
            requestBiometricPrompt.value = false
            biometricPromptMode.value = null
            biometricMessage.value = if (result.isSuccess) {
                if (mode == BiometricPromptMode.Enrollment) {
                    context.getString(R.string.biometric_enabled_message)
                } else {
                    null
                }
            } else {
                context.getString(R.string.biometric_unlock_failed_message)
            }
        }
    }

    fun onBiometricCancelled() {
        requestBiometricPrompt.value = false
        biometricPromptMode.value = null
        biometricMessage.value = context.getString(R.string.biometric_cancelled_message)
    }

    fun onBiometricUnavailable() {
        requestBiometricPrompt.value = false
        biometricPromptMode.value = null
        biometricMessage.value = context.getString(R.string.biometric_unavailable_message)
    }

    fun onBiometricFailed() {
        requestBiometricPrompt.value = false
        biometricPromptMode.value = null
        biometricMessage.value = context.getString(R.string.biometric_match_failed_message)
    }

    fun onBiometricError(message: String) {
        requestBiometricPrompt.value = false
        biometricPromptMode.value = null
        biometricMessage.value = message
    }

    fun clearBiometricMessage() {
        biometricMessage.value = null
    }
}
