package com.ibi.storefront

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.ibi.storefront.core.model.Product
import com.ibi.storefront.core.model.ProductInput
import com.ibi.storefront.core.model.ProductQuery
import com.ibi.storefront.core.model.ProductRepository
import com.ibi.storefront.core.model.ProductSort
import com.ibi.storefront.core.model.LocalProductState
import com.ibi.storefront.core.model.ProductSource
import com.ibi.storefront.feature.producteditor.ProductEditorAction
import com.ibi.storefront.feature.producteditor.ProductEditorViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProductEditorViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context = mockk<Context>().apply {
        every { getString(R.string.validation_title_required) } returns "Title is required"
        every { getString(R.string.validation_price_required) } returns "Price is required"
        every { getString(R.string.validation_price_positive) } returns "Price must be positive"
        every { getString(R.string.validation_category_required) } returns "Category is required"
        every { getString(R.string.validation_stock_required) } returns "Stock is required"
        every { getString(R.string.validation_stock_non_negative) } returns "Stock cannot be negative"
        every { getString(R.string.error_save_product) } returns "Unable to save product"
    }

    @Test
    fun submit_withEmptyTitle_setsTitleError() = runTest {
        val repository = FakeProductRepository()
        val viewModel = ProductEditorViewModel(SavedStateHandle(), context, repository)

        viewModel.onAction(ProductEditorAction.PriceChanged("10"))
        viewModel.onAction(ProductEditorAction.CategoryChanged("phones"))
        viewModel.onAction(ProductEditorAction.StockChanged("4"))
        viewModel.onAction(ProductEditorAction.Submit)

        assertThat(viewModel.uiState.value.titleError).isEqualTo("Title is required")
    }

    @Test
    fun submit_withInvalidPrice_setsPriceError() = runTest {
        val repository = FakeProductRepository()
        val viewModel = ProductEditorViewModel(SavedStateHandle(), context, repository)

        viewModel.onAction(ProductEditorAction.TitleChanged("Headphones"))
        viewModel.onAction(ProductEditorAction.PriceChanged("-1"))
        viewModel.onAction(ProductEditorAction.CategoryChanged("audio"))
        viewModel.onAction(ProductEditorAction.StockChanged("4"))
        viewModel.onAction(ProductEditorAction.Submit)

        assertThat(viewModel.uiState.value.priceError).isEqualTo("Price must be positive")
    }

    @Test
    fun submit_withInvalidStock_setsStockError() = runTest {
        val repository = FakeProductRepository()
        val viewModel = ProductEditorViewModel(SavedStateHandle(), context, repository)

        viewModel.onAction(ProductEditorAction.TitleChanged("Headphones"))
        viewModel.onAction(ProductEditorAction.PriceChanged("199.99"))
        viewModel.onAction(ProductEditorAction.CategoryChanged("audio"))
        viewModel.onAction(ProductEditorAction.StockChanged("-2"))
        viewModel.onAction(ProductEditorAction.Submit)

        assertThat(viewModel.uiState.value.stockError).isEqualTo("Stock cannot be negative")
    }

    @Test
    fun submit_withValidInput_marksSaved() = runTest {
        val repository = FakeProductRepository()
        val viewModel = ProductEditorViewModel(SavedStateHandle(), context, repository)

        viewModel.onAction(ProductEditorAction.TitleChanged("Headphones"))
        viewModel.onAction(ProductEditorAction.DescriptionChanged("Noise cancelling"))
        viewModel.onAction(ProductEditorAction.PriceChanged("199.99"))
        viewModel.onAction(ProductEditorAction.CategoryChanged("audio"))
        viewModel.onAction(ProductEditorAction.BrandChanged("Acme"))
        viewModel.onAction(ProductEditorAction.StockChanged("8"))
        viewModel.onAction(ProductEditorAction.ThumbnailChanged("https://example.com/item.png"))
        viewModel.onAction(ProductEditorAction.Submit)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isSaved).isTrue()
        assertThat(repository.createdInputs).hasSize(1)
        assertThat(repository.createdInputs.first().title).isEqualTo("Headphones")
    }

    private class FakeProductRepository : ProductRepository {
        val createdInputs = mutableListOf<ProductInput>()

        override fun observeProducts(query: ProductQuery): Flow<androidx.paging.PagingData<Product>> = emptyFlow()

        override fun observeProduct(productId: Long): Flow<Product?> = MutableStateFlow(null)

        override fun observeCategories(): Flow<List<String>> = MutableStateFlow(emptyList())

        override suspend fun refresh(): Result<Unit> = Result.success(Unit)

        override suspend fun createProduct(input: ProductInput): Result<Unit> {
            createdInputs += input
            return Result.success(Unit)
        }

        override suspend fun updateProduct(productId: Long, input: ProductInput): Result<Unit> = Result.success(Unit)

        override suspend fun deleteProduct(productId: Long): Result<Unit> = Result.success(Unit)

        override suspend fun resetLocalChanges(): Result<Unit> = Result.success(Unit)
    }
}
