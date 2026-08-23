package io.legado.app.feature.catalog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.automirrored.outlined.Toc
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.BookmarkRemove
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.legado.app.core.designsystem.kit.AppEntryRow
import io.legado.app.core.designsystem.kit.AppIconSlot
import io.legado.app.core.designsystem.kit.AppLinearProgress
import io.legado.app.core.designsystem.kit.AppSheetScrimColor
import io.legado.app.core.designsystem.kit.AppText
import io.legado.app.core.designsystem.kit.AppTopAppBar
import io.legado.app.core.designsystem.theme.AppTheme
import io.legado.app.core.designsystem.theme.ProvideAppTheme
import kotlinx.collections.immutable.ImmutableList

/**
 * 书籍详情（重设计画板 S-04 v2，更多菜单与确认框见 S-04a v2）。
 *
 * 无状态：只接收 [state] 与 [onIntent]，不注入 ViewModel、不读取偏好。
 *
 * 更多菜单与确认框都画在页面内的浮层里，而不是用 `DropdownMenu` / `AlertDialog`：
 * 那两者各自开一个系统窗口，落在截图基线的根节点之外，稿面上「菜单 + 确认框」的
 * 排版就无法回归。层级与遮罩本来也是这一页要负责的（模板 TPL-05）。
 *
 * @param bookCover 封面图槽位，与设计系统 kit 同一立场：本模块不依赖图片加载库。
 */
@Composable
fun BookDetailScreen(
    state: BookDetailUiState,
    onIntent: (BookDetailIntent) -> Unit,
    modifier: Modifier = Modifier,
    bookCover: @Composable BoxScope.(bookId: String) -> Unit = {},
) {
    val c = AppTheme.colorScheme
    val dimens = AppTheme.dimens
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(c.surface),
    ) {
        Column(Modifier.fillMaxSize()) {
            AppTopAppBar(
                title = "",
                navigationIcon = {
                    AppIconSlot(onClick = { onIntent(BookDetailIntent.Back) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "返回",
                            tint = c.onSurfaceVariant,
                            modifier = Modifier.size(dimens.iconLarge),
                        )
                    }
                },
                actions = {
                    AppIconSlot(onClick = { onIntent(BookDetailIntent.Share) }) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "分享",
                            tint = c.onSurfaceVariant,
                            modifier = Modifier.size(dimens.iconLarge),
                        )
                    }
                    AppIconSlot(onClick = { onIntent(BookDetailIntent.OpenMenu) }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "更多",
                            tint = c.onSurfaceVariant,
                            modifier = Modifier.size(dimens.iconLarge),
                        )
                    }
                },
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = dimens.spaceContent,
                        end = dimens.spaceContent,
                        bottom = dimens.spaceXl +
                            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                    ),
                verticalArrangement = Arrangement.spacedBy(dimens.spaceContent),
            ) {
                state.header?.let { DetailHeader(it, bookCover) }
                PrimaryActions(onIntent)
                state.source?.let { SourceCard(it, onIntent) }
                if (state.intro.isNotEmpty()) Intro(state, onIntent)
                if (state.entries.isNotEmpty()) EntryBlock(state, onIntent)
                if (state.related.isNotEmpty()) RelatedBooks(state, onIntent, bookCover)
            }
        }

        state.menu?.let { MoreMenuOverlay(it, onIntent) }
        state.activeDialog?.let { RemoveDialogOverlay(it, onIntent) }
    }
}

