package com.ibi.storefront.feature.products

import android.content.Context
import com.ibi.storefront.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ibi.storefront.core.model.FavoritesRepository
import com.ibi.storefront.core.model.Product
import com.ibi.storefront.core.model.ProductQuery
import com.ibi.storefront.core.model.ProductRepository
import com.ibi.storefront.core.model.ProductSort
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProductsUiState(
    val query: String = "",
    val selectedCategory: String? = null,
    val selectedSort: ProductSort = ProductSort.Default,
    val isRefreshing: Boolean = false,
    val activeSyncAction: ProductsSyncAction? = null,
    val categories: List<String> = emptyList(),
    val statusMessage: String? = null,
    val statusIsError: Boolean = false,
)

enum class ProductsSyncAction {
    Refresh,
    ResetLocalChanges,
}

@HiltViewModel
class ProductsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val productRepository: ProductRepository,
    private val favoritesRepository: FavoritesRepository,
) : ViewModel() {
    private data class UiChromeState(
        val query: String,
        val selectedCategory: String?,
        val selectedSort: ProductSort,
        val isRefreshing: Boolean,
        val activeSyncAction: ProductsSyncAction?,
        val statusMessage: String?,
        val statusIsError: Boolean,
    )

    private val searchTerm = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow<String?>(null)
    private val selectedSort = MutableStateFlow(ProductSort.Default)
    private val isRefreshing = MutableStateFlow(false)
    private val activeSyncAction = MutableStateFlow<ProductsSyncAction?>(null)
    private val statusMessage = MutableStateFlow<String?>(null)
    private val statusIsError = MutableStateFlow(false)

    private val queryControls = combine(
        searchTerm,
        selectedCategory,
        selectedSort,
    ) { query, category, sort ->
        Triple(query, category, sort)
    }

    private val uiChromeState = combine(
        queryControls,
        isRefreshing,
        activeSyncAction,
        statusMessage,
        statusIsError,
    ) { queryControls, refreshing, syncAction, message, isError ->
        UiChromeState(
            query = queryControls.first,
            selectedCategory = queryControls.second,
            selectedSort = queryControls.third,
            isRefreshing = refreshing,
            activeSyncAction = syncAction,
            statusMessage = message,
            statusIsError = isError,
        )
    }

    val uiState: StateFlow<ProductsUiState> = combine(
        uiChromeState,
        productRepository.observeCategories(),
    ) { chrome, categories ->
        ProductsUiState(
            query = chrome.query,
            selectedCategory = chrome.selectedCategory,
            selectedSort = chrome.selectedSort,
            isRefreshing = chrome.isRefreshing,
            activeSyncAction = chrome.activeSyncAction,
            statusMessage = chrome.statusMessage,
            statusIsError = chrome.statusIsError,
            categories = categories,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProductsUiState(),
    )

    val products: Flow<PagingData<Product>> = queryControls.map { (query, category, sort) ->
        ProductQuery(
            searchTerm = query,
            category = category,
            sort = sort,
        )
    }.flatMapLatest { query ->
        productRepository.observeProducts(query)
    }.cachedIn(viewModelScope)

    fun onSearchChanged(value: String) {
        searchTerm.value = value
    }

    fun onSortSelected(sort: ProductSort) {
        selectedSort.value = sort
    }

    fun onCategorySelected(category: String?) {
        selectedCategory.value = category
    }

    fun onFavoriteToggle(productId: Long, favorite: Boolean) {
        viewModelScope.launch {
            favoritesRepository.setFavorite(productId, favorite)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            isRefreshing.value = true
            activeSyncAction.value = ProductsSyncAction.Refresh
            val result = productRepository.refresh()
            isRefreshing.value = false
            activeSyncAction.value = null
            statusIsError.value = result.isFailure
            statusMessage.value = if (result.isSuccess) {
                context.getString(R.string.status_refresh_success)
            } else {
                context.getString(R.string.status_refresh_failure)
            }
        }
    }

    fun resetLocalChanges() {
        viewModelScope.launch {
            isRefreshing.value = true
            activeSyncAction.value = ProductsSyncAction.ResetLocalChanges
            val result = productRepository.resetLocalChanges()
            isRefreshing.value = false
            activeSyncAction.value = null
            statusIsError.value = result.isFailure
            statusMessage.value = if (result.isSuccess) {
                context.getString(R.string.status_reset_local_changes_success)
            } else {
                context.getString(R.string.status_reset_local_changes_failure)
            }
        }
    }

    fun clearStatusMessage() {
        statusMessage.value = null
        statusIsError.value = false
    }
}
