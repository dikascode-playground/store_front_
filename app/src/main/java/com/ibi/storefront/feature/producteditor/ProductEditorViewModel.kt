package com.ibi.storefront.feature.producteditor

import android.content.Context
import com.ibi.storefront.R
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ibi.storefront.core.model.ProductInput
import com.ibi.storefront.core.model.ProductRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductEditorUiState(
    val productId: Long? = null,
    val isEditMode: Boolean = false,
    val title: String = "",
    val description: String = "",
    val price: String = "",
    val category: String = "",
    val categories: List<String> = emptyList(),
    val brand: String = "",
    val stock: String = "",
    val thumbnail: String = "",
    val titleError: String? = null,
    val descriptionError: String? = null,
    val priceError: String? = null,
    val categoryError: String? = null,
    val stockError: String? = null,
    val submitError: String? = null,
    val isSubmitting: Boolean = false,
    val isSaved: Boolean = false,
)

sealed interface ProductEditorAction {
    data class TitleChanged(val value: String) : ProductEditorAction
    data class DescriptionChanged(val value: String) : ProductEditorAction
    data class PriceChanged(val value: String) : ProductEditorAction
    data class CategoryChanged(val value: String) : ProductEditorAction
    data class BrandChanged(val value: String) : ProductEditorAction
    data class StockChanged(val value: String) : ProductEditorAction
    data class ThumbnailChanged(val value: String) : ProductEditorAction
    data object Submit : ProductEditorAction
}

@HiltViewModel
class ProductEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val productRepository: ProductRepository,
) : ViewModel() {
    private val existingProductId: Long? = savedStateHandle.get<String>("productId")?.toLongOrNull()

    private val _uiState = MutableStateFlow(
        ProductEditorUiState(
            productId = existingProductId,
            isEditMode = existingProductId != null,
        ),
    )
    val uiState: StateFlow<ProductEditorUiState> = _uiState.asStateFlow()

    init {
        productRepository.observeCategories()
            .onEach { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
            .launchIn(viewModelScope)

        if (existingProductId != null) {
            viewModelScope.launch {
                val product = productRepository.observeProduct(existingProductId)
                    .filterNotNull()
                    .first()
                _uiState.update {
                    it.copy(
                        title = product.title,
                        description = product.description,
                        price = product.price.toString(),
                        category = product.category,
                        brand = product.brand.orEmpty(),
                        stock = product.stock.toString(),
                        thumbnail = product.thumbnail,
                    )
                }
            }
        }
    }

    fun onAction(action: ProductEditorAction) {
        when (action) {
            is ProductEditorAction.TitleChanged -> updateState { it.copy(title = action.value, titleError = null, submitError = null) }
            is ProductEditorAction.DescriptionChanged -> updateState { it.copy(description = action.value, descriptionError = null, submitError = null) }
            is ProductEditorAction.PriceChanged -> updateState { it.copy(price = action.value, priceError = null, submitError = null) }
            is ProductEditorAction.CategoryChanged -> updateState { it.copy(category = action.value, categoryError = null, submitError = null) }
            is ProductEditorAction.BrandChanged -> updateState { it.copy(brand = action.value, submitError = null) }
            is ProductEditorAction.StockChanged -> updateState { it.copy(stock = action.value, stockError = null, submitError = null) }
            is ProductEditorAction.ThumbnailChanged -> updateState { it.copy(thumbnail = action.value, submitError = null) }
            ProductEditorAction.Submit -> submit()
        }
    }

    private fun submit() {
        val state = _uiState.value
        val price = state.price.toDoubleOrNull()
        val stock = state.stock.toIntOrNull()

        val titleError = if (state.title.isBlank()) context.getString(R.string.validation_title_required) else null
        val priceError = when {
            state.price.isBlank() -> context.getString(R.string.validation_price_required)
            price == null || price <= 0.0 -> context.getString(R.string.validation_price_positive)
            else -> null
        }
        val categoryError = if (state.category.isBlank()) context.getString(R.string.validation_category_required) else null
        val stockError = when {
            state.stock.isBlank() -> context.getString(R.string.validation_stock_required)
            stock == null || stock < 0 -> context.getString(R.string.validation_stock_non_negative)
            else -> null
        }

        if (titleError != null || priceError != null || categoryError != null || stockError != null) {
            _uiState.update {
                it.copy(
                    titleError = titleError,
                    priceError = priceError,
                    categoryError = categoryError,
                    stockError = stockError,
                )
            }
            return
        }

        val input = ProductInput(
            title = state.title.trim(),
            description = state.description.trim(),
            price = price ?: 0.0,
            category = state.category.trim(),
            brand = state.brand.trim().ifBlank { null },
            stock = stock ?: 0,
            thumbnail = state.thumbnail.trim(),
            images = listOfNotNull(state.thumbnail.trim().ifBlank { null }),
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, submitError = null) }
            val result = if (state.isEditMode && state.productId != null) {
                productRepository.updateProduct(state.productId, input)
            } else {
                productRepository.createProduct(input)
            }
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(isSubmitting = false, isSaved = true)
                } else {
                    it.copy(isSubmitting = false, submitError = context.getString(R.string.error_save_product))
                }
            }
        }
    }

    private fun updateState(update: (ProductEditorUiState) -> ProductEditorUiState) {
        _uiState.update(update)
    }
}
