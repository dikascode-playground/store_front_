package com.ibi.storefront.feature.productdetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ibi.storefront.core.model.FavoritesRepository
import com.ibi.storefront.core.model.Product
import com.ibi.storefront.core.model.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProductDetailsUiState(
    val productId: Long = 0L,
    val product: Product? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val statusMessage: String? = null,
)

@HiltViewModel
class ProductDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val productRepository: ProductRepository,
    private val favoritesRepository: FavoritesRepository,
) : ViewModel() {
    private val productId: Long = checkNotNull(savedStateHandle["productId"])
    private val isDeleted = MutableStateFlow(false)

    val uiState: StateFlow<ProductDetailsUiState> = combine(
        productRepository.observeProduct(productId),
        isDeleted,
    ) { product, deleted ->
            ProductDetailsUiState(
                productId = productId,
                product = product,
                isLoading = product == null,
                isDeleted = deleted,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProductDetailsUiState(productId = productId),
        )

    fun toggleFavorite() {
        val product = uiState.value.product ?: return
        viewModelScope.launch {
            favoritesRepository.setFavorite(product.id, !product.isFavorite)
        }
    }

    fun deleteProduct() {
        val product = uiState.value.product ?: return
        viewModelScope.launch {
            val result = productRepository.deleteProduct(product.id)
            if (result.isSuccess) {
                isDeleted.value = true
            }
        }
    }
}
