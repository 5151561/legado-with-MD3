package io.legado.app.feature.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 设置主页（画板 C-01）的路由层。
 *
 * **当前是可视化预览接线，不是最终形态。**
 * `settings:api` 只有 16 行，尚未表达任何设置摘要，因此各入口的 summary 暂用画板原始示例文案
 * （见 [SettingsHomePreviewState]），**不反映用户的真实设置**。
 * 待 Phase 10 线 A 完成 `settings:api` 扩面后，改由 ViewModel 从 SSOT 提供真实摘要。
 *
 * 入口映射：设计稿的七个入口中，「朗读与听书」与「Web 服务与设备」在现有导航里
 * 没有对应目的地——这两处是重设计新引入的界面（对应画板 W-01 等），
 * 相应回调默认为空实现，点击无反应。
 */
@Composable
fun SettingsHomeRouteScreen(
    onBack: () -> Unit,
    onOpenRead: () -> Unit,
    onOpenCover: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenAi: () -> Unit,
    onOpenGeneral: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenReadAloud: () -> Unit = {},
    onOpenWebService: () -> Unit = {},
) {
    SettingsHomeScreen(
        state = SettingsHomePreviewState,
        onIntent = { intent ->
            when (intent) {
                is SettingsHomeIntent.Back -> onBack()
                // 搜索入口当前未呈现，见 SettingsHomeScreen 中的说明。
                is SettingsHomeIntent.OpenSearch -> Unit
                is SettingsHomeIntent.OpenEntry -> when (intent.id) {
                    SettingsEntryId.ReadingAndLayout -> onOpenRead()
                    SettingsEntryId.ReadAloud -> onOpenReadAloud()
                    SettingsEntryId.ShelfAndCover -> onOpenCover()
                    SettingsEntryId.BackupAndSync -> onOpenBackup()
                    SettingsEntryId.WebService -> onOpenWebService()
                    SettingsEntryId.AiAndTranslation -> onOpenAi()
                    SettingsEntryId.General -> onOpenGeneral()
                }
            }
        },
        modifier = modifier,
    )
}
