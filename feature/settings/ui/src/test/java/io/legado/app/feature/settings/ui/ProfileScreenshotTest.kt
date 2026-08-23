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
 * 画板 P-01 v2 的截图基线。约束与 [SettingsHomeScreenshotTest] 相同。
 *
 * 记录基线：`./gradlew :feature:settings:ui:recordRoborazziDebug`
 * 校验差异：`./gradlew :feature:settings:ui:verifyRoborazziDebug`
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w390dp-h844dp-xhdpi")
class ProfileScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun 我的_日光() = capture(dark = false)

    @Test
    fun 我的_夜墨() = capture(dark = true)

    private fun capture(dark: Boolean) {
        composeRule.setContent {
            ProvideAppTheme(dark = dark) {
                Box(Modifier.fillMaxSize().size(390.dp, 844.dp)) {
                    ProfileScreen(state = ProfilePreviewState, onIntent = {})
                }
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
