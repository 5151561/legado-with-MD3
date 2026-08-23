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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.OfflinePin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.legado.app.core.designsystem.kit.AppText
import io.legado.app.core.designsystem.theme.AppTheme
import io.legado.app.core.designsystem.theme.ProvideAppTheme
import io.legado.app.core.designsystem.theme.ReadingPaperNight

/**
 * 阅读器内目录（重设计画板 S-06a v2）。
 *
 * 无状态：只接收 [state] 与 [onIntent]。
 *
 * **只做跳转**：点一行就跳到那一章，没有多选也没有批量操作。底部固定一条通往
 * 目录管理页的出口——批量下载与章节管理在那里（画板 S-06b），职责不重叠（解 L5）。
 *
 * 配色走纸色而非 App 的 surface 色阶：这块面板是从正文里滑出来的，
 * 换一套材质会把阅读面和操作面撕开（设计规则「阅读面与操作面分离」的另一半——
 * 分离说的是正文与菜单的字体与对比度，不是让面板变成另一个 App）。
 *
 * @param modifier 由宿主定位。本组件只画面板本身，遮罩与进出动画归宿主。
 */
@Composable
fun ReaderTocSheet(
    state: ReaderTocUiState,
    onIntent: (ReaderTocIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val paper = AppTheme.reading
    val dimens = AppTheme.dimens
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.bottomSheet)
            .background(paper.paperHigh)
            .padding(top = dimens.spaceXl, bottom = 22.dp),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = dimens.spaceL)
                .size(width = 32.dp, height = 4.dp)
                .clip(AppTheme.shapes.indicator)
                .background(paper.paperOutline),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.spaceXl, vertical = 0.dp)
                .padding(bottom = dimens.spaceM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs),
        ) {
            AppText(
                text = "目录",
                style = AppTheme.typography.chapterTitle.copy(
                    fontWeight = FontWeight.Normal,
                    lineHeight = 24.sp,
                ),
                color = paper.ink,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = dimens.spaceM),
            )
            PaperIconSlot(Icons.Outlined.Search, "搜索章节", paper.inkDim) {
                onIntent(ReaderTocIntent.Search)
            }
            PaperIconSlot(Icons.Outlined.SwapVert, "正序 / 倒序", paper.inkDim) {
                onIntent(ReaderTocIntent.ToggleOrder)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.spaceSection)
                .padding(bottom = dimens.spaceL),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceL),
        ) {
            AppText(
                text = state.summary,
                style = AppTheme.typography.caption.copy(lineHeight = 16.8.sp),
                color = paper.inkFaint,
                modifier = Modifier.weight(1f),
            )
            Row(
                modifier = Modifier
                    .defaultMinSize(minHeight = dimens.minTouchTarget)
                    .clickable { onIntent(ReaderTocIntent.BackToCurrent) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .height(dimens.chipHeight)
                        .clip(AppTheme.shapes.full)
                        .border(dimens.divider, paper.paperOutline, AppTheme.shapes.full)
                        .padding(horizontal = dimens.spaceXl),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceS),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MyLocation,
                        contentDescription = null,
                        tint = paper.inkDim,
                        modifier = Modifier.size(16.dp),
                    )
                    AppText(
                        text = "回到当前",
                        style = AppTheme.typography.caption.copy(lineHeight = 12.sp),
                        color = paper.inkDim,
                    )
                }
            }
        }

        LazyColumn(
            // 面板不占满全屏：目录之上要留出正文，用户才知道自己没有离开阅读。
            modifier = Modifier.heightIn(max = 520.dp),
        ) {
            itemsIndexed(state.chapters, key = { _, item -> item.id }) { index, chapter ->
                if (index > 0) {
                    Box(
                        Modifier
                            .padding(start = 50.dp)
                            .fillMaxWidth()
                            .height(dimens.divider)
                            .background(paper.paperOutline),
                    )
                }
                TocRow(chapter = chapter, onIntent = onIntent)
            }
        }

        Box(
            Modifier
                .padding(top = dimens.spaceL)
                .padding(horizontal = dimens.spaceSection)
                .fillMaxWidth()
                .height(dimens.divider)
                .background(paper.paperOutline),
        )
        Row(
            modifier = Modifier
                .padding(horizontal = dimens.spaceSection)
                .fillMaxWidth()
                .clickable { onIntent(ReaderTocIntent.OpenTocManage) }
                .padding(top = dimens.spaceXl)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceXl),
        ) {
            Icon(
                imageVector = Icons.Outlined.DownloadForOffline,
                contentDescription = null,
                tint = paper.inkDim,
                modifier = Modifier.size(dimens.iconSmall),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimens.spaceXxs),
            ) {
                AppText(
                    text = "批量下载与章节管理",
                    style = AppTheme.typography.listBody.copy(fontSize = 14.sp, lineHeight = 18.2.sp),
                    color = paper.ink,
                )
                AppText(
                    text = "→ 书籍详情 · 目录管理页（S-06b）",
                    style = AppTheme.typography.micro.copy(lineHeight = 14.3.sp),
                    color = paper.inkFaint,
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = paper.inkFaint,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * 目录行。当前章有底色、加粗与进度条；已读只在行尾写「已读」，不改字色——
 * 目录是用来找位置的，把读过的章节压暗反而更难扫。
 */
@Composable
private fun TocRow(chapter: TocChapterUi, onIntent: (ReaderTocIntent) -> Unit) {
    val paper = AppTheme.reading
    val c = AppTheme.colorScheme
    val dimens = AppTheme.dimens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (chapter.isCurrent) Modifier.background(paper.paperHighlight) else Modifier)
            .clickable { onIntent(ReaderTocIntent.JumpTo(chapter.id)) }
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = dimens.spaceSection, vertical = dimens.spaceXl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceXl),
    ) {
        Icon(
            imageVector = chapter.statusIcon(),
            contentDescription = null,
            tint = when (chapter.status) {
                TocChapterStatus.Cached -> c.primary
                TocChapterStatus.NotCached -> paper.inkFaint
                TocChapterStatus.Failed -> c.error
            },
            modifier = Modifier.size(18.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(if (chapter.isCurrent) 3.dp else dimens.spaceXxs),
        ) {
            AppText(
                text = chapter.title,
                style = AppTheme.typography.listBody.copy(
                    fontWeight = if (chapter.isCurrent) FontWeight.Medium else FontWeight.Normal,
                    lineHeight = 19.5.sp,
                ),
                color = if (chapter.status == TocChapterStatus.Cached) paper.ink else paper.inkDim,
                maxLines = 1,
            )
            chapter.note?.let {
                AppText(
                    text = it,
                    style = AppTheme.typography.micro.copy(lineHeight = 14.3.sp),
                    color = if (chapter.status == TocChapterStatus.Failed) c.error else paper.inkFaint,
                )
            }
            if (chapter.progress != null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(AppTheme.shapes.indicator)
                        .background(paper.paperOutline),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(chapter.progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(c.primary),
                    )
                }
            }
        }
        when {
            chapter.progressLabel != null -> AppText(
                text = chapter.progressLabel,
                style = AppTheme.typography.micro,
                color = c.primary,
            )

            chapter.isRead -> AppText(
                text = "已读",
                style = AppTheme.typography.micro,
                color = paper.inkFaint,
            )
        }
    }
}

