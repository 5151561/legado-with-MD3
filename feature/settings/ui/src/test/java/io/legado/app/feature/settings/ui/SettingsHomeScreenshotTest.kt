package io.legado.app.feature.settings.ui

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
 * 画板 C-01 的截图基线。
 *
 * 目的是把「实现与设计稿一致」变成可执行断言，而不是靠人工比对。
 * 基线图提交进仓库，任何改动导致渲染变化都会在 `verifyRoborazziDebug` 时失败。
 *
 * 设备尺寸取设计稿画板的 390×844dp，便于与稿面直接对照。
 *
 * SDK 固定为 36：项目 targetSdk 为 37，而 Robolectric 4.16.1 最高支持 36，
 * 不固定会以「targetSdkVersion=37 > maxSdkVersion=36」失败。截图渲染因此发生在
 * SDK 36 上；Robolectric 支持 37 后可去掉此项。
 *
 * 记录基线：`./gradlew :feature:settings:ui:recordRoborazziDebug`
 * 校验差异：`./gradlew :feature:settings:ui:verifyRoborazziDebug`
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w390dp-h844dp-xhdpi")
class SettingsHomeScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun 设置主页_日光() {
        composeRule.setContent {
            ProvideAppTheme(dark = false) {
                Box(Modifier.fillMaxSize().size(390.dp, 844.dp)) {
                    SettingsHomeScreen(state = SettingsHomePreviewState, onIntent = {})
                }
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun 设置主页_夜墨() {
        composeRule.setContent {
            ProvideAppTheme(dark = true) {
                Box(Modifier.fillMaxSize().size(390.dp, 844.dp)) {
                    SettingsHomeScreen(state = SettingsHomePreviewState, onIntent = {})
                }
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
