package com.ibi.storefront.core.common

import com.ibi.storefront.core.model.AppError

sealed interface OperationResult<out T> {
    data class Success<T>(val value: T) : OperationResult<T>
    data class Failure(val error: AppError) : OperationResult<Nothing>
}