private fun TocChapterUi.statusIcon(): ImageVector = when {
    isCurrent -> Icons.Filled.PlayCircle
    status == TocChapterStatus.Cached -> Icons.Outlined.OfflinePin
    status == TocChapterStatus.NotCached -> Icons.Outlined.CloudDownload
    else -> Icons.Outlined.Error
}

/** 纸面上的 44dp 图标槽位。稿面用 44dp，比 App 面的 48dp 略紧，因为面板寸土寸金。 */
@Composable
private fun PaperIconSlot(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(AppTheme.dimens.iconMedium),
        )
    }
}

/**
 * 对稿用的宿主：面板贴底，上方留出正文。稿面上正文与面板之间有一层渐变，
 * 那是遮罩的一部分，属于宿主职责，因此画在这里而不是面板里。
 */
@Composable
private fun ReaderTocPreviewHost() {
    val paper = AppTheme.reading
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(paper.paper),
        verticalArrangement = Arrangement.Bottom,
    ) {
        ReaderTocSheet(state = ReaderTocPreviewState, onIntent = {})
    }
}

@Preview(name = "S-06a v2 阅读器内目录 · 纸", widthDp = 390, heightDp = 844)
@Composable
private fun ReaderTocPreview() {
    ProvideAppTheme(dark = false) { ReaderTocPreviewHost() }
}

@Preview(name = "S-06a v2 阅读器内目录 · 夜纸", widthDp = 390, heightDp = 844)
@Composable
private fun ReaderTocNightPreview() {
    ProvideAppTheme(dark = true, readingPalette = ReadingPaperNight) { ReaderTocPreviewHost() }
}
