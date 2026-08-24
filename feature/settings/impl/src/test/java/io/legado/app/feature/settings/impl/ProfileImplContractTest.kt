package io.legado.app.feature.settings.impl

import io.legado.app.feature.settings.api.AppThemeMode
import io.legado.app.feature.settings.api.SettingsCommandResult
import io.legado.app.feature.settings.api.WebServiceStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 「我的」（画板 P-01）的 api 契约。 */
class ProfileImplContractTest {

    private val store = FakeSettingsStore()
    private val appearance = FakeAppearanceHost()
    private val webService = FakeWebServiceHost()
    private val version = FakeVersionHost()

    private fun repository(today: String = "2026-08-24") = DefaultProfileRepository(
        store = store,
        appearance = appearance,
        webService = webService,
        version = version,
        today = fixedToday(today),
    )

    @Test
    fun `统计窗口是含今天在内的近七天`() = runTest {
        repository(today = "2026-08-24").observeProfile().first()

        assertEquals("2026-08-18", store.lastSinceDate)
    }

    @Test
    fun `书签与笔记合并成一个数，书本数不合并`() = runTest {
        store.countsFlow.value = profileCounts(
            bookmarkTotal = 96,
            markingTotal = 32,
            bookmarkBookTotal = 9,
        )

        val snapshot = repository().observeProfile().first()

        assertEquals(128, snapshot.bookmarkCount)
        assertEquals(9, snapshot.bookmarkBookCount)
    }

    @Test
    fun `规则数是五类规则之和，书源与订阅源不计入`() = runTest {
        store.sourceFlow.value = sourceCounts(
            bookSourceTotal = 312,
            rssSourceTotal = 8,
            replaceRuleTotal = 20,
            txtTocRuleTotal = 11,
            dictRuleTotal = 6,
            contentHighlightTotal = 5,
            tagHighlightTotal = 4,
        )

        val snapshot = repository().observeProfile().first()

        assertEquals(312, snapshot.bookSourceCount)
        assertEquals(8, snapshot.rssSourceCount)
        assertEquals(46, snapshot.ruleCount)
    }

    @Test
    fun `Web 服务没跑起来时不给地址`() = runTest {
        webService.status.value = WebServiceStatus(running = false)

        val snapshot = repository().observeProfile().first()

        assertNull(snapshot.webService.address)
    }

    @Test
    fun `外观三项来自偏好，跟随动态取色时强调色名为空`() = runTest {
        appearance.themeMode.value = AppThemeMode.Dark
        appearance.accentName.value = null
        appearance.scheduled.value = true
        appearance.fontScale.value = 115

        val snapshot = repository().observeProfile().first()

        assertEquals(AppThemeMode.Dark, snapshot.themeMode)
        assertNull(snapshot.accentName)
        assertTrue(snapshot.scheduledThemeEnabled)
        assertEquals(115, snapshot.uiFontScalePercent)
    }

    @Test
    fun `主题模式写回接缝`() = runTest {
        val result = repository().setThemeMode(AppThemeMode.Light)

        assertEquals(SettingsCommandResult.Success, result)
        assertEquals(AppThemeMode.Light, appearance.written)
    }

    @Test
    fun `Web 服务开关写回接缝`() = runTest {
        val result = repository().setWebServiceEnabled(true)

        assertEquals(SettingsCommandResult.Success, result)
        assertEquals(true, webService.written)
    }
}
