package io.legado.app.feature.settings.api

import kotlinx.coroutines.flow.Flow

data class SettingsOverview(
    val themeMode: String,
    val fontScale: Int,
    val bitmapCacheSizeMb: Int,
    val downloadThreadCount: Int,
    val backupConfigured: Boolean,
    val syncBookProgress: Boolean,
)

fun interface SettingsOverviewQuery {
    fun observeOverview(): Flow<SettingsOverview>
}
