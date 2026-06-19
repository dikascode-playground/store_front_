package com.ibi.storefront.core.model

sealed interface AppError {
    data object NoConnection : AppError
    data object Unauthorized : AppError
    data object NotFound : AppError
    data object InvalidCredentials : AppError
    data object StorageFailure : AppError
    data class Validation(val message: String) : AppError
    data class Unexpected(val cause: Throwable? = null) : AppError
}
