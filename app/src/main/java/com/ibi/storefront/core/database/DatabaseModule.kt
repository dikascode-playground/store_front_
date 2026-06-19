package com.ibi.storefront.core.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): StoreFrontDatabase {
        return Room.databaseBuilder(
            context,
            StoreFrontDatabase::class.java,
            "storefront.db",
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideProductDao(database: StoreFrontDatabase): ProductDao = database.productDao()

    @Provides
    fun provideFavoriteDao(database: StoreFrontDatabase): FavoriteDao = database.favoriteDao()

    @Provides
    fun provideRemoteKeysDao(database: StoreFrontDatabase): ProductRemoteKeysDao = database.remoteKeysDao()

    @Provides
    fun provideUserAccountDao(database: StoreFrontDatabase): UserAccountDao = database.userAccountDao()
}
