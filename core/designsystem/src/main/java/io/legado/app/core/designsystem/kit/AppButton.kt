package io.legado.app.core.designsystem.kit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.legado.app.core.designsystem.component.AppText
import io.legado.app.core.designsystem.theme.AppTheme

/**
 * 胶囊按钮。
 *
 * 规格来自画板 R-01e 等处：视觉高 40dp、全圆角、左右内边距 16–18dp、
 * 图标与文字间距 6dp、字重 500 / 14sp。
 *
 * 视觉高度只有 40dp，而设计规则要求「所有可点区域 ≥48 dp」，
 * 因此点击区由外层补足到 `dimens.minTouchTarget`——视觉不变，触点合规。
 */
@Composable
fun AppFilledButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val c = AppTheme.colorScheme
    AppButtonSurface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        container = c.primary,
        contentColor = c.onPrimary,
        border = null,
        horizontalPadding = 18.dp,
        text = text,
        leadingIcon = leadingIcon,
    )
}

/** 描边按钮：同尺寸，仅 1dp 描边，无填充。 */
@Composable
fun AppOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = AppTheme.colorScheme.onSurface,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    AppButtonSurface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        container = Color.Transparent,
        contentColor = contentColor,
        border = BorderStroke(AppTheme.dimens.divider, contentColor),
        horizontalPadding = 16.dp,
        text = text,
        leadingIcon = leadingIcon,
    )
}

/**
 * 强调按钮：视觉高 48dp、圆角 12dp、反色填充。
 * 用于页面主操作（画板 R-01e 底部整幅按钮一类）。
 */
@Composable
fun AppProminentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val c = AppTheme.colorScheme
    AppButtonSurface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        container = c.onSurface,
        contentColor = c.surface,
        border = null,
        horizontalPadding = 20.dp,
        text = text,
        leadingIcon = leadingIcon,
        visualHeight = AppTheme.dimens.buttonHeightProminent,
        shape = AppTheme.shapes.medium,
    )
}

@Composable
private fun AppButtonSurface(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    container: Color,
    contentColor: Color,
    border: BorderStroke?,
    horizontalPadding: Dp,
    text: String,
    leadingIcon: (@Composable () -> Unit)?,
    visualHeight: Dp = AppTheme.dimens.buttonHeight,
    shape: Shape = AppTheme.shapes.full,
) {
    val dimens = AppTheme.dimens
    val alpha = if (enabled) 1f else 0.38f
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = dimens.minTouchTarget)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .height(visualHeight)
                .background(container.copy(alpha = container.alpha * alpha), shape)
                .then(if (border != null) Modifier.border(border, shape) else Modifier)
                .padding(horizontal = horizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceS),
        ) {
            leadingIcon?.invoke()
            AppText(
                text = text,
                style = AppTheme.typography.label.copy(fontSize = 14.sp),
                color = contentColor.copy(alpha = alpha),
            )
        }
    }
}
