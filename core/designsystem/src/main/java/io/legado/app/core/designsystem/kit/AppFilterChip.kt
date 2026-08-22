package io.legado.app.core.designsystem.kit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.legado.app.core.designsystem.component.AppText
import io.legado.app.core.designsystem.theme.AppTheme

/**
 * 筛选 chip（画板 B-01 / B-03b 的分组与筛选条）。
 *
 * 视觉高 32dp、左右内边距 12dp、全圆角、字重 500 / 13sp。
 * 选中态填充 secondaryContainer，未选中态为 1dp outline 描边。
 *
 * 与按钮同理：视觉 32dp，触点由外层补足到 48dp（设计规则「触点与字号下限」）。
 */
@Composable
fun AppFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = AppTheme.colorScheme
    val dimens = AppTheme.dimens
    val shape = AppTheme.shapes.full
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = dimens.minTouchTarget)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .height(dimens.chipHeight)
                .clip(shape)
                .then(
                    if (selected) {
                        Modifier.background(c.secondaryContainer)
                    } else {
                        Modifier.border(BorderStroke(dimens.divider, c.outline), shape)
                    },
                )
                .padding(horizontal = dimens.spaceXl),
            contentAlignment = Alignment.Center,
        ) {
            AppText(
                text = label,
                style = AppTheme.typography.label,
                color = if (selected) c.onSecondaryContainer else c.onSurfaceVariant,
            )
        }
    }
}
