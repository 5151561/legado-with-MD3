package io.legado.app.ui.widget.components.navigation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import io.legado.app.domain.model.settings.customColors
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.theme.LocalAppUiConfiguration
import io.legado.app.ui.widget.components.text.AnimatedText

@Composable
fun AppNavigationBar(
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    alwaysShowLabel: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val configuration = LocalAppUiConfiguration.current
    val themeSettings = configuration.theme
    val customSecondaryColor = themeSettings.customColors(LegadoTheme.isDark).secondary
    val hasCustomSecondary = themeSettings.appTheme == "12" &&
        themeSettings.enableDeepPersonalization && customSecondaryColor != 0
    val baseColor =
        if (hasCustomSecondary) {
            Color(customSecondaryColor)
        } else {
            BottomAppBarDefaults.containerColor
        }

    ShortNavigationBar(
        modifier = modifier,
        containerColor = baseColor,
        content = { ShortNavigationBarRowScope.content() }
    )
}

@Composable
fun RowScope.AppNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    labelString: String,
    iconVector: ImageVector,
    m3Icon: @Composable () -> Unit,
    m3IndicatorColor: Color,
    m3ShowLabel: Boolean,
    m3AlwaysShowLabel: Boolean = true,
    useCustomIcon: Boolean = false,
) {
    ShortNavigationBarItem(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        icon = m3Icon,
        colors = ShortNavigationBarItemDefaults.colors(selectedIndicatorColor = m3IndicatorColor),
        label = if (m3ShowLabel && (m3AlwaysShowLabel || selected)) {
            { AnimatedText(labelString) }
        } else null
    )
}
