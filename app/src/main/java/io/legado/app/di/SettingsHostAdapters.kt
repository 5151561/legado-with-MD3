package io.legado.app.di

import android.content.Context
import io.legado.app.BuildConfig
import io.legado.app.R
import io.legado.app.constant.EventBus
import io.legado.app.domain.gateway.AppShellSettingsGateway
import io.legado.app.domain.gateway.OtherSettingsGateway
import io.legado.app.domain.gateway.ThemeSettingsGateway
import io.legado.app.feature.settings.api.AppThemeMode
import io.legado.app.feature.settings.api.WebServiceStatus
import io.legado.app.feature.settings.impl.AppVersionInfo
import io.legado.app.feature.settings.impl.SettingsAppVersionHost
import io.legado.app.feature.settings.impl.SettingsAppearanceHost
import io.legado.app.feature.settings.impl.SettingsWebServiceHost
import io.legado.app.help.update.AppUpdateStatus
import io.legado.app.service.WebService
import io.legado.app.utils.eventBus.FlowEventBus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * `:feature:settings:impl` 需要的 app shell 接缝。
 *
 * 只做搬运与命名映射，**不含业务判断**——「WebDAV 算不算配置好了」「从未备份与备份失败
 * 怎么分」这类判断在 impl 里。`:core:preferences` 建立后本文件整体删除。
 */

/**
 * 主题模式存在 `AppShellSettings.themeMode`，取值 `"0"` 跟随系统 / `"1"` 日光 / `"2"` 夜墨，
 * 与 `AppConfig.isNightTheme` 同一套编码。
 */
class AppSettingsAppearanceHost(
    private val appShellSettings: AppShellSettingsGateway,
    private val themeSettings: ThemeSettingsGateway,
    private val context: Context,
) : SettingsAppearanceHost {

    override fun observeThemeMode(): Flow<AppThemeMode> =
        appShellSettings.settings.map { it.themeMode.toThemeMode() }

    override suspend fun setThemeMode(mode: AppThemeMode) {
        appShellSettings.update { it.copy(themeMode = mode.toStorageValue()) }
    }

    override fun observeAccentName(): Flow<String?> =
        themeSettings.settings.map { it.appTheme.toAccentName() }

    override fun observeScheduledThemeEnabled(): Flow<Boolean> =
        themeSettings.settings.map { it.eyeProtectionSchedule }

    override fun observeUiFontScalePercent(): Flow<Int> =
        appShellSettings.settings.map { it.fontScale * 10 }

    /**
     * 主题预设名取自 `R.array.themes_item`，下标即 `appTheme` 的数值。
     * 越界（主题包写进了未知值）时返回 null——宁可不写这一句，也不猜一个名字。
     */
    private fun String.toAccentName(): String? {
        val index = toIntOrNull() ?: return null
        val names = context.resources.getStringArray(R.array.themes_item)
        return names.getOrNull(index)
    }
}

private fun String.toThemeMode(): AppThemeMode = when (this) {
    "1" -> AppThemeMode.Light
    "2" -> AppThemeMode.Dark
    else -> AppThemeMode.System
}

private fun AppThemeMode.toStorageValue(): String = when (this) {
    AppThemeMode.Light -> "1"
    AppThemeMode.Dark -> "2"
    AppThemeMode.System -> "0"
}

/**
 * Web 服务的运行态。
 *
 * `WebService.isRun` / `hostAddress` 是进程内的静态量，变化时经 [EventBus.WEB_SERVICE]
 * 广播地址（空串表示已停）。这里把「一个静态量 + 一条事件」合成一条 Flow。
 */
class AppSettingsWebServiceHost(
    private val context: Context,
    private val otherSettings: OtherSettingsGateway,
) : SettingsWebServiceHost {

    override fun observeStatus(): Flow<WebServiceStatus> = combine(
        FlowEventBus.with<String>(EventBus.WEB_SERVICE)
            .onStart { emit(if (WebService.isRun) WebService.hostAddress else "") },
        otherSettings.settings.map { it.webPort },
    ) { address, port ->
        val running = address.isNotEmpty()
        WebServiceStatus(
            running = running,
            address = address.takeIf { running },
            port = port,
        )
    }

    override suspend fun setEnabled(enabled: Boolean) {
        if (enabled) WebService.start(context) else WebService.stop(context)
    }
}

/**
 * 版本与更新。
 *
 * 更新检查仍然只在启动时做一次，结果记在 [AppUpdateStatus]——设置页与「我的」都不为
 * 显示一个角标而发网络请求。
 */
class AppSettingsVersionHost : SettingsAppVersionHost {
    override fun observeVersion(): Flow<AppVersionInfo> = AppUpdateStatus.info.map { info ->
        AppVersionInfo(
            versionName = BuildConfig.VERSION_NAME,
            updateAvailable = info != null,
            latestVersionName = info?.tagName,
        )
    }
}
