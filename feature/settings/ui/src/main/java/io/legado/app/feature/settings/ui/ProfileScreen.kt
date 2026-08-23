package io.legado.app.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RuleFolder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.legado.app.core.designsystem.kit.AppEntryGroupDivider
import io.legado.app.core.designsystem.kit.AppEntryGroupHeader
import io.legado.app.core.designsystem.kit.AppEntryRow
import io.legado.app.core.designsystem.kit.AppSegmentedControl
import io.legado.app.core.designsystem.kit.AppSwitch
import io.legado.app.core.designsystem.kit.AppText
import io.legado.app.core.designsystem.kit.AppTopAppBar
import io.legado.app.core.designsystem.kit.AppTopAppBarDefaults
import io.legado.app.core.designsystem.kit.SegmentedOption
import io.legado.app.core.designsystem.theme.AppTheme
import io.legado.app.core.designsystem.theme.ProvideAppTheme
import kotlinx.collections.immutable.persistentListOf

/**
 * 「我的」（重设计画板 P-01 v2）。
 *
 * 无状态：只接收 [state] 与 [onIntent]，不注入 ViewModel、不读取偏好。
 *
 * 行样式与设置主页（画板 C-01）不同：这里是不带分组卡的平铺行、22dp 裸图标，
 * 靠一条 1dp 分隔线断组。两种行样式在设计稿里都存在，且各自只用于一处，
 * 因此本页的行留在本文件内，不下沉进 kit。
 *
 * @param bottomBar 一级导航栏由宿主提供，理由同首页（画板 M-01）。
 */
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onIntent: (ProfileIntent) -> Unit,
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {},
) {
    val c = AppTheme.colorScheme
    val dimens = AppTheme.dimens
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.surface),
    ) {
        AppTopAppBar(title = "我的", titleStyle = AppTopAppBarDefaults.displayTitleStyle)

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
            verticalArrangement = Arrangement.spacedBy(dimens.spaceXxl),
        ) {
            AppearanceSection(state, onIntent)

            state.groups.forEachIndexed { index, group ->
                Column(Modifier.fillMaxWidth()) {
                    if (index > 0) AppEntryGroupDivider()
                    AppEntryGroupHeader(group.label)
                    group.entries.forEach { entry ->
                        ProfileRow(entry = entry, onIntent = onIntent)
                    }
                }
            }
        }

        bottomBar()
    }
}

/**
 * 外观区：日光 / 夜墨 / 跟随三选一，加一条通往完整主题页的行。
 *
 * 这一区没有分组标题——它是「我的」的第一屏内容，标题会把最常用的开关推下去。
 */
@Composable
private fun AppearanceSection(state: ProfileUiState, onIntent: (ProfileIntent) -> Unit) {
    val c = AppTheme.colorScheme
    val dimens = AppTheme.dimens
    val options = persistentListOf(
        SegmentedOption(ProfileThemeMode.Light.name, "日光"),
        SegmentedOption(ProfileThemeMode.Dark.name, "夜墨"),
        SegmentedOption(ProfileThemeMode.System.name, "跟随"),
    )
    Column(
        modifier = Modifier.padding(start = dimens.spaceXs, end = dimens.spaceXs, top = dimens.spaceXs),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceXl),
    ) {
        AppSegmentedControl(
            options = options,
            selectedId = state.themeMode.name,
            onSelect = { onIntent(ProfileIntent.SelectThemeMode(ProfileThemeMode.valueOf(it))) },
            icon = { option ->
                val selected = option.id == state.themeMode.name
                Icon(
                    // 选中段用勾表达「已生效」，未选中段保留各自的语义图标。
                    imageVector = if (selected) Icons.Outlined.Check else option.icon(),
                    contentDescription = null,
                    tint = if (selected) c.onSecondaryContainer else c.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            },
        )
        ProfileRow(
            entry = state.themeEntry,
            onIntent = onIntent,
            // 强调色色块：主题页当前种子色的所见即所得，放在箭头之前。
            extraTrailing = {
                Box(
                    Modifier
                        .size(dimens.iconSmall)
                        .clip(AppTheme.shapes.full)
                        .background(c.primary),
                )
            },
        )
    }
}

