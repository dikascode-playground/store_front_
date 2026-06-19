package com.ibi.storefront.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ProductEntity::class,
        FavoriteEntity::class,
        ProductRemoteKeyEntity::class,
        UserAccountEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class StoreFrontDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun remoteKeysDao(): ProductRemoteKeysDao
    abstract fun userAccountDao(): UserAccountDao
}
