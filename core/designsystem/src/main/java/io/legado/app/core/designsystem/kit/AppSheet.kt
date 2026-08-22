package io.legado.app.core.designsystem.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.legado.app.core.designsystem.component.AppText
import io.legado.app.core.designsystem.theme.AppTheme

/**
 * 底部面板的视觉容器（画板 B-03 显示配置、R-02 系列阅读设置抽屉）。
 *
 * 顶部两角圆角 28dp、surfaceContainerLow 底、底部内边距 24dp，
 * 水平内边距 20dp 由内容自行施加（分隔线与整幅列表项需要贴边）。
 *
 * **只负责视觉**：遮罩、进出动画、返回键与窗口 inset 属于宿主职责，
 * 由调用方用 Material 3 的 `ModalBottomSheet` 或自有容器承载——
 * 设计系统不重复实现一套面板行为。
 */
@Composable
fun AppSheetContainer(
    modifier: Modifier = Modifier,
    containerColor: Color = AppTheme.colorScheme.surfaceContainerLow,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.bottomSheet)
            .background(containerColor)
            .padding(bottom = AppTheme.dimens.spaceGroup),
        content = content,
    )
}

/**
 * 拖动把手：32×4dp、圆角 2dp、outlineVariant。
 * 纯装饰，对无障碍隐藏——拖动语义由宿主面板提供。
 */
@Composable
fun AppSheetHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(22.dp)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .width(32.dp)
                .height(4.dp)
                .clip(AppTheme.shapes.indicator)
                .background(AppTheme.colorScheme.outlineVariant),
        )
    }
}

/**
 * 面板标题行（画板 R-02：标题 + 预设说明 + 右侧操作）。
 * 标题 17sp / 字重 500，水平内边距 20dp。
 */
@Composable
fun AppSheetHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.dimens.spaceSection)
            .padding(bottom = AppTheme.dimens.spaceXs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.spaceL),
    ) {
        AppText(
            text = title,
            style = AppTheme.typography.listTitle.copy(fontSize = 17.sp),
            color = AppTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        trailing()
    }
}

/**
 * 面板内的分节：小标题 + 内容，标题 13sp / 字重 500 / onSurfaceVariant，
 * 与内容间距 10dp（画板 B-03「布局」「排序」等节）。
 */
@Composable
fun AppSheetSection(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.dimens.spaceSection)
            .padding(top = AppTheme.dimens.spaceM, bottom = AppTheme.dimens.spaceXs),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.spaceL),
    ) {
        AppText(
            text = label,
            style = AppTheme.typography.label,
            color = AppTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

/** 面板遮罩的标准不透明度（画板 B-03：32% 黑）。供宿主设置 scrim 时取用。 */
val AppSheetScrimAlpha = 0.32f

/** 面板遮罩色。压在任意内容之上，与主题无关，故不走语义槽位。 */
val AppSheetScrimColor = Color.Black.copy(alpha = AppSheetScrimAlpha)
