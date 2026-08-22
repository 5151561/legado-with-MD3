package io.legado.app.core.designsystem.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.legado.app.core.designsystem.component.AppText
import io.legado.app.core.designsystem.theme.AppTheme

/**
 * 分组小标题：标签 + 可选提示（画板 C-01「设置主页」的三组标题）。
 * 标签 13sp / 字重 500 / 字距 .06em，提示 11sp。
 */
@Composable
fun AppSectionHeader(
    label: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
) {
    val c = AppTheme.colorScheme
    val dimens = AppTheme.dimens
    Row(
        modifier = modifier.padding(horizontal = dimens.spaceS),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
    ) {
        AppText(
            text = label,
            style = AppTheme.typography.label.copy(letterSpacing = 0.78.sp),
            color = c.onSurfaceVariant,
        )
        if (hint != null) {
            AppText(text = hint, style = AppTheme.typography.micro, color = c.outline)
        }
    }
}

/**
 * 分组卡：圆角 18dp、最浅表面填充、1dp 描边，内部行之间由 1dp 分隔线隔开
 * （画板 C-01）。子项用 [AppSettingRow]。
 */
@Composable
fun AppGroupCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = AppTheme.colorScheme
    val shape = AppTheme.shapes.largeIncreased
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surfaceContainerLowest)
            .border(AppTheme.dimens.divider, c.outlineVariant, shape),
        content = content,
    )
}

/**
 * 设置行。最小高 54dp、内边距 8/14、槽位间距 14dp；
 * 前导为 40dp 圆底图标，主文 15sp/500，摘要 12sp，尾随箭头 20dp（画板 C-01）。
 *
 * @param showDivider 行间 1dp 分隔线。分组内第一行传 false。
 */
@Composable
fun AppSettingRow(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    showDivider: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val c = AppTheme.colorScheme
    val dimens = AppTheme.dimens
    Column(modifier = modifier.fillMaxWidth()) {
        if (showDivider) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(dimens.divider)
                    .background(c.surfaceContainer),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .defaultMinSize(minHeight = dimens.rowMinHeight)
                .padding(horizontal = dimens.spaceXxl, vertical = dimens.spaceM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceXxl),
        ) {
            if (leading != null) {
                Box(
                    modifier = Modifier
                        .size(dimens.leadingIcon)
                        .clip(AppTheme.shapes.full)
                        .background(c.surfaceContainer),
                    contentAlignment = Alignment.Center,
                ) { leading() }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                AppText(text = title, style = AppTheme.typography.listTitle, color = c.onSurface)
                if (summary != null) {
                    AppText(
                        text = summary,
                        style = AppTheme.typography.caption,
                        color = c.outline,
                        maxLines = 1,
                    )
                }
            }
            trailing?.invoke()
        }
    }
}
