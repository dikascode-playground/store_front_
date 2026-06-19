package com.ibi.storefront.core.common

import com.ibi.storefront.core.model.AppError
import com.ibi.storefront.core.model.ProductInput

class ValidateProductInputUseCase {
    operator fun invoke(input: ProductInput): AppError.Validation? {
        return when {
            input.title.isBlank() -> AppError.Validation("Title is required")
            input.price <= 0.0 -> AppError.Validation("Price must be positive")
            input.stock < 0 -> AppError.Validation("Stock cannot be negative")
            input.category.isBlank() -> AppError.Validation("Category is required")
            input.title.length > 120 -> AppError.Validation("Title is too long")
            input.description.length > 600 -> AppError.Validation("Description is too long")
            else -> null
        }
    }
}
