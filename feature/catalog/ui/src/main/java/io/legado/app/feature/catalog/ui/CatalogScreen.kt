package io.legado.app.feature.catalog.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import io.legado.app.core.designsystem.component.AppFeedback
import io.legado.app.core.designsystem.component.AppFeedbackState
import io.legado.app.core.designsystem.component.AppIcon
import io.legado.app.core.designsystem.component.AppIconButton
import io.legado.app.core.designsystem.component.AppListItem
import io.legado.app.core.designsystem.component.AppScaffold
import io.legado.app.core.designsystem.component.AppTopBar
import io.legado.app.core.designsystem.theme.LegadoTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun CatalogRouteScreen(
    onOpenDiscovery: (String, String) -> Unit,
    onSearchSource: (String, String) -> Unit,
    onLogin: (String) -> Unit,
    onEdit: (String) -> Unit,
    onGlobalSearch: () -> Unit,
    onSourceManage: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CatalogViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CatalogEffect.OpenDiscovery -> onOpenDiscovery(effect.title, effect.sourceId)
                is CatalogEffect.SearchSource -> onSearchSource(effect.name, effect.sourceId)
                is CatalogEffect.Login -> onLogin(effect.sourceId)
                is CatalogEffect.Edit -> onEdit(effect.sourceId)
                CatalogEffect.GlobalSearch -> onGlobalSearch()
                CatalogEffect.SourceManage -> onSourceManage()
                CatalogEffect.Import -> onImport()
                is CatalogEffect.Message -> snackbar.showSnackbar(effect.text)
            }
        }
    }
    CatalogScreen(state, viewModel::onIntent, snackbar, modifier)
}

@Composable
fun CatalogScreen(
    state: CatalogUiState,
    onIntent: (CatalogIntent) -> Unit,
    snackbar: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    AppScaffold(
        modifier = modifier,
        topBar = { CatalogTopBar(onIntent) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) {
        Column(Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = state.query,
                onValueChange = { onIntent(CatalogIntent.Search(it)) },
                label = { Text("搜索书源") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(LegadoTheme.spacing.medium),
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(LegadoTheme.spacing.small)) {
                item { FilterChip(selected = state.selectedGroup.isEmpty(), onClick = { onIntent(CatalogIntent.SelectGroup("")) }, label = { Text("全部") }) }
                items(state.groups) { group -> FilterChip(selected = state.selectedGroup == group, onClick = { onIntent(CatalogIntent.SelectGroup(group)) }, label = { Text(group) }) }
            }
            Box(Modifier.fillMaxSize()) {
                when {
                    state.loading -> AppFeedback(AppFeedbackState.Loading, "正在读取书源", Modifier.align(Alignment.Center))
                    state.loadFailed -> AppFeedback(AppFeedbackState.Error, "书源读取失败", Modifier.align(Alignment.Center), "重试") { onIntent(CatalogIntent.Retry) }
                    state.sources.isEmpty() -> AppFeedback(AppFeedbackState.Empty, "没有可用书源", Modifier.align(Alignment.Center))
                    else -> LazyColumn(Modifier.fillMaxSize()) {
                        items(state.sources, key = { it.id }) { source ->
                            CatalogSourceRow(source, state.commandInFlight, onIntent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogTopBar(onIntent: (CatalogIntent) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    AppTopBar(title = "发现", actions = {
        Box {
            AppIconButton(onClick = { expanded = true }) { AppIcon(Icons.Default.MoreVert, "更多") }
            DropdownMenu(expanded, { expanded = false }) {
                DropdownMenuItem({ Text("全局搜索") }, { expanded = false; onIntent(CatalogIntent.OpenGlobalSearch) })
                DropdownMenuItem({ Text("导入书源") }, { expanded = false; onIntent(CatalogIntent.OpenImport) })
                DropdownMenuItem({ Text("管理书源") }, { expanded = false; onIntent(CatalogIntent.OpenSourceManage) })
            }
        }
    })
}

@Composable
private fun CatalogSourceRow(source: CatalogSourceUi, disabled: Boolean, onIntent: (CatalogIntent) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    AppListItem(
        headlineContent = { Text(source.name) },
        supportingContent = { Text(source.group ?: "未分组") },
        modifier = Modifier.fillMaxWidth().clickable { onIntent(CatalogIntent.OpenDiscovery(source.id)) },
        trailingContent = {
            Row {
                Text(if (source.responseTimeMillis < 180_000) "${source.responseTimeMillis} ms" else "未测速")
                Box {
                    AppIconButton(onClick = { expanded = true }) { AppIcon(Icons.Default.MoreVert, "书源操作") }
                    DropdownMenu(expanded, { expanded = false }) {
                        DropdownMenuItem({ Text("搜索") }, { expanded = false; onIntent(CatalogIntent.SearchSource(source.id)) })
                        if (source.hasLogin) DropdownMenuItem({ Text("登录") }, { expanded = false; onIntent(CatalogIntent.Login(source.id)) })
                        DropdownMenuItem({ Text("编辑") }, { expanded = false; onIntent(CatalogIntent.Edit(source.id)) })
                        DropdownMenuItem({ Text("置顶") }, { expanded = false; if (!disabled) onIntent(CatalogIntent.Pin(source.id)) })
                        DropdownMenuItem({ Text("删除") }, { expanded = false; if (!disabled) onIntent(CatalogIntent.Delete(source.id)) })
                    }
                }
            }
        },
    )
}