/** 头部：104×150 封面 + 书名、署名行、章节摘要、书架标签、进度。 */
@Composable
private fun DetailHeader(
    header: BookDetailHeaderUi,
    bookCover: @Composable BoxScope.(bookId: String) -> Unit,
) {
    val c = AppTheme.colorScheme
    val dimens = AppTheme.dimens
    Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceContent)) {
        Box(
            modifier = Modifier
                .width(104.dp)
                .height(150.dp)
                .clip(AppTheme.shapes.medium)
                .background(c.surfaceContainerHighest),
        ) { bookCover(header.bookId) }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceS),
        ) {
            AppText(
                text = header.name,
                style = AppTheme.typography.chapterTitle.copy(lineHeight = 27.sp),
                color = c.onSurface,
                maxLines = 2,
            )
            AppText(
                text = header.byline,
                style = AppTheme.typography.label.copy(lineHeight = 18.2.sp),
                color = c.onSurfaceVariant,
            )
            AppText(
                text = header.chapterSummary,
                style = AppTheme.typography.caption.copy(lineHeight = 16.8.sp),
                color = c.outline,
            )
            header.shelfLabel?.let {
                Box(
                    modifier = Modifier
                        .padding(top = dimens.spaceXxs)
                        .height(22.dp)
                        .clip(AppTheme.shapes.small)
                        .background(c.secondaryContainer)
                        .padding(horizontal = dimens.spaceM),
                    contentAlignment = Alignment.Center,
                ) {
                    AppText(
                        text = it,
                        style = AppTheme.typography.micro,
                        color = c.onSecondaryContainer,
                    )
                }
            }
            if (header.progress != null) {
                Row(
                    modifier = Modifier.padding(top = dimens.spaceXs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
                ) {
                    AppLinearProgress(
                        progress = header.progress,
                        modifier = Modifier.weight(1f),
                        trackColor = c.surfaceContainerHighest,
                    )
                    header.progressLabel?.let { label ->
                        AppText(
                            text = label,
                            style = AppTheme.typography.micro,
                            color = c.outline,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 主操作行：继续阅读占满剩余宽度，听书与下载各占一个 52dp 圆钮。
 * 三者都是 52dp，本身已高于 48dp 触点下限，不需要外层补足。
 */
@Composable
private fun PrimaryActions(onIntent: (BookDetailIntent) -> Unit) {
    val c = AppTheme.colorScheme
    val dimens = AppTheme.dimens
    Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceL)) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .clip(AppTheme.shapes.full)
                .background(c.primary)
                .clickable { onIntent(BookDetailIntent.ContinueReading) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceM, Alignment.CenterHorizontally),
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = c.onPrimary,
                modifier = Modifier.size(dimens.iconSmall),
            )
            AppText(text = "继续阅读", style = AppTheme.typography.listTitle, color = c.onPrimary)
        }
        CircleAction(Icons.Outlined.Headphones, "听书") { onIntent(BookDetailIntent.ListenAloud) }
        CircleAction(Icons.Outlined.Download, "下载") { onIntent(BookDetailIntent.Download) }
    }
}

@Composable
private fun CircleAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    val c = AppTheme.colorScheme
    val shape = AppTheme.shapes.full
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(shape)
            .border(AppTheme.dimens.divider, c.outline, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = c.onSurfaceVariant,
            modifier = Modifier.size(AppTheme.dimens.iconMedium),
        )
    }
}

/** 当前源卡。换源本身走画板 S-08 的统一组件，这里只发意图。 */
@Composable
private fun SourceCard(source: BookSourceSummaryUi, onIntent: (BookDetailIntent) -> Unit) {
    val c = AppTheme.colorScheme
    val dimens = AppTheme.dimens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.large)
            .background(c.surfaceContainer)
            .padding(horizontal = dimens.spaceContent, vertical = dimens.spaceXxl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceXl),
    ) {
        Icon(
            imageVector = Icons.Outlined.TravelExplore,
            contentDescription = null,
            tint = c.onSurfaceVariant,
            modifier = Modifier.size(dimens.iconMedium),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceXxs),
        ) {
            AppText(
                text = source.name,
                style = AppTheme.typography.label.copy(lineHeight = 16.9.sp),
                color = c.onSurface,
            )
            AppText(
                text = source.alternativesLabel,
                style = AppTheme.typography.micro.copy(lineHeight = 14.3.sp),
                color = c.outline,
            )
        }
        Box(
            modifier = Modifier
                .height(36.dp)
                .clip(AppTheme.shapes.full)
                .background(c.surfaceContainerLowest)
                .clickable { onIntent(BookDetailIntent.ChangeSource) }
                .padding(horizontal = dimens.spaceXxl),
            contentAlignment = Alignment.Center,
        ) {
            AppText(text = "换源", style = AppTheme.typography.label, color = c.primary)
        }
    }
}

