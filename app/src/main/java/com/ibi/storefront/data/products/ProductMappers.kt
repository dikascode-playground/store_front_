package com.ibi.storefront.data.products

import com.ibi.storefront.core.database.ProductEntity
import com.ibi.storefront.core.database.ProductWithFavoriteEntity
import com.ibi.storefront.core.model.LocalProductState
import com.ibi.storefront.core.model.Product
import com.ibi.storefront.core.model.ProductInput
import com.ibi.storefront.core.model.ProductSort
import com.ibi.storefront.core.model.ProductSource
import com.ibi.storefront.core.network.ProductDto

internal fun ProductDto.toEntity(
    localState: LocalProductState = LocalProductState.SYNCED,
    timestamp: Long,
): ProductEntity {
    return ProductEntity(
        id = id,
        title = title,
        description = description,
        price = price,
        category = category,
        brand = brand,
        rating = rating,
        stock = stock,
        thumbnail = thumbnail,
        images = images,
        source = ProductSource.REMOTE,
        localState = localState,
        lastUpdatedAt = timestamp,
    )
}

internal fun ProductWithFavoriteEntity.toDomain(): Product {
    return Product(
        id = id,
        title = title,
        description = description,
        price = price,
        category = category,
        brand = brand,
        rating = rating,
        stock = stock,
        thumbnail = thumbnail,
        images = images,
        source = source,
        localState = localState,
        isFavorite = isFavorite,
    )
}

internal fun ProductEntity.toDomain(isFavorite: Boolean = false): Product {
    return Product(
        id = id,
        title = title,
        description = description,
        price = price,
        category = category,
        brand = brand,
        rating = rating,
        stock = stock,
        thumbnail = thumbnail,
        images = images,
        source = source,
        localState = localState,
        isFavorite = isFavorite,
    )
}

internal fun ProductEntity.toUpdatedEntity(
    input: ProductInput,
    timestamp: Long,
): ProductEntity {
    return copy(
        title = input.title,
        description = input.description,
        price = input.price,
        category = input.category,
        brand = input.brand,
        stock = input.stock,
        thumbnail = input.thumbnail,
        images = input.images,
        localState = if (source == ProductSource.LOCAL) {
            LocalProductState.CREATED
        } else {
            LocalProductState.UPDATED
        },
        lastUpdatedAt = timestamp,
    )
}

internal fun ProductSort.toQuerySortKey(): String {
    return when (this) {
        ProductSort.Default -> "DEFAULT"
        ProductSort.PriceLowToHigh -> "PRICE_LOW_TO_HIGH"
        ProductSort.PriceHighToLow -> "PRICE_HIGH_TO_LOW"
        ProductSort.RatingHighToLow -> "RATING_HIGH_TO_LOW"
        ProductSort.TitleAscending -> "TITLE_ASCENDING"
    }
}
