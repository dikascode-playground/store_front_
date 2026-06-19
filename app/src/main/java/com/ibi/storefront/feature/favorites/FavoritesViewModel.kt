package com.ibi.storefront.feature.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ibi.storefront.core.model.FavoritesRepository
import com.ibi.storefront.core.model.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
) : ViewModel() {
    val favorites: Flow<PagingData<Product>> = favoritesRepository.observeFavorites().cachedIn(viewModelScope)

    fun onFavoriteToggle(productId: Long, favorite: Boolean) {
        viewModelScope.launch {
            favoritesRepository.setFavorite(productId, favorite)
        }
    }
}
