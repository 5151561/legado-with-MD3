package io.legado.app.core.designsystem.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import io.legado.app.core.designsystem.theme.AppTheme
import kotlinx.collections.immutable.ImmutableList

/** 分段控件的一项。[id] 用作选中判定与回调标识。 */
@Immutable
data class SegmentedOption(
    val id: String,
    val label: String,
)

/**
 * 分段控件（画板 B-03「书架显示 · 布局」的 网格 / 在读优先 / 紧凑）。
 *
 * 容器高 40dp、1dp outline 描边、圆角 20dp、裁切溢出；
 * 段内文字 500 / 13sp，图标 17dp，图标与文字间距 6dp；段间以 1dp outline 竖线分隔。
 * 选中段填充 secondaryContainer，未选中段透明。
 *
 * 整体高 40dp 低于 48dp 触点下限，但各段是等分平铺的相邻选择项、不存在误触间隙，
 * 与孤立按钮情形不同，故按设计稿保持 40dp。若单独使用某一段，请自行补足触点。
 */
@Composable
fun AppSegmentedControl(
    options: ImmutableList<SegmentedOption>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable (SegmentedOption) -> Unit)? = null,
) {
    val c = AppTheme.colorScheme
    val dimens = AppTheme.dimens
    val shape = AppTheme.shapes.extraLarge
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(dimens.buttonHeight)
            .clip(shape)
            .border(dimens.divider, c.outline, shape),
    ) {
        options.forEachIndexed { index, option ->
            if (index > 0) {
                Box(
                    Modifier
                        .width(dimens.divider)
                        .fillMaxHeight()
                        .background(c.outline),
                )
            }
            val selected = option.id == selectedId
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(if (selected) Modifier.background(c.secondaryContainer) else Modifier)
                    .clickable { onSelect(option.id) },
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceS, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                icon?.invoke(option)
                AppText(
                    text = option.label,
                    style = AppTheme.typography.label,
                    color = if (selected) c.onSecondaryContainer else c.onSurfaceVariant,
                )
            }
        }
    }
}
