package com.ibi.storefront.data.products

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.ibi.storefront.core.common.IoDispatcher
import com.ibi.storefront.core.common.ValidateProductInputUseCase
import com.ibi.storefront.core.database.ProductDao
import com.ibi.storefront.core.database.ProductEntity
import com.ibi.storefront.core.database.ProductRemoteKeyEntity
import com.ibi.storefront.core.database.ProductRemoteKeysDao
import com.ibi.storefront.core.database.StoreFrontDatabase
import com.ibi.storefront.core.model.AppError
import com.ibi.storefront.core.model.LocalProductState
import com.ibi.storefront.core.model.Product
import com.ibi.storefront.core.model.ProductInput
import com.ibi.storefront.core.model.ProductQuery
import com.ibi.storefront.core.model.ProductRepository
import com.ibi.storefront.core.model.ProductSource
import com.ibi.storefront.core.network.ProductApi
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@OptIn(ExperimentalPagingApi::class)
@Singleton
class OfflineFirstProductRepository @Inject constructor(
    private val api: ProductApi,
    private val database: StoreFrontDatabase,
    private val productDao: ProductDao,
    private val remoteMediator: ProductRemoteMediator,
    private val validateProductInput: ValidateProductInputUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ProductRepository {

    override fun observeProducts(query: ProductQuery): Flow<PagingData<Product>> {
        return Pager(
            config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
            remoteMediator = remoteMediator,
            pagingSourceFactory = {
                productDao.entityPagingSource(
                    search = query.searchTerm.trim(),
                    category = query.category,
                    sort = query.sort.toQuerySortKey(),
                )
            },
        ).flow.map { pagingData -> pagingData.map { it.toDomain() } }
    }

    override fun observeProduct(productId: Long): Flow<Product?> {
        return productDao.observeProduct(productId).map { it?.toDomain() }
    }

    override fun observeCategories(): Flow<List<String>> = productDao.observeCategories()

    override suspend fun refresh(): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            remoteMediator.load(
                LoadType.REFRESH,
                PagingState(
                    pages = emptyList(),
                    anchorPosition = null,
                    config = PagingConfig(pageSize = PAGE_SIZE),
                    leadingPlaceholderCount = 0,
                ),
            ).also { result ->
                if (result is RemoteMediator.MediatorResult.Error) throw result.throwable
            }
            Unit
        }
    }

    override suspend fun createProduct(input: ProductInput): Result<Unit> = withContext(ioDispatcher) {
        validateProductInput(input)?.let { return@withContext Result.failure(ProductValidationException(it)) }
        runCatching {
            val localId = ((productDao.getMaxLocalId() ?: LOCAL_PRODUCT_ID_BASE) + 1)
                .coerceAtLeast(LOCAL_PRODUCT_ID_BASE + 1)
            productDao.upsertProduct(
                ProductEntity(
                    id = localId,
                    title = input.title,
                    description = input.description,
                    price = input.price,
                    category = input.category,
                    brand = input.brand,
                    rating = 0.0,
                    stock = input.stock,
                    thumbnail = input.thumbnail,
                    images = input.images,
                    source = ProductSource.LOCAL,
                    localState = LocalProductState.CREATED,
                    lastUpdatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    override suspend fun updateProduct(productId: Long, input: ProductInput): Result<Unit> = withContext(ioDispatcher) {
        validateProductInput(input)?.let { return@withContext Result.failure(ProductValidationException(it)) }
        runCatching {
            val existing = productDao.getProductById(productId) ?: throw ProductRepositoryException(AppError.NotFound)
            productDao.upsertProduct(existing.toUpdatedEntity(input, System.currentTimeMillis()))
        }
    }

    override suspend fun deleteProduct(productId: Long): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val existing = productDao.getProductById(productId) ?: throw ProductRepositoryException(AppError.NotFound)
            if (existing.source == ProductSource.LOCAL) {
                productDao.deleteById(productId)
            } else {
                productDao.upsertProduct(existing.copy(localState = LocalProductState.DELETED))
            }
        }
    }

    override suspend fun resetLocalChanges(): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val restoredProducts = fetchAllRemoteProducts()
            val timestamp = System.currentTimeMillis()
            database.withTransaction {
                productDao.deleteCreatedLocalProducts()
                productDao.upsertProducts(
                    restoredProducts.map { dto -> dto.toEntity(timestamp = timestamp) },
                )
            }
        }
    }

    private suspend fun fetchAllRemoteProducts(): List<com.ibi.storefront.core.network.ProductDto> {
        val products = mutableListOf<com.ibi.storefront.core.network.ProductDto>()
        var skip = 0
        var total = Int.MAX_VALUE
        do {
            val response = api.getProducts(limit = RESET_SYNC_FETCH_LIMIT, skip = skip)
            total = response.total
            products += response.products
            skip += response.products.size
        } while (products.size < total && response.products.isNotEmpty())
        return products
    }

    private class ProductRepositoryException(val error: AppError) : IllegalStateException()
    private class ProductValidationException(val error: AppError) : IllegalArgumentException()

    companion object {
        private const val PAGE_SIZE = 20
        private const val RESET_SYNC_FETCH_LIMIT = 100
        private const val LOCAL_PRODUCT_ID_BASE = 1_000_000L
    }
}
