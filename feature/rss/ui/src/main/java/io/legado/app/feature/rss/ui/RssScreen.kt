package io.legado.app.feature.rss.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.legado.app.core.designsystem.component.AppFeedback
import io.legado.app.core.designsystem.component.AppFeedbackState
import io.legado.app.core.designsystem.component.AppIcon
import io.legado.app.core.designsystem.component.AppIconButton
import io.legado.app.core.designsystem.component.AppListItem
import io.legado.app.core.designsystem.component.AppScaffold
import io.legado.app.core.designsystem.component.AppTopBar
import io.legado.app.core.designsystem.theme.LegadoTheme
import io.legado.app.feature.rss.api.RssOpenTarget
import org.koin.androidx.compose.koinViewModel

@Composable
fun RssRouteScreen(
    onOpen: (RssOpenTarget) -> Unit,
    onLogin: (String) -> Unit,
    onEdit: (String) -> Unit,
    onFavorites: () -> Unit,
    onManage: () -> Unit,
    onRuleSubscriptions: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RssViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is RssEffect.Open -> onOpen(effect.target)
                is RssEffect.Login -> onLogin(effect.sourceId)
                is RssEffect.Edit -> onEdit(effect.sourceId)
                RssEffect.Favorites -> onFavorites()
                RssEffect.Manage -> onManage()
                RssEffect.RuleSubscriptions -> onRuleSubscriptions()
                is RssEffect.Message -> snackbar.showSnackbar(effect.text)
            }
        }
    }
    RssScreen(state, viewModel::onIntent, snackbar, modifier)
}

@Composable
fun RssScreen(
    state: RssUiState,
    onIntent: (RssIntent) -> Unit,
    snackbar: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    AppScaffold(
        modifier = modifier,
        topBar = { RssTopBar(onIntent) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) {
        Column(Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = state.query,
                onValueChange = { onIntent(RssIntent.Search(it)) },
                modifier = Modifier.fillMaxWidth().padding(LegadoTheme.spacing.medium),
                label = { Text("搜索 RSS") },
                singleLine = true,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(LegadoTheme.spacing.small)) {
                item {
                    FilterChip(
                        selected = state.selectedGroup.isEmpty(),
                        onClick = { onIntent(RssIntent.SelectGroup("")) },
                        label = { Text("全部") },
                    )
                }
                items(state.groups) { group ->
                    FilterChip(
                        selected = state.selectedGroup == group,
                        onClick = { onIntent(RssIntent.SelectGroup(group)) },
                        label = { Text(group) },
                    )
                }
            }
            Box(Modifier.fillMaxSize()) {
                when {
                    state.loading -> AppFeedback(
                        AppFeedbackState.Loading,
                        "正在读取 RSS",
                        Modifier.align(Alignment.Center),
                    )
                    state.loadFailed -> AppFeedback(
                        AppFeedbackState.Error,
                        "RSS 读取失败",
                        Modifier.align(Alignment.Center),
                        "重试",
                    ) { onIntent(RssIntent.Retry) }
                    state.sources.isEmpty() -> AppFeedback(
                        AppFeedbackState.Empty,
                        "没有启用的 RSS 源",
                        Modifier.align(Alignment.Center),
                    )
                    else -> LazyColumn {
                        items(state.sources, key = { it.id }) { source ->
                            RssSourceRow(source, state.commandInFlight, onIntent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RssTopBar(onIntent: (RssIntent) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    AppTopBar(title = "RSS", actions = {
        Box {
            AppIconButton(onClick = { expanded = true }) { AppIcon(Icons.Default.MoreVert, "更多") }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem({ Text("收藏") }, { expanded = false; onIntent(RssIntent.Favorites) })
                DropdownMenuItem({ Text("订阅规则") }, { expanded = false; onIntent(RssIntent.RuleSubscriptions) })
                DropdownMenuItem({ Text("管理 RSS 源") }, { expanded = false; onIntent(RssIntent.Manage) })
            }
        }
    })
}

@Composable
private fun RssSourceRow(
    source: RssSourceUi,
    disabled: Boolean,
    onIntent: (RssIntent) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    AppListItem(
        headlineContent = { Text(source.name) },
        supportingContent = { Text(source.group ?: "未分组") },
        leadingContent = {
            if (!source.icon.isNullOrBlank()) AsyncImage(source.icon, contentDescription = null)
        },
        modifier = Modifier.fillMaxWidth().clickable(enabled = !disabled) {
            onIntent(RssIntent.Open(source.id))
        },
        trailingContent = {
            Box {
                AppIconButton(onClick = { expanded = true }) {
                    AppIcon(Icons.Default.MoreVert, "RSS 操作")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    if (source.hasLogin) {
                        DropdownMenuItem({ Text("登录") }, { expanded = false; onIntent(RssIntent.Login(source.id)) })
                    }
                    DropdownMenuItem({ Text("编辑") }, { expanded = false; onIntent(RssIntent.Edit(source.id)) })
                    DropdownMenuItem({ Text("置顶") }, { expanded = false; if (!disabled) onIntent(RssIntent.Pin(source.id)) })
                    DropdownMenuItem({ Text("停用") }, { expanded = false; if (!disabled) onIntent(RssIntent.Disable(source.id)) })
                    DropdownMenuItem({ Text("删除") }, { expanded = false; if (!disabled) onIntent(RssIntent.Delete(source.id)) })
                }
            }
        },
    )
}
