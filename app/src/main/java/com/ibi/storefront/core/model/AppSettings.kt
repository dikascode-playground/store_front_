package com.ibi.storefront.core.model

data class AppSettings(
    val theme: AppTheme = AppTheme.Light,
    val language: AppLanguage = AppLanguage.English,
    val biometricEnabled: Boolean = false,
)

enum class AppTheme {
    Light,
    Dark,
}

enum class AppLanguage(val languageTag: String) {
    English("en"),
    Hebrew("iw"),
}
