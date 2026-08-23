package io.legado.app.core.designsystem.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.legado.app.core.designsystem.theme.AppTheme
import kotlinx.collections.immutable.ImmutableList

/** 一级导航栏的一项。[id] 用作选中判定与回调标识。 */
@Immutable
data class AppNavigationItem(
    val id: String,
    val label: String,
    /** 未读数，0 表示不显示角标。 */
    val badgeCount: Int = 0,
)

/**
 * 一级导航栏（画板 M-01 首页、P-01 我的，五个 tab 共用同一实现）。
 *
 * 规格：上内边距 12dp、下内边距 16dp、surfaceContainer 底、五列等分；
 * 选中项的图标外套 64×32dp 全圆角 secondaryContainer 药丸，未选中项只有 32dp 高的图标行；
 * 图标 24dp、与文字间距 4dp，标签 12sp（选中 500 / onSurface，未选中 400 / onSurfaceVariant）。
 *
 * 未读角标压在图标右上角，最小 16dp、全圆角、error 底、10sp。
 *
 * **不处理导航栏 inset**：系统手势条让位归宿主施加——设计系统不猜宿主的布局方式。
 *
 * @param icon 由调用方提供图标，参数为该项与它的选中态（选中态在稿面上是实心字重）。
 */
@Composable
fun AppNavigationBar(
    items: ImmutableList<AppNavigationItem>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (item: AppNavigationItem, selected: Boolean) -> Unit,
) {
    val c = AppTheme.colorScheme
    val dimens = AppTheme.dimens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(c.surfaceContainer)
            .padding(top = dimens.spaceXl, bottom = dimens.spaceContent),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            val selected = item.id == selectedId
            Column(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = dimens.minTouchTarget)
                    .clickable { onSelect(item.id) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dimens.spaceXs, Alignment.CenterVertically),
            ) {
                Box(
                    modifier = Modifier
                        .then(if (selected) Modifier.width(64.dp) else Modifier)
                        .height(dimens.chipHeight)
                        .then(
                            if (selected) {
                                Modifier.clip(AppTheme.shapes.full).background(c.secondaryContainer)
                            } else {
                                Modifier
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier.size(dimens.iconLarge),
                        contentAlignment = Alignment.Center,
                    ) {
                        icon(item, selected)
                        if (item.badgeCount > 0) {
                            // 稿面上角标右移溢出图标 4dp。用内边距实现会把图标槽撑宽，故用偏移。
                            NavigationBadge(
                                count = item.badgeCount,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = dimens.spaceXs, y = -dimens.spaceXxs),
                            )
                        }
                    }
                }
                AppText(
                    text = item.label,
                    style = AppTheme.typography.caption.copy(
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                        lineHeight = 12.sp,
                    ),
                    color = if (selected) c.onSurface else c.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun NavigationBadge(count: Int, modifier: Modifier = Modifier) {
    val c = AppTheme.colorScheme
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 16.dp)
            .height(16.dp)
            .clip(AppTheme.shapes.full)
            .background(c.error)
            .padding(horizontal = AppTheme.dimens.spaceXs),
        contentAlignment = Alignment.Center,
    ) {
        AppText(
            text = count.toString(),
            style = AppTheme.typography.micro.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
            color = c.onError,
            maxLines = 1,
        )
    }
}
