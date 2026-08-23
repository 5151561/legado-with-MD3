package io.legado.app.feature.catalog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.legado.app.core.designsystem.kit.AppIconSlot
import io.legado.app.core.designsystem.kit.AppText
import io.legado.app.core.designsystem.kit.AppTopAppBar
import io.legado.app.core.designsystem.theme.AppTheme
import io.legado.app.core.designsystem.theme.ProvideAppTheme

/**
 * 目录管理页（重设计画板 S-06b v2）。
 *
 * 无状态：只接收 [state] 与 [onIntent]。
 *
 * 选择态套模板 TPL-03：底部一条浮动条，主操作恒为「下载」（对象是章节），
 * 删除缓存收在「更多」里。列表底部写出范围选择的用法——它靠长按触发，
 * 不写出来等于没有。
 */
@Composable
fun TocManageScreen(
    state: TocManageUiState,
    onIntent: (TocManageIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = AppTheme.colorScheme
    val dimens = AppTheme.dimens
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.surface),
    ) {
        TocManageTopBar(state, onIntent)
        FilterRow(state, onIntent)

        LazyColumn(Modifier.weight(1f)) {
            itemsIndexed(state.chapters, key = { _, item -> item.id }) { index, chapter ->
                if (index > 0) {
                    Box(
                        Modifier
                            .padding(start = 50.dp)
                            .fillMaxWidth()
                            .height(dimens.divider)
                            .background(c.outlineVariant),
                    )
                }
                ChapterRow(
                    chapter = chapter,
                    selected = chapter.id in state.selected,
                    onIntent = onIntent,
                )
            }
            item("hint") {
                AppText(
                    text = state.hint,
                    style = AppTheme.typography.caption.copy(lineHeight = 16.8.sp),
                    color = c.outline,
                    modifier = Modifier.padding(
                        horizontal = dimens.spaceContent,
                        vertical = dimens.spaceXl,
                    ),
                )
            }
        }

        SelectionBar(state, onIntent)
    }
}

/** 顶栏：关闭 + 双行标题（动作 + 对象），右侧搜索。 */
@Composable
private fun TocManageTopBar(state: TocManageUiState, onIntent: (TocManageIntent) -> Unit) {
    val c = AppTheme.colorScheme
    val dimens = AppTheme.dimens
    AppTopAppBar(
        title = "选择章节",
        subtitle = state.subtitle,
        navigationIcon = {
            AppIconSlot(onClick = { onIntent(TocManageIntent.Close) }) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "关闭",
                    tint = c.onSurfaceVariant,
                    modifier = Modifier.size(dimens.iconLarge),
                )
            }
        },
        actions = {
            AppIconSlot(onClick = { onIntent(TocManageIntent.Search) }) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "搜索章节",
                    tint = c.onSurfaceVariant,
                    modifier = Modifier.size(dimens.iconLarge),
                )
            }
        },
    )
}

/**
 * 筛选条。每个筛选都带计数——「未缓存 172」比「未缓存」多说了一件事：
 * 点下去会不会是空的。失败筛选用 error 描边，因为它指向需要处理的东西。
 */
