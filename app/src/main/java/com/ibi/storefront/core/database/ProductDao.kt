package com.ibi.storefront.core.database

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.ibi.storefront.core.model.LocalProductState
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query(
        """
        SELECT *
        FROM products
        WHERE localState != :deletedState
          AND (:search = '' OR title LIKE '%' || :search || '%' OR description LIKE '%' || :search || '%')
          AND (:category IS NULL OR category = :category)
        ORDER BY
            CASE WHEN :sort = 'PRICE_LOW_TO_HIGH' THEN price END ASC,
            CASE WHEN :sort = 'PRICE_HIGH_TO_LOW' THEN price END DESC,
            CASE WHEN :sort = 'RATING_HIGH_TO_LOW' THEN rating END DESC,
            CASE WHEN :sort = 'TITLE_ASCENDING' THEN title END COLLATE NOCASE ASC,
            CASE WHEN :sort = 'DEFAULT' THEN id END ASC
        """
    )
    fun entityPagingSource(
        search: String,
        category: String?,
        sort: String,
        deletedState: LocalProductState = LocalProductState.DELETED,
    ): PagingSource<Int, ProductEntity>

    @Query(
        """
        SELECT p.id, p.title, p.description, p.price, p.category, p.brand, p.rating, p.stock,
               p.thumbnail, p.images, p.source, p.localState,
               CASE WHEN f.productId IS NULL THEN 0 ELSE 1 END AS isFavorite
        FROM products p
        LEFT JOIN favorites f ON p.id = f.productId
        WHERE p.localState != :deletedState
          AND (:search = '' OR p.title LIKE '%' || :search || '%' OR p.description LIKE '%' || :search || '%')
          AND (:category IS NULL OR p.category = :category)
        ORDER BY
            CASE WHEN :sort = 'PRICE_LOW_TO_HIGH' THEN p.price END ASC,
            CASE WHEN :sort = 'PRICE_HIGH_TO_LOW' THEN p.price END DESC,
            CASE WHEN :sort = 'RATING_HIGH_TO_LOW' THEN p.rating END DESC,
            CASE WHEN :sort = 'TITLE_ASCENDING' THEN p.title END COLLATE NOCASE ASC,
            CASE WHEN :sort = 'DEFAULT' THEN p.id END ASC
        """
    )
    fun pagingSource(
        search: String,
        category: String?,
        sort: String,
        deletedState: LocalProductState = LocalProductState.DELETED,
    ): PagingSource<Int, ProductWithFavoriteEntity>

    @Query(
        """
        SELECT p.id, p.title, p.description, p.price, p.category, p.brand, p.rating, p.stock,
               p.thumbnail, p.images, p.source, p.localState,
               CASE WHEN f.productId IS NULL THEN 0 ELSE 1 END AS isFavorite
        FROM products p
        LEFT JOIN favorites f ON p.id = f.productId
        WHERE p.id = :productId AND p.localState != :deletedState
        """
    )
    fun observeProduct(
        productId: Long,
        deletedState: LocalProductState = LocalProductState.DELETED,
    ): Flow<ProductWithFavoriteEntity?>

    @Query(
        """
        SELECT p.id, p.title, p.description, p.price, p.category, p.brand, p.rating, p.stock,
               p.thumbnail, p.images, p.source, p.localState,
               1 AS isFavorite
        FROM products p
        INNER JOIN favorites f ON p.id = f.productId
        WHERE p.localState != :deletedState
        ORDER BY f.createdAt DESC
        """
    )
    fun favoritesPagingSource(
        deletedState: LocalProductState = LocalProductState.DELETED,
    ): PagingSource<Int, ProductWithFavoriteEntity>

    @Query("SELECT DISTINCT category FROM products WHERE localState != :deletedState ORDER BY category ASC")
    fun observeCategories(
        deletedState: LocalProductState = LocalProductState.DELETED,
    ): Flow<List<String>>

    @Upsert
    suspend fun upsertProducts(products: List<ProductEntity>)

    @Upsert
    suspend fun upsertProduct(product: ProductEntity)

    @Query("SELECT * FROM products WHERE id = :productId")
    suspend fun getProductById(productId: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE source = 'REMOTE' AND localState IN ('UPDATED', 'DELETED')")
    suspend fun getChangedRemoteProducts(): List<ProductEntity>

    @Query("DELETE FROM products WHERE source = 'REMOTE' AND localState = 'SYNCED'")
    suspend fun deleteSyncedRemoteProducts()

    @Query("DELETE FROM products WHERE source = 'LOCAL' AND localState = 'CREATED'")
    suspend fun deleteCreatedLocalProducts()

    @Query("UPDATE products SET localState = 'SYNCED' WHERE source = 'REMOTE' AND localState IN ('UPDATED', 'DELETED')")
    suspend fun clearDeletedAndUpdatedRemoteState()

    @Query("DELETE FROM products WHERE id = :productId")
    suspend fun deleteById(productId: Long)

    @Query("SELECT MAX(id) FROM products WHERE source = 'LOCAL'")
    suspend fun getMaxLocalId(): Long?
}

@Dao
interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE productId = :productId")
    suspend fun deleteFavorite(productId: Long)
}

@Dao
interface ProductRemoteKeysDao {
    @Query("SELECT * FROM product_remote_keys WHERE productId = :productId")
    suspend fun remoteKeyByProductId(productId: Long): ProductRemoteKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(keys: List<ProductRemoteKeyEntity>)

    @Query("DELETE FROM product_remote_keys")
    suspend fun clearRemoteKeys()
}
