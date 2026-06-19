package com.ibi.storefront.data.products

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.ibi.storefront.core.common.IoDispatcher
import com.ibi.storefront.core.database.FavoriteDao
import com.ibi.storefront.core.database.FavoriteEntity
import com.ibi.storefront.core.database.ProductDao
import com.ibi.storefront.core.model.FavoritesRepository
import com.ibi.storefront.core.model.Product
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class RoomFavoritesRepository @Inject constructor(
    private val productDao: ProductDao,
    private val favoriteDao: FavoriteDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : FavoritesRepository {
    override fun observeFavorites(): Flow<PagingData<Product>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { productDao.favoritesPagingSource() },
        ).flow.map { pagingData -> pagingData.map { it.toDomain() } }
    }

    override suspend fun setFavorite(productId: Long, favorite: Boolean): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            if (favorite) {
                favoriteDao.upsertFavorite(
                    FavoriteEntity(
                        productId = productId,
                        createdAt = System.currentTimeMillis(),
                    ),
                )
            } else {
                favoriteDao.deleteFavorite(productId)
            }
        }
    }
}
