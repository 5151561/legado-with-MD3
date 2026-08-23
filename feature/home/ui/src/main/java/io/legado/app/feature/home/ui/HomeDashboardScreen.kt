package io.legado.app.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.legado.app.core.designsystem.kit.AppIconSlot
import io.legado.app.core.designsystem.kit.AppLinearProgress
import io.legado.app.core.designsystem.kit.AppText
import io.legado.app.core.designsystem.kit.AppTopAppBar
import io.legado.app.core.designsystem.kit.AppTopAppBarDefaults
import io.legado.app.core.designsystem.theme.AppTheme
import io.legado.app.core.designsystem.theme.ProvideAppTheme

/**
 * 首页（重设计画板 M-01 v2）。
 *
 * 无状态：只接收 [state] 与 [onIntent]，不注入 ViewModel、不读取偏好。
 *
 * 顶栏只有一个操作——「首页区块设置」（画板 M-01a）。备份区块没有备份/恢复/配置按钮，
 * 精选区块的所有点击都通往发现分类页；两条约束写在 [HomeDashboardUiState] 的说明里。
 *
 * @param bottomBar 一级导航栏由宿主提供。它属于 App 外壳而非首页本身，
 *   首页只负责把它排在内容之下，并让内容不被它遮住。
 * @param bookCover 封面图槽位。与设计系统 kit 同一立场：本模块不依赖图片加载库，
 *   由调用方按 [FeaturedBookUi.id] 提供绘制。缺省时留空，背后的占位底色仍在。
 */
@Composable
fun HomeDashboardScreen(
    state: HomeDashboardUiState,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {},
    bookCover: @Composable BoxScope.(bookId: String) -> Unit = {},
) {
    val c = AppTheme.colorScheme
    val dimens = AppTheme.dimens
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.surface),
    ) {
        AppTopAppBar(
            title = state.title,
            titleStyle = AppTopAppBarDefaults.displayTitleStyle,
            actions = {
                AppIconSlot(onClick = { onIntent(HomeIntent.OpenSectionSettings) }) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = "首页区块设置",
                        tint = c.onSurfaceVariant,
                        modifier = Modifier.size(dimens.iconLarge),
                    )
                }
            },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = dimens.spaceContent,
                    end = dimens.spaceContent,
                    top = dimens.spaceXs,
                    bottom = dimens.spaceXl,
                ),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceContent),
        ) {
            state.continueReading?.let {
                ContinueReadingCard(it, onIntent, bookCover)
            }
            state.readingGoal?.let { ReadingGoalCard(it, onIntent) }
            state.backupReminder?.let { BackupReminderCard(it, onIntent) }
            state.featured?.let { FeaturedSection(it, onIntent, bookCover) }
        }

        bottomBar()
    }
}

/**
 * 继续阅读卡：圆角 12dp、primaryContainer 底、内边距 16dp。
 * 进度条的轨道是主文字色的 16% —— 卡片本身已是 primaryContainer，
 * kit 的默认 surface 轨道在这上面看不见。
 */
@Composable
private fun ContinueReadingCard(
    ui: ContinueReadingUi,
    onIntent: (HomeIntent) -> Unit,
    bookCover: @Composable BoxScope.(bookId: String) -> Unit,
) {
    val c = AppTheme.colorScheme
    val dimens = AppTheme.dimens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.medium)
            .background(c.primaryContainer)
            .clickable { onIntent(HomeIntent.ContinueReading) }
            .padding(dimens.spaceContent),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceContent),
    ) {
        Box(
            modifier = Modifier
                .width(58.dp)
                .height(84.dp)
                .clip(AppTheme.shapes.small)
                .background(c.surfaceContainerHighest),
        ) { bookCover(ui.bookId) }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceS),
        ) {
            AppText(
                text = "继续阅读 · ${ui.bookName}",
                style = AppTheme.typography.caption.copy(lineHeight = 12.sp),
                color = c.onPrimaryContainer,
                maxLines = 1,
            )
            AppText(
                text = ui.chapterTitle,
                style = AppTheme.typography.bookTitleLarge.copy(fontSize = 17.sp, lineHeight = 22.1.sp),
                color = c.onPrimaryContainer,
                maxLines = 1,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceL),
            ) {
                AppLinearProgress(
                    progress = ui.progress,
                    modifier = Modifier.weight(1f),
                    trackColor = c.onPrimaryContainer.copy(alpha = 0.16f),
                    indicatorColor = c.primary,
                )
                AppText(
                    text = ui.progressLabel,
                    style = AppTheme.typography.caption.copy(lineHeight = 12.sp),
                    color = c.onPrimaryContainer,
                )
            }
        }

        Box(
            modifier = Modifier
                .size(dimens.buttonHeight)
                .clip(AppTheme.shapes.full)
                .background(c.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "继续阅读",
                tint = c.onPrimary,
                modifier = Modifier.size(dimens.iconLarge),
            )
        }
    }
}

