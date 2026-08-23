package io.legado.app.feature.catalog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.legado.app.core.designsystem.theme.AppTheme
import io.legado.app.core.designsystem.theme.ProvideAppTheme
import io.legado.app.core.designsystem.theme.ReadingPaperDefault
import io.legado.app.core.designsystem.theme.ReadingPaperNight
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 画板 D-00 / S-04 v2 / S-04a v2 / S-06a v2 / S-06b v2 的截图基线。
 *
 * 设备尺寸取画板的 390×844dp。SDK 固定为 36——项目 targetSdk 为 37，
 * Robolectric 4.16.1 最高支持 36。
 *
 * 记录基线：`./gradlew :feature:catalog:ui:recordRoborazziDebug`
 * 校验差异：`./gradlew :feature:catalog:ui:verifyRoborazziDebug`
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w390dp-h844dp-xhdpi")
class CatalogRedesignScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun 源与规则枢纽_日光() = capture(dark = false) {
        SourceHubScreen(state = SourceHubPreviewState, onIntent = {})
    }

    @Test
    fun 源与规则枢纽_夜墨() = capture(dark = true) {
        SourceHubScreen(state = SourceHubPreviewState, onIntent = {})
    }

    @Test
    fun 书籍详情_日光() = capture(dark = false) {
        BookDetailScreen(state = BookDetailPreviewState, onIntent = {})
    }

    @Test
    fun 书籍详情_夜墨() = capture(dark = true) {
        BookDetailScreen(state = BookDetailPreviewState, onIntent = {})
    }

    @Test
    fun 书籍详情_更多菜单() = capture(dark = false) {
        BookDetailScreen(
            state = BookDetailPreviewState.copy(menu = BookDetailPreviewMenu),
            onIntent = {},
        )
    }

    @Test
    fun 书籍详情_移出书架确认() = capture(dark = false) {
        BookDetailScreen(
            state = BookDetailPreviewState.copy(activeDialog = BookDetailPreviewRemoveDialog),
            onIntent = {},
        )
    }

    @Test
    fun 阅读器内目录_纸() = capture(dark = false) { ReaderTocHost() }

    @Test
    fun 阅读器内目录_夜纸() =
        capture(dark = true, readingNight = true) { ReaderTocHost() }

    @Test
    fun 目录管理页_日光() = capture(dark = false) {
        TocManageScreen(state = TocManagePreviewState, onIntent = {})
    }

    @Test
    fun 目录管理页_夜墨() = capture(dark = true) {
        TocManageScreen(state = TocManagePreviewState, onIntent = {})
    }

    /** 目录面板贴底，上方留出正文——遮罩与定位归宿主，这里补上以便与稿面对照。 */
    @Composable
    private fun ReaderTocHost() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.reading.paper),
            verticalArrangement = Arrangement.Bottom,
        ) {
            ReaderTocSheet(state = ReaderTocPreviewState, onIntent = {})
        }
    }

    private fun capture(
        dark: Boolean,
        readingNight: Boolean = false,
        content: @Composable () -> Unit,
    ) {
        composeRule.setContent {
            ProvideAppTheme(
                dark = dark,
                readingPalette = if (readingNight) ReadingPaperNight else ReadingPaperDefault,
            ) {
                Box(Modifier.fillMaxSize().size(390.dp, 844.dp)) { content() }
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
