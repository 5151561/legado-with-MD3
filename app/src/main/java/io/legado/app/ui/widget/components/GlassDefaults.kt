package io.legado.app.ui.widget.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.legado.app.domain.model.settings.customColors
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.theme.LocalAppUiConfiguration

object GlassDefaults {

    @Composable
    fun glassColor(noBlurColor: Color, blurAlpha: Float): Color {
        return noBlurColor
    }

    @Composable
    fun secondaryColorOr(fallback: @Composable () -> Color): Color {
        val themeSettings = LocalAppUiConfiguration.current.theme
        val secondaryColor = themeSettings.customColors(LegadoTheme.isDark).secondary
        return if (themeSettings.enableDeepPersonalization && secondaryColor != 0) {
            Color(secondaryColor)
        } else {
            fallback()
        }
    }

    val DefaultBlurAlpha = 1f
    val ThickBlurAlpha = 1f
    val TransparentAlpha = 1f
}