/** 阅读目标与统计卡：圆角 12dp、最浅表面 + 1dp 描边，柱状图高 44dp、柱间距 5dp。 */
@Composable
private fun ReadingGoalCard(ui: ReadingGoalUi, onIntent: (HomeIntent) -> Unit) {
    val c = AppTheme.colorScheme
    val dimens = AppTheme.dimens
    val shape = AppTheme.shapes.medium
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surfaceContainerLowest)
            .border(dimens.divider, c.outlineVariant, shape)
            .padding(start = 18.dp, top = dimens.spaceContent, end = dimens.spaceM, bottom = dimens.spaceXl),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceXl),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AppText(
                text = ui.todayLabel,
                style = AppTheme.typography.label.copy(fontSize = 14.sp),
                color = c.onSurface,
            )
            TextAction(text = "阅读统计", onClick = { onIntent(HomeIntent.OpenReadingStats) })
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            ui.weekBars.forEachIndexed { index, value ->
                val today = index == ui.weekBars.lastIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(value.coerceIn(0f, 1f))
                        .clip(AppTheme.shapes.extraSmall)
                        .background(if (today) c.primary else c.secondaryContainer),
                )
            }
        }
        AppText(
            text = ui.summary,
            style = AppTheme.typography.micro.copy(lineHeight = 15.4.sp),
            color = c.outline,
            modifier = Modifier.padding(end = dimens.spaceL),
        )
    }
}

/**
 * 备份状态提醒（解 P3）。
 *
 * 只有「状态 + 一个去处」：一句超期结论、一句上次备份的时间与目标，外加一个跳转按钮。
 * 这里刻意没有备份 / 恢复 / 配置按钮——那一整套操作面唯一在画板 K-01。
 */
@Composable
private fun BackupReminderCard(ui: BackupReminderUi, onIntent: (HomeIntent) -> Unit) {
    val c = AppTheme.colorScheme
    val dimens = AppTheme.dimens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.medium)
            .background(c.tertiaryContainer)
            .padding(start = dimens.spaceContent, top = dimens.spaceXxl, end = dimens.spaceM, bottom = dimens.spaceXxl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceXl),
    ) {
        Icon(
            imageVector = Icons.Outlined.CloudOff,
            contentDescription = null,
            tint = c.onTertiaryContainer,
            modifier = Modifier.size(dimens.iconLarge),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceXxs),
        ) {
            AppText(
                text = ui.title,
                style = AppTheme.typography.listBody.copy(fontSize = 14.sp, lineHeight = 18.9.sp),
                color = c.onTertiaryContainer,
            )
            AppText(
                text = ui.detail,
                style = AppTheme.typography.caption.copy(lineHeight = 16.2.sp),
                color = c.onTertiaryContainer.copy(alpha = 0.8f),
            )
        }
        Box(
            modifier = Modifier
                .height(dimens.buttonHeight)
                .clip(AppTheme.shapes.full)
                .clickable { onIntent(HomeIntent.OpenBackup) }
                .padding(horizontal = dimens.spaceXl),
            contentAlignment = Alignment.Center,
        ) {
            AppText(
                text = ui.actionLabel,
                style = AppTheme.typography.label.copy(fontSize = 14.sp),
                color = c.onTertiaryContainer,
            )
        }
    }
}