@Composable
private fun Intro(state: BookDetailUiState, onIntent: (BookDetailIntent) -> Unit) {
    val c = AppTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.spaceM)) {
        AppText(
            text = state.intro,
            style = AppTheme.typography.listBody.copy(fontSize = 14.sp, lineHeight = 24.5.sp),
            color = c.onSurfaceVariant,
            maxLines = if (state.introExpanded) Int.MAX_VALUE else 3,
        )
        Box(
            modifier = Modifier
                .clip(AppTheme.shapes.small)
                .clickable { onIntent(BookDetailIntent.ToggleIntro) }
                .padding(vertical = AppTheme.dimens.spaceS),
        ) {
            AppText(
                text = if (state.introExpanded) "收起" else "展开",
                style = AppTheme.typography.caption,
                color = c.primary,
            )
        }
    }
}

/** 目录与知识两条入口，上下各一条分隔线把它们与前后内容分开。 */
@Composable
private fun EntryBlock(state: BookDetailUiState, onIntent: (BookDetailIntent) -> Unit) {
    val c = AppTheme.colorScheme
    val dimens = AppTheme.dimens
    Column(Modifier.fillMaxWidth()) {
        HairLine()
        state.entries.forEach { entry ->
            AppEntryRow(
                title = entry.title,
                summary = entry.summary,
                onClick = { onIntent(BookDetailIntent.OpenEntry(entry.id)) },
                leading = {
                    Icon(
                        imageVector = entry.id.icon(),
                        contentDescription = null,
                        tint = c.onSurfaceVariant,
                        modifier = Modifier.size(dimens.iconMedium),
                    )
                },
            ) {
                entry.valueLabel?.let {
                    AppText(text = it, style = AppTheme.typography.micro, color = c.outline)
                }
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = c.outlineVariant,
                    modifier = Modifier.size(dimens.iconSmall),
                )
            }
        }
        HairLine()
    }
}

@Composable
private fun HairLine() {
    Box(
        Modifier
            .padding(vertical = AppTheme.dimens.spaceXxs)
            .fillMaxWidth()
            .height(AppTheme.dimens.divider)
            .background(AppTheme.colorScheme.outlineVariant),
    )
}

