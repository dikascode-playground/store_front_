package com.ibi.storefront.core.model

import androidx.paging.PagingData
import javax.crypto.Cipher
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val authState: Flow<AuthState>
    suspend fun register(username: String, password: String): Result<UserSession>
    suspend fun login(username: String, password: String): Result<UserSession>
    suspend fun logout()
    suspend fun lockSessionIfNeeded()
    suspend fun createBiometricEnrollmentCipher(): Result<Cipher>
    suspend fun completeBiometricEnrollment(authenticatedCipher: Cipher): Result<Unit>
    suspend fun enableBiometric(enabled: Boolean)
    suspend fun createBiometricUnlockCipher(): Result<Cipher>
    suspend fun unlockSession(authenticatedCipher: Cipher): Result<UserSession>
}

interface ProductRepository {
    fun observeProducts(query: ProductQuery): Flow<PagingData<Product>>
    fun observeProduct(productId: Long): Flow<Product?>
    fun observeCategories(): Flow<List<String>>
    suspend fun refresh(): Result<Unit>
    suspend fun createProduct(input: ProductInput): Result<Unit>
    suspend fun updateProduct(productId: Long, input: ProductInput): Result<Unit>
    suspend fun deleteProduct(productId: Long): Result<Unit>
    suspend fun resetLocalChanges(): Result<Unit>
}

interface FavoritesRepository {
    fun observeFavorites(): Flow<PagingData<Product>>
    suspend fun setFavorite(productId: Long, favorite: Boolean): Result<Unit>
}

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setTheme(theme: AppTheme)
    suspend fun setLanguage(language: AppLanguage)
}