/**
 * 精选发现模块（解 N8）。
 *
 * 每一格与「更多」都发同一个意图，通往发现分类页——首页不持有平行的浏览系统，
 * 因此这里没有翻页、没有分类切换、没有加载更多。
 */
@Composable
private fun FeaturedSection(
    ui: FeaturedSectionUi,
    onIntent: (HomeIntent) -> Unit,
    bookCover: @Composable BoxScope.(bookId: String) -> Unit,
) {
    val c = AppTheme.colorScheme
    val dimens = AppTheme.dimens
    Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceL)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = dimens.spaceXs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AppText(
                text = ui.title,
                style = AppTheme.typography.label.copy(fontSize = 14.sp),
                color = c.onSurface,
            )
            TextAction(text = "更多", onClick = { onIntent(HomeIntent.OpenFeatured(null)) })
        }
        // 稿面每格 82dp。四格加间距正好比 390dp 屏的内容区宽几个 dp，写死会溢出，
        // 因此改为等分——比例与稿面一致，且窄屏与大字体下不会被裁掉。
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceXl)) {
            ui.books.forEach { book ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = !book.loading) {
                            onIntent(HomeIntent.OpenFeatured(book.id))
                        },
                    verticalArrangement = Arrangement.spacedBy(dimens.spaceS),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(116.dp)
                            // 封面圆角 10dp，与 kit 的 AppBookCover 默认值一致（形状阶梯里没有这一档）。
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (book.loading) c.surfaceContainerHigh else c.surfaceContainerHighest),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (book.loading) {
                            Icon(
                                imageVector = Icons.Outlined.MoreHoriz,
                                contentDescription = null,
                                tint = c.outline,
                                modifier = Modifier.size(dimens.iconSmall),
                            )
                        } else {
                            bookCover(book.id)
                        }
                    }
                    AppText(
                        text = book.title,
                        style = AppTheme.typography.micro.copy(lineHeight = 14.3.sp),
                        color = if (book.loading) c.outline else c.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
        AppText(
            text = ui.footnote,
            style = AppTheme.typography.micro.copy(lineHeight = 16.5.sp),
            color = c.outline,
            modifier = Modifier.padding(horizontal = dimens.spaceXxs),
        )
    }
}

/**
 * 卡片右上角的文字动作（「阅读统计」「更多」）。
 * 视觉是 32dp 高的无底色药丸，触点由 48dp 最小高度补足。
 */
@Composable
private fun TextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = AppTheme.colorScheme.primary,
) {
    Box(
        modifier = modifier
            .height(AppTheme.dimens.minTouchTarget)
            .clip(AppTheme.shapes.full)
            .clickable(onClick = onClick)
            .padding(horizontal = AppTheme.dimens.spaceXl),
        contentAlignment = Alignment.Center,
    ) {
        AppText(text = text, style = AppTheme.typography.label.copy(fontSize = 14.sp), color = color)
    }
}

@Preview(name = "M-01 v2 首页 · 日光", widthDp = 390, heightDp = 844)
@Composable
private fun HomeDashboardLightPreview() {
    ProvideAppTheme(dark = false) {
        HomeDashboardScreen(
            state = HomeDashboardPreviewState,
            onIntent = {},
            bottomBar = { MainNavigationPreviewBar(selectedId = "home") },
        )
    }
}

@Preview(name = "M-01 v2 首页 · 夜墨", widthDp = 390, heightDp = 844)
@Composable
private fun HomeDashboardDarkPreview() {
    ProvideAppTheme(dark = true) {
        HomeDashboardScreen(
            state = HomeDashboardPreviewState,
            onIntent = {},
            bottomBar = { MainNavigationPreviewBar(selectedId = "home") },
        )
    }
}