@Composable
private fun RelatedBooks(
    state: BookDetailUiState,
    onIntent: (BookDetailIntent) -> Unit,
    bookCover: @Composable BoxScope.(bookId: String) -> Unit,
) {
    val c = AppTheme.colorScheme
    val dimens = AppTheme.dimens
    Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceL)) {
        AppText(
            text = "相关书籍",
            style = AppTheme.typography.label.copy(fontSize = 14.sp),
            color = c.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceXl)) {
            state.related.forEach { book ->
                Column(
                    modifier = Modifier
                        .width(74.dp)
                        .clickable { onIntent(BookDetailIntent.OpenRelated(book.id)) },
                    verticalArrangement = Arrangement.spacedBy(dimens.spaceS),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(104.dp)
                            .clip(AppTheme.shapes.small)
                            .background(c.surfaceContainerHighest),
                    ) { bookCover(book.id) }
                    AppText(
                        text = book.title,
                        style = AppTheme.typography.micro.copy(lineHeight = 14.3.sp),
                        color = c.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * 更多菜单（画板 S-04a v2）。
 *
 * 危险项排在最后一组、与上面隔一条分隔线；「移出书架」与「删除本地文件」分开列，
 * 因为它们删的是两种不同的对象。
 */
@Composable
private fun BoxScope.MoreMenuOverlay(
    items: ImmutableList<BookDetailMenuItemUi>,
    onIntent: (BookDetailIntent) -> Unit,
) {
    val c = AppTheme.colorScheme
    val dimens = AppTheme.dimens
    Box(
        Modifier
            .fillMaxSize()
            .background(AppSheetScrimColor)
            .clickable { onIntent(BookDetailIntent.DismissMenu) },
    )
    Column(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = dimens.spaceXl)
            .width(250.dp)
            .clip(AppTheme.shapes.large)
            .background(c.surfaceContainerLowest)
            .padding(vertical = dimens.spaceM),
    ) {
        items.forEachIndexed { index, item ->
            val firstDangerous = item.dangerous && items.getOrNull(index - 1)?.dangerous == false
            if (firstDangerous) {
                Box(
                    Modifier
                        .padding(vertical = dimens.spaceS)
                        .fillMaxWidth()
                        .height(dimens.divider)
                        .background(c.outlineVariant),
                )
            }
            val tint = if (item.dangerous) c.error else c.onSurfaceVariant
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimens.minTouchTarget)
                    .clickable { onIntent(BookDetailIntent.SelectMenuAction(item.action)) }
                    .padding(horizontal = dimens.spaceContent),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceXxl),
            ) {
                Icon(
                    imageVector = item.action.icon(),
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(dimens.iconSmall),
                )
                Column(Modifier.weight(1f)) {
                    AppText(
                        text = item.title,
                        style = AppTheme.typography.listBody.copy(lineHeight = 19.5.sp),
                        color = if (item.dangerous) c.error else c.onSurface,
                    )
                    item.summary?.let {
                        AppText(
                            text = it,
                            style = AppTheme.typography.micro.copy(lineHeight = 14.3.sp),
                            color = c.outline,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 移出书架确认（画板 S-04a v2）。
 *
 * 标题点名对象，正文写清影响范围与体积；「同时删除本地文件」是需要单独勾选的第二意图，
 * 默认不选，并且把文件路径写出来——用户要能判断被删的是哪一个文件。
 */
@Composable
private fun BoxScope.RemoveDialogOverlay(
    dialog: BookDetailDialog,
    onIntent: (BookDetailIntent) -> Unit,
) {
    val c = AppTheme.colorScheme
    val dimens = AppTheme.dimens
    val remove = dialog as BookDetailDialog.RemoveFromShelf
    Box(
        Modifier
            .fillMaxSize()
            .background(AppSheetScrimColor)
            .clickable { onIntent(BookDetailIntent.DismissDialog) },
    )
    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .padding(horizontal = dimens.spaceGroup)
            .fillMaxWidth()
            .clip(AppTheme.shapes.extraExtraLarge)
            .background(c.surfaceContainerLow)
            .padding(dimens.spaceGroup),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceContent),
    ) {
        Icon(
            imageVector = Icons.Outlined.BookmarkRemove,
            contentDescription = null,
            tint = c.error,
            modifier = Modifier.size(dimens.iconLarge),
        )
        AppText(
            text = "把《${remove.bookName}》移出书架？",
            style = AppTheme.typography.bookTitleLarge.copy(fontSize = 24.sp, lineHeight = 32.4.sp),
            color = c.onSurface,
        )
        AppText(
            text = remove.impact,
            style = AppTheme.typography.listBody.copy(fontSize = 14.sp, lineHeight = 22.4.sp),
            color = c.onSurfaceVariant,
        )
        remove.localFilePath?.let { path ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppTheme.shapes.medium)
                    .background(c.surfaceContainer)
                    .clickable { onIntent(BookDetailIntent.SetDeleteLocalFile(!remove.deleteLocalFile)) }
                    .padding(horizontal = dimens.spaceXxl, vertical = dimens.spaceXl),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceL),
            ) {
                Icon(
                    imageVector = if (remove.deleteLocalFile) {
                        Icons.Outlined.CheckBox
                    } else {
                        Icons.Outlined.CheckBoxOutlineBlank
                    },
                    contentDescription = null,
                    tint = c.primary,
                    modifier = Modifier.size(dimens.iconSmall),
                )
                AppText(
                    // 路径用 outline：它是判断依据而不是动作本身，与前半句同行但不同重量。
                    text = buildAnnotatedString {
                        append("同时删除本地文件 ")
                        withStyle(SpanStyle(color = c.outline)) { append(path) }
                    },
                    style = AppTheme.typography.label.copy(fontWeight = FontWeight.Normal, lineHeight = 19.5.sp),
                    color = c.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = dimens.spaceXs),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceM, Alignment.End),
        ) {
            DialogAction("取消", c.primary) { onIntent(BookDetailIntent.DismissDialog) }
            DialogAction("移出书架", c.error) { onIntent(BookDetailIntent.ConfirmDialog) }
        }
    }
}

@Composable
private fun DialogAction(
    text: String,
    color: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(AppTheme.dimens.minTouchTarget)
            .clip(AppTheme.shapes.full)
            .clickable(onClick = onClick)
            .padding(horizontal = AppTheme.dimens.spaceContent),
        contentAlignment = Alignment.Center,
    ) {
        AppText(text = text, style = AppTheme.typography.label.copy(fontSize = 14.sp), color = color)
    }
}

