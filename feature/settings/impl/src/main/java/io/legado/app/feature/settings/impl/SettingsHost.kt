package io.legado.app.feature.settings.impl

import io.legado.app.feature.settings.api.AppThemeMode
import io.legado.app.feature.settings.api.WebServiceStatus
import kotlinx.coroutines.flow.Flow

/**
 * 设置实现的 app shell 接缝。
 *
 * 「我的」与设置主页的摘要有一半落在偏好上，`:core:preferences` 建立之前偏好的唯一 owner
 * 还在 app shell。接缝只出**原始值**，不出摘要文案，也不出已经折叠过的判断——
 * 折叠归 impl，拼句归 UI。`:core:preferences` 建成后本文件整体删除。
 */

/** 外观偏好。主题模式是本页唯一能就地改的偏好，因此读写都在这里。 */
interface SettingsAppearanceHost {
    fun observeThemeMode(): Flow<AppThemeMode>
    suspend fun setThemeMode(mode: AppThemeMode)

    /** 强调色种子的名字；跟随系统动态取色时为 null。 */
    fun observeAccentName(): Flow<String?>

    /** 定时切换日夜的开关。 */
    fun observeScheduledThemeEnabled(): Flow<Boolean>

    /** 界面字号，100 为标准。 */
    fun observeUiFontScalePercent(): Flow<Int>
}

/**
 * Web 服务。
 *
 * 启停要 `Context` 与前台服务，地址由服务自己广播，因此整块留在 app shell；
 * impl 只消费状态、只发命令。
 */
interface SettingsWebServiceHost {
    fun observeStatus(): Flow<WebServiceStatus>
    suspend fun setEnabled(enabled: Boolean)
}

/**
 * 版本与更新。
 *
 * @param latestVersionName 仅 [updateAvailable] 为真时有值；没检查过或检查失败时
 *   [updateAvailable] 为 false——「不知道」与「已是最新」在这里合并，
 *   因为界面上两者都不该出现「新版本」角标。
 */
data class AppVersionInfo(
    val versionName: String,
    val updateAvailable: Boolean = false,
    val latestVersionName: String? = null,
)

fun interface SettingsAppVersionHost {
    fun observeVersion(): Flow<AppVersionInfo>
}
