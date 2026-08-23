package io.legado.app.core.designsystem.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.legado.app.core.designsystem.theme.AppTheme

/**
 * 开关（画板 M-01a 首页区块、P-01 Web 服务）。
 *
 * 轨道 52×32dp 全圆角。开态填充 primary、旋钮 24dp 内嵌一枚勾；
 * 关态为 1dp outline 描边 + 16dp outline 圆点；
 * 停用态（[enabled] 为 false）轨道退到 surfaceContainerHighest 并整体降到 50% 不透明度，
 * 用于「恒开、不可改」这类锁定项——它仍要显示成开着的样子，只是点不动。
 *
 * 轨道视觉高 32dp，触点由 48dp 方形补足（设计规则「触点与字号下限」）。
 *
 * 取色说明：稿面上开态与停用态的旋钮都是 `#fff`。夜墨下 primary 本身是浅色，
 * 白旋钮会糊在轨道上，因此开态旋钮取 `onPrimary`、停用态取 `surfaceContainerLowest`，
 * 两种主题下都保持对比。
 *
 * @param thumbIcon 开态旋钮里的图标，参数为该图标应有的颜色。设计系统不带图标资源，
 *   缺省不画——需要勾或锁的调用方自行传入（画板上这两处都有图标）。
 */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    thumbIcon: @Composable ((tint: Color) -> Unit)? = null,
) {
    val c = AppTheme.colorScheme
    val dimens = AppTheme.dimens
    val shape = AppTheme.shapes.full
    Box(
        modifier = modifier
            .size(dimens.minTouchTarget)
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(dimens.chipHeight)
                .alpha(if (enabled) 1f else 0.5f)
                .clip(shape)
                .background(if (checked && enabled) c.primary else c.surfaceContainerHighest)
                .then(if (checked) Modifier else Modifier.border(dimens.divider, c.outline, shape))
                .padding(if (checked) dimens.spaceXs else dimens.spaceS),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            if (checked) {
                Box(
                    modifier = Modifier
                        .size(dimens.iconLarge)
                        .clip(shape)
                        .background(if (enabled) c.onPrimary else c.surfaceContainerLowest),
                    contentAlignment = Alignment.Center,
                ) {
                    thumbIcon?.invoke(if (enabled) c.primary else c.outline)
                }
            } else {
                Box(
                    Modifier
                        .size(16.dp)
                        .clip(shape)
                        .background(c.outline),
                )
            }
        }
    }
}
