package com.ibi.storefront.core.security

import javax.crypto.Cipher

interface SessionProtector {
    fun encrypt(value: String): String
    fun decrypt(value: String): String
    fun createBiometricEncryptionCipher(): Cipher
    fun biometricEncrypt(value: String, authenticatedCipher: Cipher): String
    fun createBiometricDecryptionCipher(value: String): Cipher
    fun biometricDecrypt(value: String, authenticatedCipher: Cipher): String
}
