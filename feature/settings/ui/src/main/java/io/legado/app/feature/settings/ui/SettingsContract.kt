package io.legado.app.feature.settings.ui

import androidx.compose.runtime.Stable

@Stable
data class SettingsUiState(
    val loading: Boolean = true,
    val themeMode: String = "",
    val fontScale: Int = 10,
    val bitmapCacheSizeMb: Int = 0,
    val downloadThreadCount: Int = 0,
    val backupConfigured: Boolean = false,
    val syncBookProgress: Boolean = false,
    val loadFailed: Boolean = false,
)

sealed interface SettingsIntent {
    data object Retry : SettingsIntent
    data object OpenTheme : SettingsIntent
    data object OpenInterface : SettingsIntent
    data object OpenDownloadCache : SettingsIntent
    data object OpenBackup : SettingsIntent
    data object OpenRead : SettingsIntent
    data object OpenCover : SettingsIntent
    data object OpenAi : SettingsIntent
    data object OpenTranslation : SettingsIntent
    data object OpenLab : SettingsIntent
}

sealed interface SettingsEffect {
    data object Theme : SettingsEffect
    data object Interface : SettingsEffect
    data object DownloadCache : SettingsEffect
    data object Backup : SettingsEffect
    data object Read : SettingsEffect
    data object Cover : SettingsEffect
    data object Ai : SettingsEffect
    data object Translation : SettingsEffect
    data object Lab : SettingsEffect
}
