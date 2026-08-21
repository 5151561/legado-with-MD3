package io.legado.app.feature.settings.compat

import io.legado.app.domain.gateway.AppShellSettingsGateway
import io.legado.app.domain.gateway.BackupSettingsGateway
import io.legado.app.domain.gateway.DownloadCacheSettingsGateway
import io.legado.app.feature.settings.api.SettingsOverview
import io.legado.app.feature.settings.api.SettingsOverviewQuery
import kotlinx.coroutines.flow.combine

/** Phase 3 compatibility seam. Contains mapping only; settings writes keep their existing owners. */
class LegacySettingsAdapter(
    appShellSettings: AppShellSettingsGateway,
    downloadCacheSettings: DownloadCacheSettingsGateway,
    backupSettings: BackupSettingsGateway,
) : SettingsOverviewQuery {
    private val overview = combine(
        appShellSettings.settings,
        downloadCacheSettings.settings,
        backupSettings.settings,
    ) { appShell, download, backup ->
        SettingsOverview(
            themeMode = appShell.themeMode,
            fontScale = appShell.fontScale,
            bitmapCacheSizeMb = download.bitmapCacheSize,
            downloadThreadCount = download.threadCount,
            backupConfigured = backup.webDavUrl.isNotBlank() && backup.webDavAccount.isNotBlank(),
            syncBookProgress = backup.syncBookProgress,
        )
    }

    override fun observeOverview() = overview
}
