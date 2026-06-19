package com.ibi.storefront.data.products

import androidx.paging.LoadType
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.ibi.storefront.core.database.ProductDao
import com.ibi.storefront.core.database.ProductEntity
import com.ibi.storefront.core.database.ProductRemoteKeyEntity
import com.ibi.storefront.core.database.ProductRemoteKeysDao
import com.ibi.storefront.core.database.StoreFrontDatabase
import com.ibi.storefront.core.model.LocalProductState
import com.ibi.storefront.core.network.ProductApi
import javax.inject.Inject

@OptIn(ExperimentalPagingApi::class)
class ProductRemoteMediator @Inject constructor(
    private val api: ProductApi,
    private val database: StoreFrontDatabase,
    private val productDao: ProductDao,
    private val remoteKeysDao: ProductRemoteKeysDao,
) : RemoteMediator<Int, ProductEntity>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, ProductEntity>,
    ): MediatorResult {
        return try {
            val pageSize = state.config.pageSize
            val skip = when (loadType) {
                LoadType.REFRESH -> 0
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    val lastItem = state.lastItemOrNull()
                        ?: return MediatorResult.Success(endOfPaginationReached = true)
                    val key = remoteKeysDao.remoteKeyByProductId(lastItem.id)
                    key?.nextKey ?: return MediatorResult.Success(endOfPaginationReached = true)
                }
            }

            val response = api.getProducts(limit = pageSize, skip = skip)
            val endOfPaginationReached = skip + response.products.size >= response.total
            val timestamp = System.currentTimeMillis()

            database.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    remoteKeysDao.clearRemoteKeys()
                    productDao.deleteSyncedRemoteProducts()
                }

                val entities = response.products.map { dto ->
                    val existing = productDao.getProductById(dto.id)
                    when {
                        existing == null -> dto.toEntity(timestamp = timestamp)
                        existing.source == com.ibi.storefront.core.model.ProductSource.LOCAL -> existing
                        existing.localState == LocalProductState.UPDATED ||
                            existing.localState == LocalProductState.DELETED -> existing
                        else -> dto.toEntity(timestamp = timestamp)
                    }
                }

                val keys = response.products.map { dto ->
                    ProductRemoteKeyEntity(
                        productId = dto.id,
                        previousKey = if (skip == 0) null else (skip - pageSize).coerceAtLeast(0),
                        nextKey = if (endOfPaginationReached) null else skip + pageSize,
                    )
                }

                productDao.upsertProducts(entities)
                remoteKeysDao.upsertAll(keys)
            }

            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (throwable: Throwable) {
            MediatorResult.Error(throwable)
        }
    }
}
