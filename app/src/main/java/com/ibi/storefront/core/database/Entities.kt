package com.ibi.storefront.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ibi.storefront.core.model.LocalProductState
import com.ibi.storefront.core.model.ProductSource

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val description: String,
    val price: Double,
    val category: String,
    val brand: String?,
    val rating: Double,
    val stock: Int,
    val thumbnail: String,
    val images: List<String>,
    val source: ProductSource,
    val localState: LocalProductState,
    val lastUpdatedAt: Long,
)

@Entity(
    tableName = "favorites",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("productId")],
)
data class FavoriteEntity(
    @PrimaryKey val productId: Long,
    val createdAt: Long,
)

@Entity(tableName = "product_remote_keys")
data class ProductRemoteKeyEntity(
    @PrimaryKey val productId: Long,
    val previousKey: Int?,
    val nextKey: Int?,
)

@Entity(
    tableName = "user_accounts",
    indices = [Index(value = ["username"], unique = true)],
)
data class UserAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val passwordHash: String,
    val passwordSalt: String,
    val createdAt: Long,
)

data class ProductWithFavoriteEntity(
    val id: Long,
    val title: String,
    val description: String,
    val price: Double,
    val category: String,
    val brand: String?,
    val rating: Double,
    val stock: Int,
    val thumbnail: String,
    val images: List<String>,
    val source: ProductSource,
    val localState: LocalProductState,
    val isFavorite: Boolean,
)
