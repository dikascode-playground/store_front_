package com.ibi.storefront.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ProductApi {
    @GET("products")
    suspend fun getProducts(
        @Query("limit") limit: Int,
        @Query("skip") skip: Int,
    ): ProductResponseDto

    @GET("products/{id}")
    suspend fun getProduct(@Path("id") id: Long): ProductDto
}

@Serializable
data class ProductResponseDto(
    @SerialName("products") val products: List<ProductDto>,
    @SerialName("total") val total: Int,
    @SerialName("skip") val skip: Int,
    @SerialName("limit") val limit: Int,
)

@Serializable
data class ProductDto(
    @SerialName("id") val id: Long,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String,
    @SerialName("price") val price: Double,
    @SerialName("category") val category: String,
    @SerialName("brand") val brand: String? = null,
    @SerialName("rating") val rating: Double = 0.0,
    @SerialName("stock") val stock: Int = 0,
    @SerialName("thumbnail") val thumbnail: String = "",
    @SerialName("images") val images: List<String> = emptyList(),
)
