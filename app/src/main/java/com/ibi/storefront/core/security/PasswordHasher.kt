package com.ibi.storefront.core.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PasswordHasher @Inject constructor() {
    fun createHash(password: String): PasswordHash {
        val salt = ByteArray(SALT_LENGTH_BYTES).also(secureRandom::nextBytes)
        val hash = deriveHash(password, salt)
        return PasswordHash(
            hash = Base64.encodeToString(hash, Base64.NO_WRAP),
            salt = Base64.encodeToString(salt, Base64.NO_WRAP),
        )
    }

    fun verify(password: String, expectedHash: String, expectedSalt: String): Boolean {
        val decodedSalt = Base64.decode(expectedSalt, Base64.NO_WRAP)
        val derivedHash = deriveHash(password, decodedSalt)
        val decodedExpectedHash = Base64.decode(expectedHash, Base64.NO_WRAP)
        return MessageDigest.isEqual(decodedExpectedHash, derivedHash)
    }

    private fun deriveHash(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH_BITS)
        return secretKeyFactory.generateSecret(spec).encoded
    }

    data class PasswordHash(
        val hash: String,
        val salt: String,
    )

    private companion object {
        const val ITERATION_COUNT = 120_000
        const val KEY_LENGTH_BITS = 256
        const val SALT_LENGTH_BYTES = 16
        const val ALGORITHM = "PBKDF2WithHmacSHA256"
        val secureRandom = SecureRandom()
        val secretKeyFactory: SecretKeyFactory = SecretKeyFactory.getInstance(ALGORITHM)
    }
}
