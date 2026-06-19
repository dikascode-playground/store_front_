package com.ibi.storefront.data.auth
import com.ibi.storefront.core.database.UserAccountDao
import com.ibi.storefront.core.database.UserAccountEntity
import com.ibi.storefront.core.datastore.PreferenceStorage
import com.ibi.storefront.core.model.AuthRepository
import com.ibi.storefront.core.model.AuthState
import com.ibi.storefront.core.model.UserSession
import com.ibi.storefront.core.security.PasswordHasher
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import javax.crypto.Cipher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class LocalAccountAuthRepository @Inject constructor(
    private val userAccountDao: UserAccountDao,
    private val passwordHasher: PasswordHasher,
    private val preferenceStorage: PreferenceStorage,
) : AuthRepository {
    override val authState: Flow<AuthState> = preferenceStorage.authState

    override suspend fun register(username: String, password: String): Result<UserSession> {
        val normalizedUsername = normalizeUsername(username)
        ensureSeedAccountExists()
        if (userAccountDao.getByUsername(normalizedUsername) != null) {
            return Result.failure(IllegalArgumentException("Username already exists"))
        }
        val passwordHash = passwordHasher.createHash(password)
        userAccountDao.insert(
            UserAccountEntity(
                username = normalizedUsername,
                passwordHash = passwordHash.hash,
                passwordSalt = passwordHash.salt,
                createdAt = System.currentTimeMillis(),
            ),
        )
        return persistSession(normalizedUsername)
    }

    override suspend fun login(username: String, password: String): Result<UserSession> {
        val normalizedUsername = normalizeUsername(username)
        ensureSeedAccountExists()
        val account = userAccountDao.getByUsername(normalizedUsername)
            ?: return Result.failure(IllegalArgumentException("Invalid credentials"))
        val passwordMatches = passwordHasher.verify(
            password = password,
            expectedHash = account.passwordHash,
            expectedSalt = account.passwordSalt,
        )
        if (!passwordMatches) {
            return Result.failure(IllegalArgumentException("Invalid credentials"))
        }
        return persistSession(account.username)
    }

    override suspend fun logout() {
        preferenceStorage.clearSession()
    }

    override suspend fun lockSessionIfNeeded() {
        preferenceStorage.lockBiometricSessionIfNeeded()
    }

    override suspend fun createBiometricEnrollmentCipher(): Result<Cipher> {
        return runCatching { preferenceStorage.createBiometricEnrollmentCipher() }
    }

    override suspend fun completeBiometricEnrollment(authenticatedCipher: Cipher): Result<Unit> {
        return runCatching { preferenceStorage.completeBiometricEnrollment(authenticatedCipher) }
    }

    override suspend fun enableBiometric(enabled: Boolean) {
        preferenceStorage.setBiometricEnabled(enabled)
    }

    override suspend fun createBiometricUnlockCipher(): Result<Cipher> {
        return runCatching { preferenceStorage.createBiometricUnlockCipher() }
    }

    override suspend fun unlockSession(authenticatedCipher: Cipher): Result<UserSession> {
        return runCatching { preferenceStorage.unlockSession(authenticatedCipher) }
    }

    private suspend fun persistSession(username: String): Result<UserSession> {
        val session = UserSession(
            sessionId = UUID.randomUUID().toString(),
            username = username,
            biometricEnabled = false,
        )
        preferenceStorage.persistSession(username = session.username, sessionId = session.sessionId)
        return Result.success(session)
    }

    private suspend fun ensureSeedAccountExists() {
        seedMutex.withLock {
            val existing = userAccountDao.getByUsername(SEEDED_REVIEW_USERNAME)
            if (existing != null) return
            val seedHash = passwordHasher.createHash(SEEDED_REVIEW_PASSWORD)
            userAccountDao.insert(
                UserAccountEntity(
                    username = SEEDED_REVIEW_USERNAME,
                    passwordHash = seedHash.hash,
                    passwordSalt = seedHash.salt,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    private fun normalizeUsername(username: String): String {
        return username.trim().lowercase(Locale.US)
    }

    companion object {
        const val SEEDED_REVIEW_USERNAME = "ibi_engineer"
        const val SEEDED_REVIEW_PASSWORD = "Android@123"
        private val seedMutex = Mutex()
    }
}
