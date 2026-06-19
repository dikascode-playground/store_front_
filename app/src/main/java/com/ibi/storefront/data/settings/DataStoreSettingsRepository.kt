package com.ibi.storefront.data.settings

import com.ibi.storefront.core.datastore.PreferenceStorage
import com.ibi.storefront.core.model.AppLanguage
import com.ibi.storefront.core.model.AppSettings
import com.ibi.storefront.core.model.AppTheme
import com.ibi.storefront.core.model.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val preferenceStorage: PreferenceStorage,
) : SettingsRepository {
    override val settings: Flow<AppSettings> = preferenceStorage.settings

    override suspend fun setTheme(theme: AppTheme) {
        preferenceStorage.setTheme(theme)
    }

    override suspend fun setLanguage(language: AppLanguage) {
        preferenceStorage.setLanguage(language)
    }
}
