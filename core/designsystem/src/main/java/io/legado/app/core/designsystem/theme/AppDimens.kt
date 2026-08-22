package io.legado.app.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 间距与控件尺寸。
 *
 * 间距是 2dp 网格——画板墙 1092 处 `gap` 与 1051 处 `padding` 的分布集中在
 * 4/6/8/10/12/14/16/20/24，不是 4/8 的整倍数体系，因此保留 2dp 粒度。
 *
 * 控件尺寸同样取自实测：图标按钮 48、顶栏 56、chip 32、进度条 4、分隔线 1。
 *
 * **不提供状态栏/导航栏高度常量**：画板里的 44dp 状态栏只是稿面模拟，真机高度随设备与
 * 显示模式变化，必须来自 `WindowInsets`。写成固定值在刘海屏、小屏和手势导航下都会错位。
 */
@Immutable
data class AppDimens(
    // ---- 间距（2dp 网格）----
    val spaceXxs: Dp = 2.dp,
    val spaceXs: Dp = 4.dp,
    val spaceS: Dp = 6.dp,
    val spaceM: Dp = 8.dp,
    val spaceL: Dp = 10.dp,
    val spaceXl: Dp = 12.dp,
    val spaceXxl: Dp = 14.dp,
    /** 16dp——页面水平内边距与卡片内边距的默认值，最高频。 */
    val spaceContent: Dp = 16.dp,
    val spaceSection: Dp = 20.dp,
    val spaceGroup: Dp = 24.dp,

    // ---- 控件 ----
    /**
     * 最小可点区域。设计规则「触点与字号下限」：所有可点区域 ≥48 dp。
     * 视觉上更矮的按钮（如 40dp 高的胶囊）必须由外层补足到这个尺寸。
     */
    val minTouchTarget: Dp = 48.dp,
    /** 图标按钮的方形触点。 */
    val iconButton: Dp = 48.dp,
    /** 顶栏高度。 */
    val topBarHeight: Dp = 56.dp,
    /** 胶囊按钮视觉高度。 */
    val buttonHeight: Dp = 40.dp,
    /** 强调按钮视觉高度。 */
    val buttonHeightProminent: Dp = 48.dp,
    /** chip / 小头像。 */
    val chipHeight: Dp = 32.dp,
    /** 设置行的前导图标圆底。 */
    val leadingIcon: Dp = 40.dp,
    /** 列表行最小高度。 */
    val rowMinHeight: Dp = 54.dp,
    /** 线性进度条厚度。 */
    val progressThickness: Dp = 4.dp,
    val divider: Dp = 1.dp,

    // ---- 图标字号 ----
    val iconSmall: Dp = 20.dp,
    val iconMedium: Dp = 22.dp,
    val iconLarge: Dp = 24.dp,
)

val LocalAppDimens = staticCompositionLocalOf { AppDimens() }
