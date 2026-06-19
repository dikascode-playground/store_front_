package com.ibi.storefront

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.ibi.storefront.core.database.StoreFrontDatabase
import com.ibi.storefront.core.datastore.PreferenceStorage
import com.ibi.storefront.core.model.AuthState
import com.ibi.storefront.core.security.PasswordHasher
import com.ibi.storefront.core.security.SessionProtector
import com.ibi.storefront.data.auth.LocalAccountAuthRepository
import io.mockk.mockk
import javax.crypto.Cipher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LocalAccountAuthRepositoryTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val sessionProtector = FakeSessionProtector()
    private lateinit var database: StoreFrontDatabase
    private lateinit var preferenceStorage: PreferenceStorage
    private lateinit var repository: LocalAccountAuthRepository

    @Before
    fun setUp() = runTest {
        database = Room.inMemoryDatabaseBuilder(context, StoreFrontDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        preferenceStorage = PreferenceStorage(context, sessionProtector)
        preferenceStorage.clearSession()
        repository = LocalAccountAuthRepository(
            userAccountDao = database.userAccountDao(),
            passwordHasher = PasswordHasher(),
            preferenceStorage = preferenceStorage,
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun register_createsNormalizedAccountAndSession() = runTest {
        val result = repository.register("  Tester  ", "Password123")

        assertThat(result.isSuccess).isTrue()
        val storedAccount = database.userAccountDao().getByUsername("tester")
        assertThat(storedAccount).isNotNull()
        assertThat(storedAccount!!.passwordHash).isNotEqualTo("Password123")
        assertThat(preferenceStorage.authState.first()).isInstanceOf(AuthState.Authenticated::class.java)
    }

    @Test
    fun register_duplicateUsernameFails() = runTest {
        repository.register("tester", "Password123")

        val duplicateResult = repository.register("TESTER", "Password123")

        assertThat(duplicateResult.isFailure).isTrue()
    }

    @Test
    fun login_withCorrectPasswordSucceeds() = runTest {
        repository.register("tester", "Password123")
        repository.logout()

        val result = repository.login("tester", "Password123")

        assertThat(result.isSuccess).isTrue()
        assertThat(preferenceStorage.authState.first()).isInstanceOf(AuthState.Authenticated::class.java)
    }

    @Test
    fun login_withWrongPasswordFails() = runTest {
        repository.register("tester", "Password123")
        repository.logout()

        val result = repository.login("tester", "WrongPassword")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun biometricUnlock_withoutStoredPayloadFailsSafely() = runTest {
        repository.register("tester", "Password123")

        val result = repository.createBiometricUnlockCipher()

        assertThat(result.isFailure).isTrue()
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
