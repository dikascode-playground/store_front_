package com.ibi.storefront

import android.os.Bundle
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ibi.storefront.app.navigation.AppViewModel
import com.ibi.storefront.app.navigation.StoreFrontApp
import com.ibi.storefront.core.model.AppTheme
import com.ibi.storefront.ui.theme.StoreFrontTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appState = appViewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(appState.value.language) {
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(appState.value.language.languageTag),
                )
            }
            LaunchedEffect(appState.value.requestBiometricPrompt) {
                if (appState.value.requestBiometricPrompt) {
                    val promptRequest = appViewModel.prepareBiometricPrompt()
                    if (promptRequest == null) {
                        return@LaunchedEffect
                    }
                    val biometricManager = BiometricManager.from(this@MainActivity)
                    val canAuthenticate = biometricManager.canAuthenticate(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG,
                    )
                    if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
                        appViewModel.onBiometricUnavailable()
                    } else {
                        val executor = ContextCompat.getMainExecutor(this@MainActivity)
                        val prompt = BiometricPrompt(
                            this@MainActivity,
                            executor,
                            object : BiometricPrompt.AuthenticationCallback() {
                                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                    val authenticatedCipher = result.cryptoObject?.cipher
                                    if (authenticatedCipher == null) {
                                        appViewModel.onBiometricError(getString(R.string.biometric_unavailable_message))
                                    } else {
                                        appViewModel.completeBiometricAuthentication(
                                            mode = promptRequest.mode,
                                            authenticatedCipher = authenticatedCipher,
                                        )
                                    }
                                }

                                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                    when (errorCode) {
                                        BiometricPrompt.ERROR_USER_CANCELED,
                                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                                        BiometricPrompt.ERROR_CANCELED,
                                        BiometricPrompt.ERROR_TIMEOUT,
                                        BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL -> {
                                            appViewModel.onBiometricCancelled()
                                        }
                                        BiometricPrompt.ERROR_HW_UNAVAILABLE,
                                        BiometricPrompt.ERROR_HW_NOT_PRESENT,
                                        BiometricPrompt.ERROR_NO_BIOMETRICS -> {
                                            appViewModel.onBiometricUnavailable()
                                        }
                                        else -> appViewModel.onBiometricError(errString.toString())
                                    }
                                }

                                override fun onAuthenticationFailed() {
                                    appViewModel.onBiometricFailed()
                                }
                            },
                        )
                        val promptInfo = BiometricPrompt.PromptInfo.Builder()
                            .setTitle(getString(R.string.biometric_prompt_title))
                            .setSubtitle(getString(R.string.biometric_prompt_subtitle))
                            .setNegativeButtonText(getString(R.string.biometric_fallback_action))
                            .build()
                        prompt.authenticate(
                            promptInfo,
                            BiometricPrompt.CryptoObject(promptRequest.cipher),
                        )
                        appViewModel.onBiometricPromptConsumed()
                    }
                }
            }
            StoreFrontTheme(
                darkTheme = appState.value.theme == AppTheme.Dark,
                dynamicColor = false,
            ) {
                StoreFrontApp(appViewModel = appViewModel)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            appViewModel.lockBiometricSessionIfNeeded()
        }
    }
}
