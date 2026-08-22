package io.legado.app.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.legado.app.core.designsystem.kit.AppGroupCard
import io.legado.app.core.designsystem.kit.AppIconSlot
import io.legado.app.core.designsystem.kit.AppSectionHeader
import io.legado.app.core.designsystem.kit.AppSettingRow
import io.legado.app.core.designsystem.kit.AppTopAppBar
import io.legado.app.core.designsystem.kit.AppTopAppBarDefaults
import io.legado.app.core.designsystem.theme.AppTheme
import io.legado.app.core.designsystem.theme.ProvideAppTheme
import kotlinx.collections.immutable.persistentListOf

/**
 * 设置主页（重设计画板 C-01）。
 *
 * 无状态：只接收 [state] 与 [onIntent]，不注入 ViewModel、不读取偏好。
 * 数据装配等 `settings:api` 扩面后由 ViewModel 提供（Phase 10 线 A）。
 */
@Composable
fun SettingsHomeScreen(
    state: SettingsHomeUiState,
    onIntent: (SettingsHomeIntent) -> Unit,
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
            title = "设置",
            titleStyle = AppTopAppBarDefaults.sectionTitleStyle,
            navigationIcon = {
                AppIconSlot(onClick = { onIntent(SettingsHomeIntent.Back) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "返回",
                        tint = c.onSurface,
                        modifier = Modifier.size(dimens.iconLarge),
                    )
                }
            },
            // 画板 C-01 顶栏右侧有搜索图标，但设计稿没有对应的设置搜索页，现有 App 也没有可接的实现。
            // 真正的设置搜索需要 settings:api 提供全量设置项索引（标题、所属页、跳转路径），
            // 只在这七个入口里过滤没有意义。索引就绪前先不呈现这个入口，避免出现点了没反应的控件。
            // 契约里保留 SettingsHomeIntent.OpenSearch，扩面时直接接上。
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // App 为 edge-to-edge：底部需让开系统导航栏，否则末行被手势条遮住。
            contentPadding = PaddingValues(
                start = dimens.spaceContent,
                end = dimens.spaceContent,
                top = dimens.spaceXs,
                bottom = dimens.spaceGroup +
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceL),
        ) {
            items(state.groups, key = { it.label }) { group ->
                Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceM)) {
                    AppSectionHeader(label = group.label, hint = group.hint)
                    AppGroupCard {
                        group.entries.forEachIndexed { index, entry ->
                            AppSettingRow(
                                title = entry.title,
                                summary = entry.summary,
                                showDivider = index > 0,
                                onClick = { onIntent(SettingsHomeIntent.OpenEntry(entry.id)) },
                                leading = {
                                    Icon(
                                        imageVector = entry.id.icon(),
                                        contentDescription = null,
                                        tint = c.primary,
                                        modifier = Modifier.size(dimens.iconMedium),
                                    )
                                },
                                trailing = {
                                    Icon(
                                        imageVector = Icons.Outlined.ChevronRight,
                                        contentDescription = null,
                                        tint = c.outline,
                                        modifier = Modifier.size(dimens.iconSmall),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 入口图标。放在 UI 层而非契约里——[SettingsEntryId] 是业务标识，
 * ViewModel 不应认识 [ImageVector]。
 */
private fun SettingsEntryId.icon(): ImageVector = when (this) {
    SettingsEntryId.ReadingAndLayout -> Icons.AutoMirrored.Outlined.MenuBook
    SettingsEntryId.ReadAloud -> Icons.Outlined.Headphones
    SettingsEntryId.ShelfAndCover -> Icons.Outlined.PhotoLibrary
    SettingsEntryId.BackupAndSync -> Icons.Outlined.Backup
    SettingsEntryId.WebService -> Icons.Outlined.Lan
    SettingsEntryId.AiAndTranslation -> Icons.Outlined.SmartToy
    SettingsEntryId.General -> Icons.Outlined.Tune
}

/** 画板 C-01 的原始数据，用于预览与对稿。 */
internal val SettingsHomePreviewState = SettingsHomeUiState(
    groups = persistentListOf(
        SettingsGroupUi(
            label = "阅读体验",
            hint = "改得最勤，放最上",
            entries = persistentListOf(
                SettingsEntryUi(
                    SettingsEntryId.ReadingAndLayout,
                    "阅读与排版",
                    "点击区域三分 · 音量键翻页 · 护眼 22:00",
                ),
                SettingsEntryUi(SettingsEntryId.ReadAloud, "朗读与听书", "云端·晓萱 · 1.2× · 按页朗读"),
                SettingsEntryUi(SettingsEntryId.ShelfAndCover, "书架与封面", "网格 3 列 · 仅 Wi-Fi 下载封面"),
            ),
        ),
        SettingsGroupUi(
            label = "数据与服务",
            hint = "会影响别处的开关",
            entries = persistentListOf(
                SettingsEntryUi(
                    SettingsEntryId.BackupAndSync,
                    "备份与同步",
                    "WebDAV · 今天 08:12 · 含阅读进度",
                ),
                SettingsEntryUi(SettingsEntryId.WebService, "Web 服务与设备", "运行中 · 端口 1122 · 局域网可见"),
            ),
        ),
        SettingsGroupUi(
            label = "系统",
            hint = "很少动",
            entries = persistentListOf(
                SettingsEntryUi(
                    SettingsEntryId.AiAndTranslation,
                    "AI 与翻译",
                    "3 个提供商 · 默认 gpt-4o-mini · 译至简体",
                ),
                SettingsEntryUi(SettingsEntryId.General, "通用", "语言、权限、日志、实验室 · v3.25.0 可更新"),
            ),
        ),
    ),
)

@Preview(name = "C-01 设置主页 · 日光", widthDp = 390, heightDp = 844)
@Composable
private fun SettingsHomeScreenLightPreview() {
    ProvideAppTheme(dark = false) {
        SettingsHomeScreen(state = SettingsHomePreviewState, onIntent = {})
    }
}

@Preview(name = "C-01 设置主页 · 夜墨", widthDp = 390, heightDp = 844)
@Composable
private fun SettingsHomeScreenDarkPreview() {
    ProvideAppTheme(dark = true) {
        SettingsHomeScreen(state = SettingsHomePreviewState, onIntent = {})
    }
}
