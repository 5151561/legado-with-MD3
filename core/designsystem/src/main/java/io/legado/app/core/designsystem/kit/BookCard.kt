package io.legado.app.core.designsystem.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.legado.app.core.designsystem.component.AppText
import io.legado.app.core.designsystem.theme.AppTheme

/**
 * 书籍卡片状态。
 *
 * 设计稿「已定方向」写明：均齐网格（1c）与在读优先混排（1d）两种模式
 * **共用同一套书籍卡片状态**，紧凑列表（B-03b）也是同一套。因此状态在此统一建模，
 * 三种布局共享，不各自定义。
 */
@Immutable
data class BookCardState(
    /** 未读更新数，0 表示不显示角标。 */
    val unreadCount: Int = 0,
    /** 已缓存。 */
    val isCached: Boolean = false,
    /** 来源失效。 */
    val isSourceInvalid: Boolean = false,
    /** 阅读进度 0f..1f；null 表示不显示进度条。 */
    val progress: Float? = null,
)

/** 封面比例 3:4，与设计稿网格（112×150）和紧凑列表（40×54）一致。 */
const val BookCoverAspectRatio = 3f / 4f

/** 封面上的蒙版与进度轨道压在图片之上，是固定不透明黑，不走语义色槽位。 */
private val CoverScrim = Color.Black.copy(alpha = 0.42f)
private val CoverProgressTrack = Color.Black.copy(alpha = 0.30f)

/**
 * 书籍封面，含四种状态覆盖层（画板 B-01）：
 *
 * | 状态 | 位置 | 规格 |
 * |---|---|---|
 * | 更新数 | 右上 8dp | 高 20dp、圆角全圆、error 底、白字 11sp |
 * | 来源失效 | 左上 8dp | 高 20dp、圆角 8dp、errorContainer 底、10sp |
 * | 已缓存 | 右下 8dp | 22dp 圆形、42% 黑色蒙版 |
 * | 进度 | 底边 | 厚 4dp、30% 黑色轨道、primaryContainer 指示 |
 *
 * 封面图由调用方经 [cover] 提供——设计系统不依赖图片加载库。无封面时用 [BookCoverPlaceholder]。
 */
@Composable
fun AppBookCover(
    state: BookCardState,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 10.dp,
    badgeSize: Dp = 20.dp,
    sourceInvalidLabel: String = "来源失效",
    cachedIcon: (@Composable () -> Unit)? = null,
    cover: @Composable BoxScope.() -> Unit,
) {
    val c = AppTheme.colorScheme
    Box(modifier = modifier.clip(RoundedCornerShape(cornerRadius))) {
        cover()

        if (state.isSourceInvalid) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .height(badgeSize)
                    .clip(AppTheme.shapes.small)
                    .background(c.errorContainer)
                    .padding(horizontal = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                AppText(
                    text = sourceInvalidLabel,
                    style = AppTheme.typography.micro.copy(fontSize = 10.sp),
                    color = c.onErrorContainer,
                )
            }
        }

        if (state.unreadCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .height(badgeSize)
                    .clip(AppTheme.shapes.full)
                    .background(c.error)
                    .padding(horizontal = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                AppText(
                    text = state.unreadCount.toString(),
                    style = AppTheme.typography.micro,
                    color = c.onError,
                )
            }
        }

        if (state.isCached && cachedIcon != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(22.dp)
                    .clip(AppTheme.shapes.full)
                    .background(CoverScrim),
                contentAlignment = Alignment.Center,
            ) { cachedIcon() }
        }

        val progress = state.progress
        if (progress != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(AppTheme.dimens.progressThickness)
                    .background(CoverProgressTrack),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(c.primaryContainer),
                )
            }
        }
    }
}

/** 无封面图时的占位：底色 + 底部书名（衬线 12sp），与设计稿的彩色占位封面一致。 */
@Composable
fun BookCoverPlaceholder(
    title: String,
    modifier: Modifier = Modifier,
    containerColor: Color = AppTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = AppTheme.colorScheme.onSurfaceVariant,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(containerColor)
            .padding(10.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        AppText(
            text = title,
            style = AppTheme.typography.coverTitle,
            color = contentColor,
            maxLines = 3,
        )
    }
}

/** 网格书籍项：封面 3:4 + 下方文字，间距 8dp（画板 B-01 模式 1c，3 列）。 */
@Composable
fun AppBookGridItem(
    title: String,
    state: BookCardState,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    cachedIcon: (@Composable () -> Unit)? = null,
    cover: @Composable BoxScope.() -> Unit,
) {
    val c = AppTheme.colorScheme
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.spaceM),
    ) {
        AppBookCover(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(BookCoverAspectRatio),
            cachedIcon = cachedIcon,
            cover = cover,
        )
        AppText(text = title, style = AppTheme.typography.label, color = c.onSurface, maxLines = 1)
        if (subtitle != null) {
            AppText(
                text = subtitle,
                style = AppTheme.typography.micro,
                color = c.outline,
                maxLines = 1,
            )
        }
    }
}

/**
 * 紧凑列表行：高 72dp、封面缩略 40×54dp（圆角 6dp）、右侧状态列（画板 B-03b）。
 *
 * 该布局只显示更新数与来源失效两种角标；缓存与进度改由 [trailing] 与 [subtitle]
 * 表达（设计稿此处用「读到 第四十七章 · 62%」+ 右侧下载完成图标），因此这里显式清零，
 * 避免同一事实在行内出现两次。
 */
@Composable
fun AppBookListRow(
    title: String,
    subtitle: String,
    state: BookCardState,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    cover: @Composable BoxScope.() -> Unit,
) {
    val c = AppTheme.colorScheme
    val dimens = AppTheme.dimens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(horizontal = dimens.spaceXl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceXxl),
    ) {
        AppBookCover(
            state = state.copy(progress = null, isCached = false),
            modifier = Modifier
                .width(40.dp)
                .height(54.dp),
            cornerRadius = 6.dp,
            badgeSize = 18.dp,
            cover = cover,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            AppText(
                text = title,
                style = AppTheme.typography.listTitle,
                color = c.onSurface,
                maxLines = 1,
            )
            AppText(
                text = subtitle,
                style = AppTheme.typography.caption,
                color = c.onSurfaceVariant,
                maxLines = 1,
            )
        }
        trailing?.invoke()
    }
}
