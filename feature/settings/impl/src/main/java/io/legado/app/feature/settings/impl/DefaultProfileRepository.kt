package io.legado.app.feature.settings.impl

import io.legado.app.feature.settings.api.AppThemeMode
import io.legado.app.feature.settings.api.ProfileCommands
import io.legado.app.feature.settings.api.ProfileQuery
import io.legado.app.feature.settings.api.ProfileSnapshot
import io.legado.app.feature.settings.api.SettingsCommandResult
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/** 「今天是哪天」的注入点。统计窗口按自然日算，测试要能把今天钉死。 */
internal fun interface TodayProvider {
    fun today(): LocalDate
}

/**
 * 「我的」（画板 P-01）。
 *
 * 本页只读不写，两个例外是主题模式与 Web 服务开关——它们在稿面上就是就地操作的控件。
 */
internal class DefaultProfileRepository(
    private val store: SettingsStore,
    private val appearance: SettingsAppearanceHost,
    private val webService: SettingsWebServiceHost,
    private val version: SettingsAppVersionHost,
    private val today: TodayProvider,
) : ProfileQuery, ProfileCommands {

    private val counts = flowOf(Unit).flatMapLatest {
        // 窗口起点随日期变，但一次订阅内不需要跨零点重算：跨零点时读记录本身会变，
        // Room 会重新发射，届时这条 Flow 也已经重建。
        store.observeProfileCounts(today.today().minusDays(WINDOW_DAYS - 1).toString())
    }

    private val snapshot = combine(
        counts,
        store.observeSourceCounts(),
        combine(
            appearance.observeThemeMode(),
            appearance.observeAccentName(),
            appearance.observeScheduledThemeEnabled(),
            appearance.observeUiFontScalePercent(),
        ) { mode, accent, scheduled, fontScale -> Appearance(mode, accent, scheduled, fontScale) },
        webService.observeStatus(),
        version.observeVersion(),
    ) { profileCounts, sourceCounts, look, web, versionInfo ->
        ProfileSnapshot(
            themeMode = look.mode,
            accentName = look.accentName,
            scheduledThemeEnabled = look.scheduledEnabled,
            uiFontScalePercent = look.fontScalePercent,
            bookmarkCount = profileCounts.bookmarkTotal + profileCounts.markingTotal,
            bookmarkBookCount = profileCounts.bookmarkBookTotal,
            weeklyReadMillis = profileCounts.windowReadMillis,
            aiConversationCount = profileCounts.aiConversationTotal,
            bookSourceCount = sourceCounts.bookSourceTotal,
            rssSourceCount = sourceCounts.rssSourceTotal,
            ruleCount = sourceCounts.ruleTotal(),
            webService = web,
            appVersionName = versionInfo.versionName,
            updateAvailable = versionInfo.updateAvailable,
        )
    }

    override fun observeProfile(): Flow<ProfileSnapshot> = snapshot

    override suspend fun setThemeMode(mode: AppThemeMode): SettingsCommandResult {
        appearance.setThemeMode(mode)
        return SettingsCommandResult.Success
    }

    override suspend fun setWebServiceEnabled(enabled: Boolean): SettingsCommandResult {
        webService.setEnabled(enabled)
        return SettingsCommandResult.Success
    }

    private data class Appearance(
        val mode: AppThemeMode,
        val accentName: String?,
        val scheduledEnabled: Boolean,
        val fontScalePercent: Int,
    )

    private companion object {
        const val WINDOW_DAYS = 7L
    }
}

/**
 * 「规则」那一档的口径：五类规则之和。
 *
 * 与源与规则枢纽（画板 D-00）里「规则」那一档同口径——书源、订阅源、朗读引擎与规则订阅
 * 各自单列，不计入规则数。两处口径必须一致，否则同一台设备上两页会给出不同的数。
 */
internal fun io.legado.app.data.entities.SourceCatalogCounts.ruleTotal(): Int =
    replaceRuleTotal + txtTocRuleTotal + dictRuleTotal + contentHighlightTotal + tagHighlightTotal