@Composable
private fun FilterRow(state: TocManageUiState, onIntent: (TocManageIntent) -> Unit) {
    val c = AppTheme.colorScheme
    val dimens = AppTheme.dimens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.spaceContent)
            .padding(bottom = dimens.spaceL),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
    ) {
        state.filters.forEach { filter ->
            val selected = filter.id == state.activeFilter
            val danger = filter.id == TocFilter.Failed
            Box(
                modifier = Modifier
                    .defaultMinSize(minHeight = dimens.minTouchTarget)
                    .clickable { onIntent(TocManageIntent.SelectFilter(filter.id)) },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .height(dimens.chipHeight)
                        .clip(AppTheme.shapes.small)
                        .then(
                            when {
                                selected -> Modifier.background(c.secondaryContainer)
                                danger -> Modifier.border(dimens.divider, c.error, AppTheme.shapes.small)
                                else -> Modifier.border(dimens.divider, c.outline, AppTheme.shapes.small)
                            },
                        )
                        .padding(horizontal = dimens.spaceXxl),
                    contentAlignment = Alignment.Center,
                ) {
                    AppText(
                        text = "${filter.label} ${filter.count}",
                        style = AppTheme.typography.label.copy(
                            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                        ),
                        color = when {
                            selected -> c.onSecondaryContainer
                            danger -> c.error
                            else -> c.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

/** 章节行：勾选框 + 标题与状态说明，失败项行尾带就地重试。 */
@Composable
private fun ChapterRow(
    chapter: TocManageChapterUi,
    selected: Boolean,
    onIntent: (TocManageIntent) -> Unit,
) {
    val c = AppTheme.colorScheme
    val dimens = AppTheme.dimens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (selected) Modifier.background(c.surfaceContainer) else Modifier)
            .clickable { onIntent(TocManageIntent.ToggleChapter(chapter.id)) }
            .defaultMinSize(minHeight = 60.dp)
            .padding(horizontal = dimens.spaceContent, vertical = dimens.spaceXl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceXl),
    ) {
        Icon(
            imageVector = if (selected) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
            contentDescription = null,
            tint = if (selected) c.primary else c.outline,
            modifier = Modifier.size(dimens.iconMedium),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceXxs),
        ) {
            AppText(
                text = chapter.title,
                style = AppTheme.typography.listBody.copy(lineHeight = 19.5.sp),
                color = c.onSurface,
                maxLines = 1,
            )
            AppText(
                text = chapter.note,
                style = AppTheme.typography.micro.copy(lineHeight = 14.3.sp),
                color = when (chapter.status) {
                    TocChapterStatus.Failed -> c.error
                    TocChapterStatus.Cached -> c.primary
                    TocChapterStatus.NotCached -> c.outline
                },
            )
        }
        if (chapter.retryable) {
            Box(
                modifier = Modifier
                    .defaultMinSize(minHeight = dimens.minTouchTarget)
                    .clip(AppTheme.shapes.full)
                    .clickable { onIntent(TocManageIntent.RetryChapter(chapter.id)) }
                    .padding(horizontal = dimens.spaceM),
                contentAlignment = Alignment.Center,
            ) {
                AppText(
                    text = "重试",
                    style = AppTheme.typography.captionStrong,
                    color = c.primary,
                )
            }
        }
    }
}

/**
 * 选择态底部浮动条（模板 TPL-03）。
 *
 * 左侧是选择工具与计数，右侧是主操作与「更多」。主操作按对象固定为下载，
 * 危险动作不在这一层露出。
 */
@Composable
private fun SelectionBar(state: TocManageUiState, onIntent: (TocManageIntent) -> Unit) {
    val c = AppTheme.colorScheme
    val dimens = AppTheme.dimens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.spaceXl, vertical = dimens.spaceXl)
            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
            .clip(AppTheme.shapes.extraLarge)
            .background(c.surfaceContainerHigh)
            .padding(horizontal = dimens.spaceM, vertical = dimens.spaceL),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceXxs),
    ) {
        BarAction("全选", c.primary) { onIntent(TocManageIntent.SelectAll) }
        BarAction("反选", c.primary) { onIntent(TocManageIntent.InvertSelection) }
        Box(
            modifier = Modifier
                .height(44.dp)
                .padding(horizontal = dimens.spaceL),
            contentAlignment = Alignment.Center,
        ) {
            AppText(
                text = "已选 ${state.selected.size}",
                style = AppTheme.typography.label.copy(fontSize = 14.sp, fontWeight = FontWeight.Normal),
                color = c.onSurfaceVariant,
            )
        }
        Box(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .height(44.dp)
                .clip(AppTheme.shapes.full)
                .background(c.primary)
                .clickable(enabled = state.selected.isNotEmpty()) {
                    onIntent(TocManageIntent.DownloadSelected)
                }
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
        ) {
            Icon(
                imageVector = Icons.Outlined.Download,
                contentDescription = null,
                tint = c.onPrimary,
                modifier = Modifier.size(dimens.iconSmall),
            )
            AppText(
                text = "下载",
                style = AppTheme.typography.label.copy(fontSize = 14.sp),
                color = c.onPrimary,
            )
        }
        Row(
            modifier = Modifier
                .height(44.dp)
                .clip(AppTheme.shapes.full)
                .clickable { onIntent(TocManageIntent.OpenMoreMenu) }
                .padding(horizontal = dimens.spaceXl),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceXxs),
        ) {
            AppText(
                text = "更多",
                style = AppTheme.typography.label.copy(fontSize = 14.sp),
                color = c.onSurfaceVariant,
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = c.onSurfaceVariant,
                modifier = Modifier.size(dimens.iconSmall),
            )
        }
    }
}

@Composable
private fun BarAction(text: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(44.dp)
            .clip(AppTheme.shapes.full)
            .clickable(onClick = onClick)
            .padding(horizontal = AppTheme.dimens.spaceXl),
        contentAlignment = Alignment.Center,
    ) {
        AppText(text = text, style = AppTheme.typography.label.copy(fontSize = 14.sp), color = color)
    }
}

@Preview(name = "S-06b v2 目录管理页 · 日光", widthDp = 390, heightDp = 844)
@Composable
private fun TocManageLightPreview() {
    ProvideAppTheme(dark = false) {
        TocManageScreen(state = TocManagePreviewState, onIntent = {})
    }
}

@Preview(name = "S-06b v2 目录管理页 · 夜墨", widthDp = 390, heightDp = 844)
@Composable
private fun TocManageDarkPreview() {
    ProvideAppTheme(dark = true) {
        TocManageScreen(state = TocManagePreviewState, onIntent = {})
    }
}
