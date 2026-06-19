package com.ibi.storefront.app.navigation

sealed class AppDestination(val route: String) {
    data object Login : AppDestination("login")
    data object SignUp : AppDestination("sign_up")
    data object Products : AppDestination("products")
    data object Favorites : AppDestination("favorites")
    data object ProductDetails : AppDestination("product_details/{productId}") {
        fun createRoute(productId: Long): String = "product_details/$productId"
    }
    data object ProductEditor : AppDestination("product_editor?productId={productId}") {
        fun createRoute(productId: Long? = null): String {
            return if (productId == null) {
                "product_editor"
            } else {
                "product_editor?productId=$productId"
            }
        }
    }
    data object Settings : AppDestination("settings")
}
