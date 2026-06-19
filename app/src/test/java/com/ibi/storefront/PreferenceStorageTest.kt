package com.ibi.storefront

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.ibi.storefront.core.datastore.PreferenceStorage
import com.ibi.storefront.core.model.AuthState
import com.ibi.storefront.core.security.SessionProtector
import javax.crypto.Cipher
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PreferenceStorageTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val sessionProtector = FakeSessionProtector()

    @Before
    fun resetStorage() = runTest {
        PreferenceStorage(context, sessionProtector).clearSession()
    }

    @Test
    fun persistSession_setsAuthenticatedNonBiometricState() = runTest {
        val storage = PreferenceStorage(context, sessionProtector)

        storage.persistSession(username = "tester", sessionId = "session-1")

        val state = storage.authState.first()
        assertThat(state).isInstanceOf(AuthState.Authenticated::class.java)
        val session = (state as AuthState.Authenticated).session
        assertThat(session.username).isEqualTo("tester")
        assertThat(session.sessionId).isEqualTo("session-1")
        assertThat(session.biometricEnabled).isFalse()
    }

    @Test
    fun biometricEnrollment_persistsLockedStateAcrossStorageReload() = runTest {
        val storage = PreferenceStorage(context, sessionProtector)
        storage.persistSession(username = "tester", sessionId = "session-1")

        storage.completeBiometricEnrollment(sessionProtector.createBiometricEncryptionCipher())

        val activeState = storage.authState.first()
        assertThat(activeState).isInstanceOf(AuthState.Authenticated::class.java)
        val reloadedState = PreferenceStorage(context, sessionProtector).authState.first()
        assertThat(reloadedState).isEqualTo(AuthState.Locked(username = "tester", biometricEnabled = true))
    }

    @Test
    fun biometricUnlock_restoresAuthenticatedState() = runTest {
        val storage = PreferenceStorage(context, sessionProtector)
        storage.persistSession(username = "tester", sessionId = "session-1")
        storage.completeBiometricEnrollment(sessionProtector.createBiometricEncryptionCipher())

        val reloadedStorage = PreferenceStorage(context, sessionProtector)
        val session = reloadedStorage.unlockSession(sessionProtector.createBiometricDecryptionCipher("bio:session-1"))

        assertThat(session.username).isEqualTo("tester")
        assertThat(session.sessionId).isEqualTo("session-1")
        assertThat(session.biometricEnabled).isTrue()
        val state = reloadedStorage.authState.first()
        assertThat(state).isInstanceOf(AuthState.Authenticated::class.java)
    }

    @Test
    fun disableBiometric_restoresNormalPersistedSession() = runTest {
        val storage = PreferenceStorage(context, sessionProtector)
        storage.persistSession(username = "tester", sessionId = "session-1")
        storage.completeBiometricEnrollment(sessionProtector.createBiometricEncryptionCipher())

        storage.setBiometricEnabled(false)

        val reloadedState = PreferenceStorage(context, sessionProtector).authState.first()
        assertThat(reloadedState).isInstanceOf(AuthState.Authenticated::class.java)
        val session = (reloadedState as AuthState.Authenticated).session
        assertThat(session.biometricEnabled).isFalse()
        assertThat(session.sessionId).isEqualTo("session-1")
    }

    @Test
    fun clearSession_returnsLoggedOutState() = runTest {
        val storage = PreferenceStorage(context, sessionProtector)
        storage.persistSession(username = "tester", sessionId = "session-1")

        storage.clearSession()

        assertThat(storage.authState.first()).isEqualTo(AuthState.LoggedOut)
    }

    private class FakeSessionProtector : SessionProtector {
        override fun encrypt(value: String): String = "enc:$value"

        override fun decrypt(value: String): String = value.removePrefix("enc:")

        override fun createBiometricEncryptionCipher(): Cipher = mockk(relaxed = true)

        override fun biometricEncrypt(value: String, authenticatedCipher: Cipher): String = "bio:$value"

        override fun createBiometricDecryptionCipher(value: String): Cipher = mockk(relaxed = true)

        override fun biometricDecrypt(value: String, authenticatedCipher: Cipher): String = value.removePrefix("bio:")
    }
}
