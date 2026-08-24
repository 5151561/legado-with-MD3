package io.legado.app.feature.settings.impl

import io.legado.app.data.entities.ProfileCounts
import io.legado.app.data.entities.SourceCatalogCounts
import io.legado.app.feature.settings.api.AppThemeMode
import io.legado.app.feature.settings.api.WebServiceStatus
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal fun profileCounts(
    bookmarkTotal: Int = 0,
    bookmarkBookTotal: Int = 0,
    markingTotal: Int = 0,
    windowReadMillis: Long = 0L,
    aiConversationTotal: Int = 0,
) = ProfileCounts(
    bookmarkTotal = bookmarkTotal,
    bookmarkBookTotal = bookmarkBookTotal,
    markingTotal = markingTotal,
    windowReadMillis = windowReadMillis,
    aiConversationTotal = aiConversationTotal,
)

internal fun sourceCounts(
    bookSourceTotal: Int = 0,
    rssSourceTotal: Int = 0,
    replaceRuleTotal: Int = 0,
    txtTocRuleTotal: Int = 0,
    dictRuleTotal: Int = 0,
    contentHighlightTotal: Int = 0,
    tagHighlightTotal: Int = 0,
) = SourceCatalogCounts(
    bookSourceTotal = bookSourceTotal,
    bookSourceEnabled = bookSourceTotal,
    bookSourceUnhealthy = 0,
    rssSourceTotal = rssSourceTotal,
    rssSourceEnabled = rssSourceTotal,
    httpTtsTotal = 0,
    replaceRuleTotal = replaceRuleTotal,
    replaceRuleEnabled = replaceRuleTotal,
    txtTocRuleTotal = txtTocRuleTotal,
    txtTocRuleBuiltIn = 0,
    dictRuleTotal = dictRuleTotal,
    dictRuleEnabled = dictRuleTotal,
    contentHighlightTotal = contentHighlightTotal,
    contentHighlightEnabled = contentHighlightTotal,
    tagHighlightTotal = tagHighlightTotal,
    tagHighlightEnabled = tagHighlightTotal,
    ruleSubscriptionTotal = 0,
)

internal class FakeSettingsStore : SettingsStore {
    val countsFlow = MutableStateFlow(profileCounts())
    val sourceFlow = MutableStateFlow(sourceCounts())
    val httpTtsNameFlow = MutableStateFlow<String?>(null)

    /** 记下最后一次统计窗口的起点，供「窗口是近七天」的断言检查。 */
    var lastSinceDate: String? = null
        private set

    /** 记下最后一次查引擎名用的 id，供「id 为空时不查库」的断言检查。 */
    var lastHttpTtsId: String? = null
        private set

    override fun observeProfileCounts(sinceDate: String): Flow<ProfileCounts> {
        lastSinceDate = sinceDate
        return countsFlow
    }

    override fun observeSourceCounts(): Flow<SourceCatalogCounts> = sourceFlow

    override fun observeHttpTtsName(id: String?): Flow<String?> {
        lastHttpTtsId = id
        return httpTtsNameFlow
    }
}

internal class FakeAppearanceHost : SettingsAppearanceHost {
    val themeMode = MutableStateFlow(AppThemeMode.System)
    val accentName = MutableStateFlow<String?>(null)
    val scheduled = MutableStateFlow(false)
    val fontScale = MutableStateFlow(100)
    var written: AppThemeMode? = null
        private set

    override fun observeThemeMode(): Flow<AppThemeMode> = themeMode
    override suspend fun setThemeMode(mode: AppThemeMode) {
        written = mode
        themeMode.value = mode
    }

    override fun observeAccentName(): Flow<String?> = accentName
    override fun observeScheduledThemeEnabled(): Flow<Boolean> = scheduled
    override fun observeUiFontScalePercent(): Flow<Int> = fontScale
}

internal class FakeWebServiceHost : SettingsWebServiceHost {
    val status = MutableStateFlow(WebServiceStatus(running = false))
    var written: Boolean? = null
        private set

    override fun observeStatus(): Flow<WebServiceStatus> = status
    override suspend fun setEnabled(enabled: Boolean) {
        written = enabled
    }
}

internal class FakeVersionHost : SettingsAppVersionHost {
    val version = MutableStateFlow(AppVersionInfo(versionName = "3.26"))
    override fun observeVersion(): Flow<AppVersionInfo> = version
}

internal fun fixedToday(date: String) = TodayProvider { LocalDate.parse(date) }
