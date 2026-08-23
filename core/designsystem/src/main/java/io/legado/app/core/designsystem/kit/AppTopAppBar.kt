package io.legado.app.core.designsystem.kit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.legado.app.core.designsystem.theme.AppTheme

/**
 * 顶栏。高 56dp，左右内边距 8dp，前导与操作槽位均为 48dp 方形触点。
 *
 * 设计稿的标题有三档，见 [AppTopAppBarDefaults]。带 [subtitle] 时标题降到 19sp、
 * 下方补一行 11sp 的对象说明（画板 S-06b：动作「选择章节」在上，对象「雪落长安 · 386 章」在下）——
 * 选择态一类页面必须同时说清在做什么和对谁做。
 *
 * App 为 edge-to-edge，内容会绘制到系统状态栏之下，因此顶栏自己消费
 * [windowInsets]（默认状态栏），把 56dp 栏体推到安全区内——与 Material 3 `TopAppBar`
 * 的约定一致。画板里那条 44dp 状态栏只是稿面模拟，真机高度必须来自 `WindowInsets`，
 * 不可写成固定值。
 */
@Composable
fun AppTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleStyle: TextStyle = AppTopAppBarDefaults.compactTitleStyle,
    windowInsets: WindowInsets = WindowInsets.statusBars,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val dimens = AppTheme.dimens
    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(windowInsets),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimens.topBarHeight)
                .padding(horizontal = dimens.spaceM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (navigationIcon != null) {
                AppIconSlot(content = navigationIcon)
            } else {
                Box(Modifier.size(dimens.spaceM))
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = dimens.spaceS),
                verticalArrangement = Arrangement.spacedBy(dimens.spaceXxs),
            ) {
                AppText(
                    text = title,
                    style = if (subtitle == null) titleStyle else AppTopAppBarDefaults.actionTitleStyle,
                    color = AppTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                if (subtitle != null) {
                    AppText(
                        text = subtitle,
                        style = AppTheme.typography.micro.copy(lineHeight = 13.2.sp),
                        color = AppTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            actions()
        }
    }
}

object AppTopAppBarDefaults {
    /**
     * 主界面的衬线大标题，22sp / 字重 400（画板 M-01「今天」、B-01 书架）。
     */
    val displayTitleStyle: TextStyle
        @Composable get() = AppTheme.typography.bookTitleLarge.copy(
            fontWeight = FontWeight.Normal,
        )

    /**
     * 二级页的衬线标题，20sp / 字重 400（画板 C-01「设置」）。
     * 设计稿的顶栏衬线标题有 22 与 20 两档，不可混用。
     */
    val sectionTitleStyle: TextStyle
        @Composable get() = AppTheme.typography.chapterTitle.copy(
            fontWeight = FontWeight.Normal,
        )

    /** 子页的无衬线标题，15sp / 字重 500（画板 B-03b 一类）。 */
    val compactTitleStyle: TextStyle
        @Composable get() = AppTheme.typography.listTitle

    /**
     * 带副标题时的衬线标题，19sp / 字重 400（画板 S-06b「选择章节」）。
     * 比 [sectionTitleStyle] 小一档，给副标题让出高度。
     */
    val actionTitleStyle: TextStyle
        @Composable get() = AppTheme.typography.chapterTitle.copy(
            fontSize = 19.sp,
            lineHeight = 22.8.sp,
            fontWeight = FontWeight.Normal,
        )
}

/**
 * 48dp 方形触点，用于顶栏前导 / 操作图标。
 * 满足设计规则「所有可点区域 ≥48 dp」。
 */
@Composable
fun AppIconSlot(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(AppTheme.dimens.iconButton)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) { content() }
}
