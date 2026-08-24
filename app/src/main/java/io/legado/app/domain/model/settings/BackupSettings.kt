package io.legado.app.domain.model.settings

data class BackupSettings(
    val webDavUrl: String = "",
    val webDavAccount: String = "",
    val webDavPassword: String = "",
    val webDavDir: String = "legado",
    val webDavDeviceName: String = "",
    val syncBookProgress: Boolean = true,
    val syncBookProgressPlus: Boolean = false,
    val autoCheckNewBackup: Boolean = true,
    val onlyLatestBackup: Boolean = true,
    val backupSyncMode: String = "both",
    val backupPath: String? = null,
    /**
     * 上次成功备份的时刻；从未备份过为 0。
     *
     * **只读**：写入 owner 是 `Backup`/`Restore`，因此不在 `toPrefMap()` 里，
     * 经 gateway 的 `update {}` 改它不会落盘。
     */
    val lastBackupAtMillis: Long = 0L,
)
