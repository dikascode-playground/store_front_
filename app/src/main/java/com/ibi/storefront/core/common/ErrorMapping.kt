package com.ibi.storefront.core.common

import com.ibi.storefront.core.model.AppError
import java.io.IOException
import retrofit2.HttpException

fun Throwable.toAppError(): AppError {
    return when (this) {
        is IOException -> AppError.NoConnection
        is HttpException -> when (code()) {
            401 -> AppError.Unauthorized
            404 -> AppError.NotFound
            else -> AppError.Unexpected(this)
        }
        else -> AppError.Unexpected(this)
    }
}
