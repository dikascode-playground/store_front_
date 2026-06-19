package com.ibi.storefront.core.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionCipher @Inject constructor() : SessionProtector {
    private val keyStoreName = "AndroidKeyStore"
    private val transformation = "AES/GCM/NoPadding"

    override fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(transformation).apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateStorageKey())
        }
        return combinePayload(cipher.iv, cipher.doFinal(value.toByteArray(Charsets.UTF_8)))
    }

    override fun decrypt(value: String): String {
        val payload = splitPayload(value)
        val cipher = Cipher.getInstance(transformation).apply {
            init(Cipher.DECRYPT_MODE, getOrCreateStorageKey(), GCMParameterSpec(128, payload.iv))
        }
        return String(cipher.doFinal(payload.encryptedBytes), Charsets.UTF_8)
    }

    override fun createBiometricEncryptionCipher(): Cipher {
        return Cipher.getInstance(transformation).apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateBiometricKey())
        }
    }

    override fun biometricEncrypt(value: String, authenticatedCipher: Cipher): String {
        val encrypted = authenticatedCipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return combinePayload(authenticatedCipher.iv, encrypted)
    }

    override fun createBiometricDecryptionCipher(value: String): Cipher {
        val payload = splitPayload(value)
        return Cipher.getInstance(transformation).apply {
            init(Cipher.DECRYPT_MODE, getOrCreateBiometricKey(), GCMParameterSpec(128, payload.iv))
        }
    }

    override fun biometricDecrypt(value: String, authenticatedCipher: Cipher): String {
        val payload = splitPayload(value)
        return String(authenticatedCipher.doFinal(payload.encryptedBytes), Charsets.UTF_8)
    }

    private fun combinePayload(iv: ByteArray, encrypted: ByteArray): String {
        val combined = ByteBuffer.allocate(4 + iv.size + encrypted.size)
            .putInt(iv.size)
            .put(iv)
            .put(encrypted)
            .array()
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun splitPayload(value: String): SessionPayload {
        val combined = Base64.decode(value, Base64.NO_WRAP)
        val buffer = ByteBuffer.wrap(combined)
        val ivSize = buffer.int
        val iv = ByteArray(ivSize)
        buffer.get(iv)
        val encrypted = ByteArray(buffer.remaining())
        buffer.get(encrypted)
        return SessionPayload(iv = iv, encryptedBytes = encrypted)
    }

    private fun getOrCreateStorageKey(): SecretKey {
        return getOrCreateSecretKey(
            alias = STORAGE_ALIAS,
            configure = { builder ->
                builder.setUserAuthenticationRequired(false)
            },
        )
    }

    private fun getOrCreateBiometricKey(): SecretKey {
        return getOrCreateSecretKey(
            alias = BIOMETRIC_ALIAS,
            configure = { builder ->
                builder.setUserAuthenticationRequired(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    builder.setUserAuthenticationParameters(
                        0,
                        KeyProperties.AUTH_BIOMETRIC_STRONG,
                    )
                } else {
                    @Suppress("DEPRECATION")
                    builder.setUserAuthenticationValidityDurationSeconds(-1)
                }
                builder.setInvalidatedByBiometricEnrollment(true)
            },
        )
    }

    private fun getOrCreateSecretKey(
        alias: String,
        configure: (KeyGenParameterSpec.Builder) -> Unit,
    ): SecretKey {
        val keyStore = KeyStore.getInstance(keyStoreName).apply { load(null) }
        val existing = keyStore.getKey(alias, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, keyStoreName)
        val specBuilder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        configure(specBuilder)
        generator.init(specBuilder.build())
        return generator.generateKey()
    }

    private data class SessionPayload(
        val iv: ByteArray,
        val encryptedBytes: ByteArray,
    )

    private companion object {
        const val STORAGE_ALIAS = "storefront_session_key"
        const val BIOMETRIC_ALIAS = "storefront_biometric_session_key"
    }
}
