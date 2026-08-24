package io.legado.app.ui.shell

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.legado.app.core.designsystem.theme.ProvideAppTheme
import io.legado.app.core.designsystem.theme.ReadingPaperDefault
import io.legado.app.core.designsystem.theme.ReadingPaperNight
import io.legado.app.domain.gateway.AppUiConfigurationGateway
import org.koin.android.ext.android.inject

/**
 * 应用入口。
 *
 * 只做三件事：边到边、把系统深浅色同步给配置网关、把主题装上再交给 [ShellScreen]。
 * 导航、一级 tab、回退栈都不在这里——那些归 `:core:navigation` 与 [ShellScreen]。
 *
 * **不继承 `BaseComposeActivity`**：那个基类装的是旧主题 `io.legado.app.ui.theme.AppTheme`，
 * 而新外壳只用 `:core:designsystem` 的一套。旧基类随最后一个旧页面一起删。
 */
open class ShellActivity : AppCompatActivity() {

    private val appUiConfigurationGateway by inject<AppUiConfigurationGateway>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        syncSystemDarkTheme()

        val initial = appUiConfigurationGateway.currentConfiguration
        setContent {
            val configuration by appUiConfigurationGateway.configuration
                .collectAsStateWithLifecycle(initial)
            val dark = configuration.isDarkTheme
            ProvideAppTheme(
                dark = dark,
                // 正文纸色独立于 App 主题（画板 N-04）。阅读样式抽屉重做前，
                // 外壳按深浅色给一个默认纸色，不让阅读面退回 surface 色阶。
                readingPalette = if (dark) ReadingPaperNight else ReadingPaperDefault,
            ) {
                ShellScreen()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        syncSystemDarkTheme()
    }

    private fun syncSystemDarkTheme() {
        val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        appUiConfigurationGateway.synchronizeSystemDarkTheme(
            nightMode == Configuration.UI_MODE_NIGHT_YES
        )
    }
}