private fun SegmentedOption.icon(): ImageVector = when (id) {
    ProfileThemeMode.Dark.name -> Icons.Outlined.DarkMode
    else -> Icons.Outlined.BrightnessAuto
}

/** 本页的行 = kit 的平铺入口行 + 「我的」自己的尾随形态。 */
@Composable
private fun ProfileRow(
    entry: ProfileEntryUi,
    onIntent: (ProfileIntent) -> Unit,
    modifier: Modifier = Modifier,
    extraTrailing: (@Composable () -> Unit)? = null,
) {
    val c = AppTheme.colorScheme
    val dimens = AppTheme.dimens
    AppEntryRow(
        title = entry.title,
        modifier = modifier,
        summary = entry.summary,
        summaryColor = if (entry.summaryAccent) c.primary else c.outline,
        onClick = { onIntent(ProfileIntent.OpenEntry(entry.id)) },
        leading = {
            Icon(
                imageVector = entry.id.icon(),
                contentDescription = null,
                tint = c.onSurfaceVariant,
                modifier = Modifier.size(dimens.iconMedium),
            )
        },
    ) {
        extraTrailing?.invoke()
        when (val trailing = entry.trailing) {
            is ProfileTrailing.Chevron -> Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = c.outlineVariant,
                modifier = Modifier.size(dimens.iconSmall),
            )

            is ProfileTrailing.Toggle -> AppSwitch(
                checked = trailing.checked,
                onCheckedChange = { onIntent(ProfileIntent.SetToggle(entry.id, it)) },
            ) { tint ->
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(16.dp),
                )
            }

            is ProfileTrailing.Badge -> Box(
                modifier = Modifier
                    .height(22.dp)
                    .clip(AppTheme.shapes.full)
                    .background(c.error)
                    .padding(horizontal = dimens.spaceM),
                contentAlignment = Alignment.Center,
            ) {
                AppText(
                    text = trailing.label,
                    style = AppTheme.typography.micro.copy(fontWeight = FontWeight.Medium),
                    color = c.onError,
                )
            }
        }
    }
}

/**
 * 入口图标。放在 UI 层而非契约里——[ProfileEntryId] 是业务标识，
 * ViewModel 不应认识 [ImageVector]。
 */
private fun ProfileEntryId.icon(): ImageVector = when (this) {
    ProfileEntryId.Theme -> Icons.Outlined.Palette
    ProfileEntryId.BookmarksAndNotes -> Icons.Outlined.Bookmark
    ProfileEntryId.ReadingRecords -> Icons.Outlined.History
    ProfileEntryId.AiChat -> Icons.Outlined.Forum
    ProfileEntryId.SourcesAndRules -> Icons.Outlined.RuleFolder
    ProfileEntryId.WebService -> Icons.Outlined.WifiTethering
    ProfileEntryId.Settings -> Icons.Outlined.Settings
    ProfileEntryId.About -> Icons.Outlined.Info
}

// 预览与截图基线不带一级导航栏：栏体属于 App 外壳，画板 M-01 的基线已经在盯它的渲染，
// 这里再摆一份只会让两处对稿装置各自漂移。
@Preview(name = "P-01 v2 我的 · 日光", widthDp = 390, heightDp = 844)
@Composable
private fun ProfileScreenLightPreview() {
    ProvideAppTheme(dark = false) {
        ProfileScreen(state = ProfilePreviewState, onIntent = {})
    }
}

@Preview(name = "P-01 v2 我的 · 夜墨", widthDp = 390, heightDp = 844)
@Composable
private fun ProfileScreenDarkPreview() {
    ProvideAppTheme(dark = true) {
        ProfileScreen(state = ProfilePreviewState, onIntent = {})
    }
}
