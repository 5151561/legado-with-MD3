package io.legado.app.feature.catalog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.BorderColor
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.legado.app.core.designsystem.kit.AppEntryGroupDivider
import io.legado.app.core.designsystem.kit.AppEntryGroupHeader
import io.legado.app.core.designsystem.kit.AppEntryRow
import io.legado.app.core.designsystem.kit.AppIconSlot
import io.legado.app.core.designsystem.kit.AppText
import io.legado.app.core.designsystem.kit.AppTopAppBar
import io.legado.app.core.designsystem.kit.AppTopAppBarDefaults
import io.legado.app.core.designsystem.theme.AppTheme
import io.legado.app.core.designsystem.theme.ProvideAppTheme

/**
 * 源与规则枢纽（重设计画板 D-00）。
 *
 * 无状态：只接收 [state] 与 [onIntent]，不注入 ViewModel、不读取偏好。
 *
 * 页面只做一件事——把九类源与规则摆成可比较的一张表。所有行同一种形态、同一种摘要口径
 * （数量 + 健康信号），所以这里没有任何按类特化的布局分支。
 */
@Composable
fun SourceHubScreen(
    state: SourceHubUiState,
    onIntent: (SourceHubIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = AppTheme.colorScheme
    val dimens = AppTheme.dimens
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.surface),
    ) {
        AppTopAppBar(
            title = "源与规则",
            titleStyle = AppTopAppBarDefaults.displayTitleStyle,
            navigationIcon = {
                AppIconSlot(onClick = { onIntent(SourceHubIntent.Back) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "返回",
                        tint = c.onSurfaceVariant,
                        modifier = Modifier.size(dimens.iconLarge),
                    )
                }
            },
            actions = {
                AppIconSlot(onClick = { onIntent(SourceHubIntent.Search) }) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "搜索源与规则",
                        tint = c.onSurfaceVariant,
                        modifier = Modifier.size(dimens.iconLarge),
                    )
                }
            },
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = dimens.spaceContent,
                end = dimens.spaceContent,
                top = dimens.spaceXs,
                bottom = dimens.spaceXl +
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
            ),
        ) {
            item("import") {
                ImportCard(
                    summary = state.importSummary,
                    onClick = { onIntent(SourceHubIntent.OpenImport) },
                    modifier = Modifier.padding(bottom = dimens.spaceXxl),
                )
            }
            state.groups.forEachIndexed { index, group ->
                item("header-${group.label}") {
                    Column {
                        if (index > 0) AppEntryGroupDivider()
                        AppEntryGroupHeader(group.label)
                    }
                }
                items(
                    count = group.entries.size,
                    key = { group.entries[it].id },
                ) { position ->
                    val entry = group.entries[position]
                    AppEntryRow(
                        title = entry.title,
                        summary = entry.summary,
                        // 规则组的行比内容来源矮 4dp（画板 D-00）：规则条目多，
                        // 一屏能不能看完九类是这一页成立的前提。
                        minHeight = if (index == 0) 56.dp else 52.dp,
                        onClick = { onIntent(SourceHubIntent.OpenEntry(entry.id)) },
                        leading = {
                            Icon(
                                imageVector = entry.id.icon(),
                                contentDescription = null,
                                tint = c.onSurfaceVariant,
                                modifier = Modifier.size(dimens.iconMedium),
                            )
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            tint = c.outlineVariant,
                            modifier = Modifier.size(dimens.iconSmall),
                        )
                    }
                }
            }
        }
    }
}

/**
 * 导入卡：全页唯一的写操作入口。
 *
 * 副标题列出四种来源与它们共同的去处，因为「导入」这个词本身不说明支持粘贴还是扫码，
 * 也不说明导入之后要不要审核。
 */
@Composable
private fun ImportCard(summary: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = AppTheme.colorScheme
    val dimens = AppTheme.dimens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.extraLarge)
            .background(c.primaryContainer)
            .clickable(onClick = onClick)
            .padding(dimens.spaceContent),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceXxl),
    ) {
        Icon(
            imageVector = Icons.Outlined.Download,
            contentDescription = null,
            tint = c.onPrimaryContainer,
            modifier = Modifier.size(dimens.iconLarge),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceXxs),
        ) {
            AppText(
                text = "导入",
                style = AppTheme.typography.listTitle,
                color = c.onPrimaryContainer,
            )
            AppText(
                text = summary,
                style = AppTheme.typography.micro.copy(lineHeight = 14.3.sp),
                color = c.onPrimaryContainer.copy(alpha = 0.85f),
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = c.onPrimaryContainer,
            modifier = Modifier.size(dimens.iconSmall),
        )
    }
}

/**
 * 入口图标。放在 UI 层而非契约里——[SourceHubEntryId] 是业务标识，
 * ViewModel 不应认识 [ImageVector]。
 */
private fun SourceHubEntryId.icon(): ImageVector = when (this) {
    SourceHubEntryId.BookSources -> Icons.Outlined.TravelExplore
    SourceHubEntryId.RssSources -> Icons.Outlined.RssFeed
    SourceHubEntryId.HttpTts -> Icons.Outlined.RecordVoiceOver
    SourceHubEntryId.ReplaceRules -> Icons.Outlined.CleaningServices
    SourceHubEntryId.TxtTocRules -> Icons.Outlined.FormatListNumbered
    SourceHubEntryId.DictRules -> Icons.Outlined.Translate
    SourceHubEntryId.ContentHighlight -> Icons.Outlined.BorderColor
    SourceHubEntryId.TagHighlight -> Icons.AutoMirrored.Outlined.Label
    SourceHubEntryId.RssRules -> Icons.Outlined.Subscriptions
}

@Preview(name = "D-00 源与规则枢纽 · 日光", widthDp = 390, heightDp = 844)
@Composable
private fun SourceHubLightPreview() {
    ProvideAppTheme(dark = false) {
        SourceHubScreen(state = SourceHubPreviewState, onIntent = {})
    }
}

@Preview(name = "D-00 源与规则枢纽 · 夜墨", widthDp = 390, heightDp = 844)
@Composable
private fun SourceHubDarkPreview() {
    ProvideAppTheme(dark = true) {
        SourceHubScreen(state = SourceHubPreviewState, onIntent = {})
    }
}