private fun BookDetailEntryId.icon(): ImageVector = when (this) {
    BookDetailEntryId.Catalog -> Icons.AutoMirrored.Outlined.Toc
    BookDetailEntryId.Insights -> Icons.Outlined.Groups
}

private fun BookDetailMenuAction.icon(): ImageVector = when (this) {
    BookDetailMenuAction.MoveToGroup -> Icons.Outlined.Folder
    BookDetailMenuAction.ChangeCover -> Icons.Outlined.Image
    BookDetailMenuAction.EditInfo -> Icons.Outlined.Edit
    BookDetailMenuAction.Note -> Icons.AutoMirrored.Outlined.StickyNote2
    BookDetailMenuAction.EditVariables -> Icons.Outlined.DataObject
    BookDetailMenuAction.RemoveFromShelf -> Icons.Outlined.BookmarkRemove
    BookDetailMenuAction.DeleteLocalFile -> Icons.Outlined.DeleteForever
}

@Preview(name = "S-04 v2 书籍详情 · 日光", widthDp = 390, heightDp = 844)
@Composable
private fun BookDetailLightPreview() {
    ProvideAppTheme(dark = false) {
        BookDetailScreen(state = BookDetailPreviewState, onIntent = {})
    }
}

@Preview(name = "S-04 v2 书籍详情 · 夜墨", widthDp = 390, heightDp = 844)
@Composable
private fun BookDetailDarkPreview() {
    ProvideAppTheme(dark = true) {
        BookDetailScreen(state = BookDetailPreviewState, onIntent = {})
    }
}

@Preview(name = "S-04a v2 更多菜单", widthDp = 390, heightDp = 844)
@Composable
private fun BookDetailMenuPreview() {
    ProvideAppTheme(dark = false) {
        BookDetailScreen(
            state = BookDetailPreviewState.copy(menu = BookDetailPreviewMenu),
            onIntent = {},
        )
    }
}

@Preview(name = "S-04a v2 移出书架确认", widthDp = 390, heightDp = 844)
@Composable
private fun BookDetailRemoveDialogPreview() {
    ProvideAppTheme(dark = false) {
        BookDetailScreen(
            state = BookDetailPreviewState.copy(activeDialog = BookDetailPreviewRemoveDialog),
            onIntent = {},
        )
    }
}
