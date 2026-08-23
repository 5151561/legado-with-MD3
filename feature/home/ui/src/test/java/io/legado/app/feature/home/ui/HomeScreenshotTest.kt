package io.legado.app.feature.home.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.legado.app.core.designsystem.theme.ProvideAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 画板 M-01 v2 与 M-01a v2 的截图基线。
 *
 * 设备尺寸取画板的 390×844dp，便于与稿面直接对照。SDK 固定为 36——
 * 项目 targetSdk 为 37，Robolectric 4.16.1 最高支持 36。
 *
 * 记录基线：`./gradlew :feature:home:ui:recordRoborazziDebug`
 * 校验差异：`./gradlew :feature:home:ui:verifyRoborazziDebug`
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w390dp-h844dp-xhdpi")
class HomeScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun 首页_日光() {
        capture(dark = false) {
            HomeDashboardScreen(
                state = HomeDashboardPreviewState,
                onIntent = {},
                bottomBar = { MainNavigationPreviewBar(selectedId = "home") },
            )
        }
    }

    @Test
    fun 首页_夜墨() {
        capture(dark = true) {
            HomeDashboardScreen(
                state = HomeDashboardPreviewState,
                onIntent = {},
                bottomBar = { MainNavigationPreviewBar(selectedId = "home") },
            )
        }
    }

    @Test
    fun 首页区块设置_日光() {
        capture(dark = false) {
            HomeSectionsScreen(state = HomeSectionsPreviewState, onIntent = {})
        }
    }

    @Test
    fun 首页区块设置_夜墨() {
        capture(dark = true) {
            HomeSectionsScreen(state = HomeSectionsPreviewState, onIntent = {})
        }
    }

    private fun capture(dark: Boolean, content: @androidx.compose.runtime.Composable () -> Unit) {
        composeRule.setContent {
            ProvideAppTheme(dark = dark) {
                Box(Modifier.fillMaxSize().size(390.dp, 844.dp)) { content() }
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
