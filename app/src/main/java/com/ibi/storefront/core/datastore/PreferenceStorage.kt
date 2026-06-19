package com.ibi.storefront.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import com.ibi.storefront.core.model.AppLanguage
import com.ibi.storefront.core.model.AppSettings
import com.ibi.storefront.core.model.AppTheme
import com.ibi.storefront.core.model.AuthState
import com.ibi.storefront.core.model.UserSession
import com.ibi.storefront.core.security.SessionProtector
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import javax.crypto.Cipher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.storeFrontPreferences by preferencesDataStore(name = "storefront_preferences")

@Singleton
class PreferenceStorage @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val sessionProtector: SessionProtector,
) {
    private object Keys {
        val theme = stringPreferencesKey("theme")
        val language = stringPreferencesKey("language")
        val username = stringPreferencesKey("username")
        val encryptedSessionId = stringPreferencesKey("encrypted_session_id")
        val biometricEncryptedSessionId = stringPreferencesKey("biometric_encrypted_session_id")
        val biometricEnabled = booleanPreferencesKey("biometric_enabled")
        val sessionLocked = booleanPreferencesKey("session_locked")
    }

    private val unlockedBiometricSession = MutableStateFlow<UserSession?>(null)

    val settings: Flow<AppSettings> = context.storeFrontPreferences.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            AppSettings(
                theme = prefs[Keys.theme]?.let(AppTheme::valueOf) ?: AppTheme.Light,
                language = prefs[Keys.language]?.let(AppLanguage::valueOf) ?: AppLanguage.English,
                biometricEnabled = prefs[Keys.biometricEnabled] ?: false,
            )
        }

    val authState: Flow<AuthState> = combine(
        context.storeFrontPreferences.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences()) else throw exception
            },
        unlockedBiometricSession,
    ) { prefs, unlockedSession ->
            val username = prefs[Keys.username]
            val encryptedSession = prefs[Keys.encryptedSessionId]
            val biometricEncryptedSession = prefs[Keys.biometricEncryptedSessionId]
            val biometricEnabled = prefs[Keys.biometricEnabled] ?: false

            if (username.isNullOrBlank()) {
                AuthState.LoggedOut
            } else if (biometricEnabled && !biometricEncryptedSession.isNullOrBlank()) {
                val activeUnlockedSession = unlockedSession?.takeIf { it.username == username }
                if (activeUnlockedSession != null) {
                    AuthState.Authenticated(activeUnlockedSession.copy(biometricEnabled = true))
                } else {
                    AuthState.Locked(username = username, biometricEnabled = true)
                }
            } else if (!encryptedSession.isNullOrBlank()) {
                        AuthState.Authenticated(
                    UserSession(
                        sessionId = sessionProtector.decrypt(encryptedSession),
                        username = username,
                        biometricEnabled = false,
                    ),
                )
            } else {
                AuthState.LoggedOut
            }
    }

    suspend fun persistSession(username: String, sessionId: String) {
        unlockedBiometricSession.value = null
        context.storeFrontPreferences.edit { prefs ->
            prefs[Keys.username] = username
            prefs[Keys.encryptedSessionId] = sessionProtector.encrypt(sessionId)
            prefs.remove(Keys.biometricEncryptedSessionId)
            prefs[Keys.biometricEnabled] = false
            prefs[Keys.sessionLocked] = false
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        if (enabled) return
        val session = unlockedBiometricSession.value
        context.storeFrontPreferences.edit { prefs ->
            val username = prefs[Keys.username]
            if (!username.isNullOrBlank() && session != null) {
                prefs[Keys.encryptedSessionId] = sessionProtector.encrypt(session.sessionId)
            }
            prefs.remove(Keys.biometricEncryptedSessionId)
            prefs[Keys.biometricEnabled] = false
            prefs[Keys.sessionLocked] = false
        }
        unlockedBiometricSession.value = null
    }

    suspend fun createBiometricEnrollmentCipher(): Cipher {
        return sessionProtector.createBiometricEncryptionCipher()
    }

    suspend fun completeBiometricEnrollment(authenticatedCipher: Cipher) {
        val prefs = context.storeFrontPreferences.data.first()
        val username = prefs[Keys.username]
        val encryptedSession = prefs[Keys.encryptedSessionId]
        if (username.isNullOrBlank() || encryptedSession.isNullOrBlank()) {
            throw IllegalStateException("No active session to protect")
        }
        val sessionId = sessionProtector.decrypt(encryptedSession)
        val biometricPayload = sessionProtector.biometricEncrypt(sessionId, authenticatedCipher)
        unlockedBiometricSession.value = UserSession(
            sessionId = sessionId,
            username = username,
            biometricEnabled = true,
        )
        context.storeFrontPreferences.edit { prefs ->
            prefs.remove(Keys.encryptedSessionId)
            prefs[Keys.biometricEncryptedSessionId] = biometricPayload
            prefs[Keys.biometricEnabled] = true
            prefs[Keys.sessionLocked] = true
        }
    }

    suspend fun createBiometricUnlockCipher(): Cipher {
        val biometricPayload = context.storeFrontPreferences.data.first()[Keys.biometricEncryptedSessionId]
            ?: throw IllegalStateException("No biometric session available")
        return sessionProtector.createBiometricDecryptionCipher(biometricPayload)
    }

    suspend fun unlockSession(authenticatedCipher: Cipher): UserSession {
        val prefs = context.storeFrontPreferences.data.first()
        val username = prefs[Keys.username] ?: throw IllegalStateException("No username available")
        val biometricPayload = prefs[Keys.biometricEncryptedSessionId]
            ?: throw IllegalStateException("No biometric session available")
        val session = UserSession(
            sessionId = sessionProtector.biometricDecrypt(biometricPayload, authenticatedCipher),
            username = username,
            biometricEnabled = true,
        )
        unlockedBiometricSession.value = session
        context.storeFrontPreferences.edit { mutablePrefs ->
            mutablePrefs[Keys.sessionLocked] = false
        }
        return session
    }

    suspend fun lockBiometricSessionIfNeeded() {
        val prefs = context.storeFrontPreferences.data.first()
        val biometricEnabled = prefs[Keys.biometricEnabled] ?: false
        val biometricPayload = prefs[Keys.biometricEncryptedSessionId]
        if (!biometricEnabled || biometricPayload.isNullOrBlank()) return

        unlockedBiometricSession.value = null
        context.storeFrontPreferences.edit { mutablePrefs ->
            mutablePrefs[Keys.sessionLocked] = true
        }
    }

    suspend fun clearSession() {
        unlockedBiometricSession.value = null
        context.storeFrontPreferences.edit { prefs ->
            prefs.remove(Keys.username)
            prefs.remove(Keys.encryptedSessionId)
            prefs.remove(Keys.biometricEncryptedSessionId)
            prefs[Keys.sessionLocked] = false
            prefs[Keys.biometricEnabled] = false
        }
    }

    suspend fun setTheme(theme: AppTheme) = editString(Keys.theme, theme.name)

    suspend fun setLanguage(language: AppLanguage) = editString(Keys.language, language.name)

    private suspend fun editString(key: Preferences.Key<String>, value: String) {
        context.storeFrontPreferences.edit { prefs -> prefs[key] = value }
    }
}
