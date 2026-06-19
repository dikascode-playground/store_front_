package com.ibi.storefront.data

import com.ibi.storefront.core.common.ValidateProductInputUseCase
import com.ibi.storefront.core.model.AuthRepository
import com.ibi.storefront.core.model.FavoritesRepository
import com.ibi.storefront.core.model.ProductRepository
import com.ibi.storefront.core.model.SettingsRepository
import com.ibi.storefront.data.auth.LocalAccountAuthRepository
import com.ibi.storefront.data.products.OfflineFirstProductRepository
import com.ibi.storefront.data.products.RoomFavoritesRepository
import com.ibi.storefront.data.settings.DataStoreSettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindingsModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(repository: LocalAccountAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindProductRepository(repository: OfflineFirstProductRepository): ProductRepository

    @Binds
    @Singleton
    abstract fun bindFavoritesRepository(repository: RoomFavoritesRepository): FavoritesRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(repository: DataStoreSettingsRepository): SettingsRepository
}

@Module
@InstallIn(SingletonComponent::class)
object RepositorySupportModule {
    @Provides
    fun provideValidateProductInputUseCase(): ValidateProductInputUseCase = ValidateProductInputUseCase()
}
