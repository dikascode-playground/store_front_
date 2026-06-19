package com.ibi.storefront

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.ibi.storefront.core.common.ValidateProductInputUseCase
import com.ibi.storefront.core.database.FavoriteEntity
import com.ibi.storefront.core.database.StoreFrontDatabase
import com.ibi.storefront.core.model.LocalProductState
import com.ibi.storefront.core.model.ProductSource
import com.ibi.storefront.core.network.ProductApi
import com.ibi.storefront.core.network.ProductDto
import com.ibi.storefront.core.network.ProductResponseDto
import com.ibi.storefront.data.products.OfflineFirstProductRepository
import com.ibi.storefront.data.products.ProductRemoteMediator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OfflineFirstProductRepositoryTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private lateinit var database: StoreFrontDatabase
    private lateinit var api: FakeProductApi
    private lateinit var repository: OfflineFirstProductRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, StoreFrontDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        api = FakeProductApi(
            products = listOf(
                productDto(id = 1, title = "Remote One", price = 10.0),
                productDto(id = 2, title = "Remote Two", price = 20.0),
                productDto(id = 3, title = "Remote Three", price = 30.0),
            ),
        )
        val productDao = database.productDao()
        repository = OfflineFirstProductRepository(
            api = api,
            database = database,
            productDao = productDao,
            remoteMediator = ProductRemoteMediator(
                api = api,
                database = database,
                productDao = productDao,
                remoteKeysDao = database.remoteKeysDao(),
            ),
            validateProductInput = ValidateProductInputUseCase(),
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun resetLocalChanges_restoresUpdatedAndDeletedRemoteRows_andRemovesLocalCreated_andPreservesFavorites() = runTest {
        val productDao = database.productDao()
        val favoriteDao = database.favoriteDao()

        productDao.upsertProducts(
            listOf(
                remoteProductEntity(
                    id = 1,
                    title = "Locally Updated",
                    localState = LocalProductState.UPDATED,
                ),
                remoteProductEntity(
                    id = 2,
                    title = "Locally Deleted",
                    localState = LocalProductState.DELETED,
                ),
                remoteProductEntity(
                    id = 3,
                    title = "Already Synced",
                    localState = LocalProductState.SYNCED,
                ),
                localCreatedEntity(id = 1_000_001L, title = "Draft Product"),
            ),
        )
        favoriteDao.upsertFavorite(FavoriteEntity(productId = 1, createdAt = 1L))

        val result = repository.resetLocalChanges()

        assertThat(result.isSuccess).isTrue()
        val restoredOne = productDao.getProductById(1)!!
        val restoredTwo = productDao.getProductById(2)!!
        val restoredThree = productDao.getProductById(3)!!
        assertThat(restoredOne.title).isEqualTo("Remote One")
        assertThat(restoredOne.localState).isEqualTo(LocalProductState.SYNCED)
        assertThat(restoredTwo.title).isEqualTo("Remote Two")
        assertThat(restoredTwo.localState).isEqualTo(LocalProductState.SYNCED)
        assertThat(restoredThree.title).isEqualTo("Remote Three")
        assertThat(productDao.getProductById(1_000_001L)).isNull()

        val favoritedProduct = repository.observeProduct(1).first()
        assertThat(favoritedProduct?.isFavorite).isTrue()
    }

    @Test
    fun resetLocalChanges_whenApiFails_leavesDatabaseUntouched() = runTest {
        val productDao = database.productDao()
        productDao.upsertProducts(
            listOf(
                remoteProductEntity(
                    id = 1,
                    title = "Locally Updated",
                    localState = LocalProductState.UPDATED,
                ),
                localCreatedEntity(id = 1_000_001L, title = "Draft Product"),
            ),
        )
        api.failRequests = true

        val result = repository.resetLocalChanges()

        assertThat(result.isFailure).isTrue()
        assertThat(productDao.getProductById(1)?.title).isEqualTo("Locally Updated")
        assertThat(productDao.getProductById(1)?.localState).isEqualTo(LocalProductState.UPDATED)
        assertThat(productDao.getProductById(1_000_001L)?.title).isEqualTo("Draft Product")
    }

    private fun productDto(
        id: Long,
        title: String,
        price: Double,
    ) = ProductDto(
        id = id,
        title = title,
        description = "$title description",
        price = price,
        category = "beauty",
        brand = "Brand",
        rating = 4.5,
        stock = 20,
        thumbnail = "https://example.com/$id.png",
        images = listOf("https://example.com/$id.png"),
    )

    private fun remoteProductEntity(
        id: Long,
        title: String,
        localState: LocalProductState,
    ) = com.ibi.storefront.core.database.ProductEntity(
        id = id,
        title = title,
        description = "$title description",
        price = 99.0,
        category = "beauty",
        brand = "Brand",
        rating = 1.0,
        stock = 1,
        thumbnail = "local-$id",
        images = listOf("local-$id"),
        source = ProductSource.REMOTE,
        localState = localState,
        lastUpdatedAt = 1L,
    )

    private fun localCreatedEntity(id: Long, title: String) = com.ibi.storefront.core.database.ProductEntity(
        id = id,
        title = title,
        description = "$title description",
        price = 15.0,
        category = "local",
        brand = null,
        rating = 0.0,
        stock = 2,
        thumbnail = "",
        images = emptyList(),
        source = ProductSource.LOCAL,
        localState = LocalProductState.CREATED,
        lastUpdatedAt = 1L,
    )

    private class FakeProductApi(
        private val products: List<ProductDto>,
    ) : ProductApi {
        var failRequests: Boolean = false

        override suspend fun getProducts(limit: Int, skip: Int): ProductResponseDto {
            if (failRequests) error("network failure")
            val slice = products.drop(skip).take(limit)
            return ProductResponseDto(
                products = slice,
                total = products.size,
                skip = skip,
                limit = limit,
            )
        }

        override suspend fun getProduct(id: Long): ProductDto {
            return products.first { it.id == id }
        }
    }
}
