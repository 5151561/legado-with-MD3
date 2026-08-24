package io.legado.app.core.designsystem.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.legado.app.core.designsystem.theme.AppTheme
import kotlinx.collections.immutable.ImmutableList

/**
 * 一级导航栏的竖向形态，用于中等与展开宽度（画板 X-01 平板三栏 / X-02 折叠屏双栏）。
 *
 * **取值是推导的，不是稿面值**：X-01 与 X-02 尚未从画板墙导入，
 * 因此这里没有一个属于导轨自己的设计稿。做法与 `ReadingPalette` 扩档时相同——
 * 沿用 [AppNavigationBar] 的全部令牌（surfaceContainer 底、64×32dp 选中药丸、
 * 24dp 图标、12sp 标签、error 底角标），只把主轴从横改竖，宽度取药丸宽 64dp
 * 加两侧 spaceContent。X-01 导入后按稿面值逐项复核。
 *
 * **不处理系统 inset**：理由同 [AppNavigationBar]，让位归宿主。
 *
 * @param icon 由调用方提供图标，参数为该项与它的选中态。
 */
@Composable
fun AppNavigationRail(
    items: ImmutableList<AppNavigationItem>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (item: AppNavigationItem, selected: Boolean) -> Unit,
) {
    val c = AppTheme.colorScheme
    val dimens = AppTheme.dimens
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(c.surfaceContainer)
            .width(RailWidth)
            .padding(vertical = dimens.spaceGroup),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimens.spaceXl, Alignment.CenterVertically),
    ) {
        items.forEach { item ->
            val selected = item.id == selectedId
            Column(
                modifier = Modifier
                    .defaultMinSize(minHeight = dimens.minTouchTarget)
                    .clickable { onSelect(item.id) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dimens.spaceXs, Alignment.CenterVertically),
            ) {
                Box(
                    modifier = Modifier
                        .then(if (selected) Modifier.width(SelectedPillWidth) else Modifier)
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
                            NavigationRailBadge(
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

/** 选中药丸宽，与 [AppNavigationBar] 同值。 */
private val SelectedPillWidth = 64.dp

/** 药丸宽加两侧 spaceContent。 */
private val RailWidth = 96.dp

@Composable
private fun NavigationRailBadge(count: Int, modifier: Modifier = Modifier) {
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
