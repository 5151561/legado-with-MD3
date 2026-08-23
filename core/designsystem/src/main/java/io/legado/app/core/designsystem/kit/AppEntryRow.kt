package io.legado.app.core.designsystem.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.legado.app.core.designsystem.theme.AppTheme

/**
 * 平铺入口行（画板 P-01「我的」、D-00「源与规则枢纽」）。
 *
 * 与 [AppSettingRow] 是设计稿里并存的两种行：
 *
 * | | [AppSettingRow]（画板 C-01） | [AppEntryRow]（画板 P-01 / D-00） |
 * |---|---|---|
 * | 容器 | 18dp 圆角分组卡 | 无卡，直接铺在页面上 |
 * | 前导 | 40dp 圆底图标 | 22dp 裸图标 |
 * | 断组 | 卡与卡之间留白 | 一条 1dp 分隔线 |
 *
 * 一次配置很久不动的入口用前者，每天要动的用后者——这与设计规则「设置路由重排」
 * 把入口分到 C-01 与「我的」两处是同一条线。
 *
 * 规格：最小高 56dp（画板 D-00 的「规则」组为 52dp）、内边距 4/12/8、槽位间距 16dp，
 * 标题 15sp/400，摘要 11sp。
 *
 * @param summaryColor 摘要色。缺省 outline；表示「正在生效的运行态」时传 primary
 *   （画板 P-01 的 Web 服务）。
 */
@Composable
fun AppEntryRow(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    summaryColor: Color = AppTheme.colorScheme.outline,
    titleColor: Color = AppTheme.colorScheme.onSurface,
    minHeight: Dp = 56.dp,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val dimens = AppTheme.dimens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .defaultMinSize(minHeight = minHeight)
            .padding(
                start = dimens.spaceXs,
                end = dimens.spaceXl,
                top = dimens.spaceM,
                bottom = dimens.spaceM,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceContent),
    ) {
        leading?.invoke()
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceXxs),
        ) {
            AppText(
                text = title,
                style = AppTheme.typography.listBody.copy(lineHeight = 19.5.sp),
                color = titleColor,
            )
            if (summary != null) {
                AppText(
                    text = summary,
                    style = AppTheme.typography.micro.copy(lineHeight = 14.3.sp),
                    color = summaryColor,
                    maxLines = 1,
                )
            }
        }
        trailing()
    }
}

/**
 * 平铺分组的小标题（画板 P-01 / D-00）：14sp / 字重 500 / primary。
 *
 * 与 [AppSectionHeader] 的区别是它用强调色、字号更大——平铺列表没有卡片边界，
 * 断组只能靠标题的重量与一条分隔线。
 */
@Composable
fun AppEntryGroupHeader(text: String, modifier: Modifier = Modifier) {
    AppText(
        text = text,
        style = AppTheme.typography.label.copy(fontSize = 14.sp),
        color = AppTheme.colorScheme.primary,
        modifier = modifier.padding(
            start = AppTheme.dimens.spaceXs,
            top = AppTheme.dimens.spaceL,
            bottom = AppTheme.dimens.spaceM,
        ),
    )
}

/** 平铺分组之间的分隔线：1dp outlineVariant，上下留 6dp。 */
@Composable
fun AppEntryGroupDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .padding(vertical = AppTheme.dimens.spaceS)
            .fillMaxWidth()
            .height(AppTheme.dimens.divider)
            .background(AppTheme.colorScheme.outlineVariant),
    )
}
