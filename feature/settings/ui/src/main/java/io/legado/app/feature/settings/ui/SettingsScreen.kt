package io.legado.app.feature.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.legado.app.core.designsystem.component.AppFeedback
import io.legado.app.core.designsystem.component.AppFeedbackState
import io.legado.app.core.designsystem.component.AppIconButton
import io.legado.app.core.designsystem.component.AppListItem
import io.legado.app.core.designsystem.component.AppScaffold
import io.legado.app.core.designsystem.component.AppTopBar
import kotlinx.coroutines.flow.Flow
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsRouteScreen(
    onBack: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenInterface: () -> Unit,
    onOpenDownloadCache: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenRead: () -> Unit,
    onOpenCover: () -> Unit,
    onOpenAi: () -> Unit,
    onOpenTranslation: () -> Unit,
    onOpenLab: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsEffectHost(
        effects = viewModel.effects,
        onOpenTheme = onOpenTheme,
        onOpenInterface = onOpenInterface,
        onOpenDownloadCache = onOpenDownloadCache,
        onOpenBackup = onOpenBackup,
        onOpenRead = onOpenRead,
        onOpenCover = onOpenCover,
        onOpenAi = onOpenAi,
        onOpenTranslation = onOpenTranslation,
        onOpenLab = onOpenLab,
    )
    SettingsScreen(state, viewModel::onIntent, onBack, modifier)
}

@Composable
private fun SettingsEffectHost(
    effects: Flow<SettingsEffect>,
    onOpenTheme: () -> Unit,
    onOpenInterface: () -> Unit,
    onOpenDownloadCache: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenRead: () -> Unit,
    onOpenCover: () -> Unit,
    onOpenAi: () -> Unit,
    onOpenTranslation: () -> Unit,
    onOpenLab: () -> Unit,
) {
    LaunchedEffect(effects) {
        effects.collect { effect ->
            when (effect) {
                SettingsEffect.Theme -> onOpenTheme()
                SettingsEffect.Interface -> onOpenInterface()
                SettingsEffect.DownloadCache -> onOpenDownloadCache()
                SettingsEffect.Backup -> onOpenBackup()
                SettingsEffect.Read -> onOpenRead()
                SettingsEffect.Cover -> onOpenCover()
                SettingsEffect.Ai -> onOpenAi()
                SettingsEffect.Translation -> onOpenTranslation()
                SettingsEffect.Lab -> onOpenLab()
            }
        }
    }
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppScaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = "设置",
                navigationIcon = {
                    AppIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) {
        Box(Modifier.fillMaxSize()) {
            when {
                state.loading -> AppFeedback(
                    AppFeedbackState.Loading,
                    "正在读取设置",
                    Modifier.align(Alignment.Center),
                )
                state.loadFailed -> AppFeedback(
                    AppFeedbackState.Error,
                    "设置读取失败",
                    Modifier.align(Alignment.Center),
                    actionText = "重试",
                    onAction = { onIntent(SettingsIntent.Retry) },
                )
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    item { SettingRow("主题与外观", "模式 ${state.themeMode} · 字号 ${state.fontScale}") { onIntent(SettingsIntent.OpenTheme) } }
                    item { SettingRow("界面与通用", "导航、语言和应用行为") { onIntent(SettingsIntent.OpenInterface) } }
                    item { SettingRow("阅读设置", "排版、翻页和朗读入口") { onIntent(SettingsIntent.OpenRead) } }
                    item { SettingRow("封面设置", "封面显示与相册") { onIntent(SettingsIntent.OpenCover) } }
                    item { SettingRow("下载与缓存", "图片缓存 ${state.bitmapCacheSizeMb} MB · ${state.downloadThreadCount} 线程") { onIntent(SettingsIntent.OpenDownloadCache) } }
                    item { SettingRow("备份与恢复", if (state.backupConfigured) "已配置 WebDAV" else "尚未配置 WebDAV") { onIntent(SettingsIntent.OpenBackup) } }
                    item { SettingRow("AI", "模型、会话和生成能力") { onIntent(SettingsIntent.OpenAi) } }
                    item { SettingRow("翻译", "章节翻译与缓存") { onIntent(SettingsIntent.OpenTranslation) } }
                    item { SettingRow("实验室", "实验功能") { onIntent(SettingsIntent.OpenLab) } }
                }
            }
        }
    }
}

@Composable
private fun SettingRow(title: String, summary: String, onClick: () -> Unit) {
    AppListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
