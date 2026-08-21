package io.legado.app.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Design System 的主题消费接缝。
 *
 * app shell 负责解析动态色、用户偏好和字体，再把只读结果注入这里。Design System 不读取
 * Context、设置 Gateway 或业务模型。
 */
@Composable
fun ProvideLegadoTheme(
    mode: LegadoThemeMode,
    colorScheme: LegadoColorScheme,
    typography: LegadoTypography,
    spacing: LegadoSpacing = LegadoSpacing(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalLegadoThemeMode provides mode,
        LocalLegadoColorScheme provides colorScheme,
        LocalLegadoTypography provides typography,
        LocalLegadoSpacing provides spacing,
        content = content,
    )
}
