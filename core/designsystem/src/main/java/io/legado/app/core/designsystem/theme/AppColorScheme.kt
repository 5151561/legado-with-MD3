package io.legado.app.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 重设计的 App 配色。
 *
 * 取值直接来自设计稿 `阅读 Legado 重设计` 三面画板墙的 `:root` 令牌，不经 MaterialKolor 生成，
 * 以保证默认主题与设计稿逐像素一致。[SeedGraphiteTeal] 记录生成这套方案的种子色，
 * 供"强调色"设置项在用户自选强调色时重新生成同构方案（见画板 N-04）。
 *
 * 主题模式为 日光 / 夜墨 / 跟随系统 三选一，另有定时切换；与阅读页纸色无关——
 * 正文纸色由 [ReadingPalette] 独立承载，在阅读样式抽屉里单独选择。
 */
@Immutable
data class AppColorScheme(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val surface: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val surfaceVariant: Color,
    val surfaceContainerLowest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val outline: Color,
    val outlineVariant: Color,
)

/** 石墨青——设计稿默认种子色。 */
val SeedGraphiteTeal = Color(0xFF35606E)

/** 日光（浅色）。 */
val AppLightColorScheme = AppColorScheme(
    primary = Color(0xFF35606E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC3E7F4),
    onPrimaryContainer = Color(0xFF001F28),
    secondary = Color(0xFF4C6169),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFE6EE),
    onSecondaryContainer = Color(0xFF071E24),
    tertiary = Color(0xFF6E5B3E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF6E4C9),
    onTertiaryContainer = Color(0xFF251A05),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    surface = Color(0xFFF7FAFB),
    onSurface = Color(0xFF181C1E),
    onSurfaceVariant = Color(0xFF3F484B),
    surfaceVariant = Color(0xFFDCE5E8),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F6F8),
    surfaceContainer = Color(0xFFEAF1F3),
    surfaceContainerHigh = Color(0xFFE3EBEE),
    // 设计稿的 sf 阶梯止于 sf4；highest 未单独给出，沿用 sf4。
    surfaceContainerHighest = Color(0xFFDCE5E8),
    outline = Color(0xFF6F787C),
    outlineVariant = Color(0xFFC0C9CC),
)

/**
 * 夜墨（深色）。
 *
 * 设计稿的深色令牌覆盖 primary、surface 阶梯、outline 与 error；
 * secondary / tertiary / onError 未给出深色值，下方标注处按 MD3 同色族推导。
 * 2026-08-22 已确认采用推导值，不再等设计补稿；后续若设计给出正式值，替换这几行即可。
 */
val AppDarkColorScheme = AppColorScheme(
    primary = Color(0xFF9CD0E1),
    onPrimary = Color(0xFF003544),
    primaryContainer = Color(0xFF1E4B58),
    onPrimaryContainer = Color(0xFFC3E7F4),
    secondary = Color(0xFFB3CAD3),          // 推导值
    onSecondary = Color(0xFF1D343A),        // 推导值
    secondaryContainer = Color(0xFF344A51), // 推导值
    onSecondaryContainer = Color(0xFFCFE6EE),
    tertiary = Color(0xFFD9C3A0),           // 推导值
    onTertiary = Color(0xFF3C2E15),         // 推导值
    tertiaryContainer = Color(0xFF554429),  // 推导值
    onTertiaryContainer = Color(0xFFF6E4C9),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),            // 推导值
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    surface = Color(0xFF0F1315),
    onSurface = Color(0xFFDDE3E5),
    onSurfaceVariant = Color(0xFFA9B4B7),
    surfaceVariant = Color(0xFF323C40),
    surfaceContainerLowest = Color(0xFF0F1315),
    surfaceContainerLow = Color(0xFF191F21),
    surfaceContainer = Color(0xFF222A2D),
    surfaceContainerHigh = Color(0xFF2A3336),
    surfaceContainerHighest = Color(0xFF323C40),
    outline = Color(0xFF899497),
    outlineVariant = Color(0xFF3F484B),
)

val LocalAppColorScheme = staticCompositionLocalOf { AppLightColorScheme }
