package io.legado.app.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * 形状体系。档位来自三面画板墙 869 处 `border-radius` 的实测分布，
 * 与 MD3 Expressive 的形状阶梯一致：
 *
 * ```
 * 16px×170  20px×91  12px×89  2px×90  28px×71  10px×58  8px×44  14px×42  4px×29
 * ```
 */
@Immutable
data class AppShapes(
    /** 2dp——进度条、细指示条。 */
    val indicator: RoundedCornerShape = RoundedCornerShape(2.dp),
    /** 4dp。 */
    val extraSmall: RoundedCornerShape = RoundedCornerShape(4.dp),
    /** 8dp。 */
    val small: RoundedCornerShape = RoundedCornerShape(8.dp),
    /** 12dp——强调按钮、内嵌块。 */
    val medium: RoundedCornerShape = RoundedCornerShape(12.dp),
    /** 16dp——最高频，内容卡片默认。 */
    val large: RoundedCornerShape = RoundedCornerShape(16.dp),
    /** 18dp——设置分组卡。 */
    val largeIncreased: RoundedCornerShape = RoundedCornerShape(18.dp),
    /** 20dp——面板、错误卡。 */
    val extraLarge: RoundedCornerShape = RoundedCornerShape(20.dp),
    /** 24dp——说明卡、大面板。 */
    val extraLargeIncreased: RoundedCornerShape = RoundedCornerShape(24.dp),
    /** 28dp——设备圆角、底部 sheet 顶角。 */
    val extraExtraLarge: RoundedCornerShape = RoundedCornerShape(28.dp),
    /** 底部 sheet：仅顶部两角 28dp。 */
    val bottomSheet: RoundedCornerShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    /** 全圆角——按钮、chip。 */
    val full: RoundedCornerShape = RoundedCornerShape(percent = 50),
)

val LocalAppShapes = staticCompositionLocalOf { AppShapes() }
