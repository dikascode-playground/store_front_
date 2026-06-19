package com.ibi.storefront.core.model

sealed interface AuthState {
    data object LoggedOut : AuthState
    data class Locked(val username: String, val biometricEnabled: Boolean) : AuthState
    data class Authenticated(val session: UserSession) : AuthState
}

data class UserSession(
    val sessionId: String,
    val username: String,
    val biometricEnabled: Boolean,
)
