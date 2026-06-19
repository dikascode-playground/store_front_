package com.ibi.storefront.core.model

data class Product(
    val id: Long,
    val title: String,
    val description: String,
    val price: Double,
    val category: String,
    val brand: String?,
    val rating: Double,
    val stock: Int,
    val thumbnail: String,
    val images: List<String>,
    val source: ProductSource,
    val localState: LocalProductState,
    val isFavorite: Boolean,
)

data class ProductInput(
    val title: String,
    val description: String,
    val price: Double,
    val category: String,
    val brand: String?,
    val stock: Int,
    val thumbnail: String,
    val images: List<String>,
)

data class ProductQuery(
    val searchTerm: String = "",
    val category: String? = null,
    val sort: ProductSort = ProductSort.Default,
)

enum class ProductSort {
    Default,
    PriceLowToHigh,
    PriceHighToLow,
    RatingHighToLow,
    TitleAscending,
}

enum class ProductSource {
    REMOTE,
    LOCAL,
}

enum class LocalProductState {
    SYNCED,
    CREATED,
    UPDATED,
    DELETED,
}
