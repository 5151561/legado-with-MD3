package io.legado.app.feature.ai.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.legado.app.core.designsystem.component.AppFeedback
import io.legado.app.core.designsystem.component.AppFeedbackState
import io.legado.app.core.designsystem.component.AppIconButton
import io.legado.app.core.designsystem.component.AppListItem
import io.legado.app.core.designsystem.component.AppScaffold
import io.legado.app.core.designsystem.component.AppTopBar
import org.koin.androidx.compose.koinViewModel

@Composable
fun AiRouteScreen(
    onBack: () -> Unit,
    onOpenChat: () -> Unit,
    onAddProvider: () -> Unit,
    onEditProvider: (String) -> Unit,
    onEditModel: (String, String) -> Unit,
    onOpenSummaryPrompt: () -> Unit,
    onOpenPromptSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AiViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AiEffect.Chat -> onOpenChat()
                AiEffect.AddProvider -> onAddProvider()
                is AiEffect.EditProvider -> onEditProvider(effect.providerId)
                is AiEffect.EditModel -> onEditModel(effect.providerId, effect.modelId)
                AiEffect.SummaryPrompt -> onOpenSummaryPrompt()
                AiEffect.PromptSettings -> onOpenPromptSettings()
                is AiEffect.Message -> snackbar.showSnackbar(effect.text)
            }
        }
    }
    AiScreen(state, viewModel::onIntent, onBack, snackbar, modifier)
}

@Composable
fun AiScreen(
    state: AiUiState,
    onIntent: (AiIntent) -> Unit,
    onBack: () -> Unit,
    snackbar: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    AppScaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = "AI",
                navigationIcon = {
                    AppIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    AppIconButton(onClick = { onIntent(AiIntent.AddProvider) }) {
                        Icon(Icons.Default.Add, contentDescription = "添加供应商")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) {
        Box(Modifier.fillMaxSize()) {
            when {
                state.loading -> AppFeedback(AppFeedbackState.Loading, "正在读取 AI 配置", Modifier.align(Alignment.Center))
                state.loadFailed -> AppFeedback(AppFeedbackState.Error, "AI 配置读取失败", Modifier.align(Alignment.Center), "重试") { onIntent(AiIntent.Retry) }
                else -> Column(Modifier.fillMaxSize()) {
                    AppListItem(
                        headlineContent = { Text("${state.providerCount} 个供应商 · ${state.models.size} 个模型") },
                        supportingContent = { Text("${state.presetCount} 个任务预设") },
                    )
                    Row(Modifier.fillMaxWidth()) {
                        Button(onClick = { onIntent(AiIntent.OpenChat) }, modifier = Modifier.weight(1f)) { Text("打开会话") }
                        Button(onClick = { onIntent(AiIntent.OpenSummaryPrompt) }, modifier = Modifier.weight(1f)) { Text("摘要") }
                        Button(onClick = { onIntent(AiIntent.OpenPromptSettings) }, modifier = Modifier.weight(1f)) { Text("提示词") }
                    }
                    if (state.models.isEmpty()) {
                        AppFeedback(AppFeedbackState.Empty, "还没有 AI 模型", Modifier.align(Alignment.CenterHorizontally), "添加供应商") { onIntent(AiIntent.AddProvider) }
                    } else {
                        LazyColumn {
                            items(state.models, key = { it.id }) { model ->
                                AppListItem(
                                    headlineContent = { Text(model.name) },
                                    supportingContent = { Text("${model.providerName} · ${model.modelId}") },
                                    leadingContent = {
                                        RadioButton(
                                            selected = model.isDefault,
                                            onClick = { if (!state.commandInFlight) onIntent(AiIntent.SetDefaultModel(model.id)) },
                                            enabled = model.enabled && !state.commandInFlight,
                                        )
                                    },
                                    modifier = Modifier.clickable {
                                        onIntent(AiIntent.EditModel(model.providerId, model.id))
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
