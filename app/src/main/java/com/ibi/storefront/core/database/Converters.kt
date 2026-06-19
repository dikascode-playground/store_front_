package com.ibi.storefront.core.database

import androidx.room.TypeConverter
import com.ibi.storefront.core.model.LocalProductState
import com.ibi.storefront.core.model.ProductSource

class Converters {
    @TypeConverter
    fun fromImages(images: List<String>): String = images.joinToString("|")

    @TypeConverter
    fun toImages(images: String): List<String> = if (images.isBlank()) emptyList() else images.split("|")

    @TypeConverter
    fun fromSource(source: ProductSource): String = source.name

    @TypeConverter
    fun toSource(source: String): ProductSource = ProductSource.valueOf(source)

    @TypeConverter
    fun fromState(state: LocalProductState): String = state.name

    @TypeConverter
    fun toState(state: String): LocalProductState = LocalProductState.valueOf(state)
}
