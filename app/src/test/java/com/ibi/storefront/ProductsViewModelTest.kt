package com.ibi.storefront

import android.content.Context
import androidx.paging.PagingData
import com.google.common.truth.Truth.assertThat
import com.ibi.storefront.core.model.FavoritesRepository
import com.ibi.storefront.core.model.LocalProductState
import com.ibi.storefront.core.model.Product
import com.ibi.storefront.core.model.ProductQuery
import com.ibi.storefront.core.model.ProductRepository
import com.ibi.storefront.core.model.ProductSort
import com.ibi.storefront.core.model.ProductSource
import com.ibi.storefront.feature.products.ProductsViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProductsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context = mockk<Context>().apply {
        every { getString(R.string.status_reset_local_changes_success) } returns "Local product changes were reset"
        every { getString(R.string.status_reset_local_changes_failure) } returns "Could not reset local changes right now"
    }

    @Test
    fun sortSearchAndCategory_updatesUiState() = runTest {
        val viewModel = ProductsViewModel(
            context = context,
            productRepository = FakeProductRepository(),
            favoritesRepository = FakeFavoritesRepository(),
        )
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }

        viewModel.onSearchChanged("tablet")
        viewModel.onCategorySelected("phones")
        viewModel.onSortSelected(ProductSort.RatingHighToLow)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.query).isEqualTo("tablet")
        assertThat(viewModel.uiState.value.selectedCategory).isEqualTo("phones")
        assertThat(viewModel.uiState.value.selectedSort).isEqualTo(ProductSort.RatingHighToLow)
        collector.cancel()
    }

    @Test
    fun resetLocalChanges_success_setsSuccessMessage() = runTest {
        val repository = FakeProductRepository(resetSucceeds = true)
        val viewModel = ProductsViewModel(
            context = context,
            productRepository = repository,
            favoritesRepository = FakeFavoritesRepository(),
        )
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }

        viewModel.resetLocalChanges()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.statusMessage).isEqualTo("Local product changes were reset")
        collector.cancel()
    }

    @Test
    fun resetLocalChanges_failure_setsFailureMessage() = runTest {
        val repository = FakeProductRepository(resetSucceeds = false)
        val viewModel = ProductsViewModel(
            context = context,
            productRepository = repository,
            favoritesRepository = FakeFavoritesRepository(),
        )
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }

        viewModel.resetLocalChanges()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.statusMessage).isEqualTo("Could not reset local changes right now")
        collector.cancel()
    }

    private class FakeProductRepository(
        private val resetSucceeds: Boolean = true,
    ) : ProductRepository {
        override fun observeProducts(query: ProductQuery): Flow<PagingData<Product>> = emptyFlow()

        override fun observeProduct(productId: Long): Flow<Product?> = MutableStateFlow(null)

        override fun observeCategories(): Flow<List<String>> = MutableStateFlow(listOf("audio", "phones"))

        override suspend fun refresh(): Result<Unit> = Result.success(Unit)

        override suspend fun createProduct(input: com.ibi.storefront.core.model.ProductInput): Result<Unit> = Result.success(Unit)

        override suspend fun updateProduct(productId: Long, input: com.ibi.storefront.core.model.ProductInput): Result<Unit> = Result.success(Unit)

        override suspend fun deleteProduct(productId: Long): Result<Unit> = Result.success(Unit)

        override suspend fun resetLocalChanges(): Result<Unit> {
            return if (resetSucceeds) Result.success(Unit) else Result.failure(IllegalStateException("network"))
        }
    }

    private class FakeFavoritesRepository : FavoritesRepository {
        override fun observeFavorites(): Flow<PagingData<Product>> = emptyFlow()

        override suspend fun setFavorite(productId: Long, favorite: Boolean): Result<Unit> = Result.success(Unit)
    }
}
