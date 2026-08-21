package io.legado.app.feature.catalog.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.legado.app.feature.catalog.api.CatalogCommandResult
import io.legado.app.feature.catalog.api.CatalogCommands
import io.legado.app.feature.catalog.api.CatalogQuery
import io.legado.app.feature.catalog.api.CatalogQueryState
import io.legado.app.feature.catalog.api.CatalogRequest
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModel(
    private val query: CatalogQuery,
    private val commands: CatalogCommands,
) : ViewModel() {
    private val request = MutableStateFlow(CatalogRequest())
    private val reload = MutableStateFlow(0L)
    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState = _uiState.asStateFlow()
    private val _effects = MutableSharedFlow<CatalogEffect>(extraBufferCapacity = 8)
    val effects = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(request, reload) { value, _ -> value }
                .flatMapLatest(query::observeCatalog)
                .collect(::applyState)
        }
    }

    fun onIntent(intent: CatalogIntent) {
        when (intent) {
            CatalogIntent.Retry -> reload.update { it + 1 }
            is CatalogIntent.Search -> {
                _uiState.update { it.copy(query = intent.value) }
                request.update { it.copy(query = intent.value) }
            }
            is CatalogIntent.SelectGroup -> {
                _uiState.update { it.copy(selectedGroup = intent.value) }
                request.update { it.copy(group = intent.value, query = "") }
            }
            is CatalogIntent.OpenDiscovery -> source(intent.sourceId)?.let { _effects.tryEmit(CatalogEffect.OpenDiscovery(it.id, it.name)) }
            is CatalogIntent.SearchSource -> source(intent.sourceId)?.let { _effects.tryEmit(CatalogEffect.SearchSource(it.id, it.name)) }
            is CatalogIntent.Login -> _effects.tryEmit(CatalogEffect.Login(intent.sourceId))
            is CatalogIntent.Edit -> _effects.tryEmit(CatalogEffect.Edit(intent.sourceId))
            is CatalogIntent.Pin -> command { commands.pinSource(intent.sourceId) }
            is CatalogIntent.Delete -> command { commands.deleteSource(intent.sourceId) }
            CatalogIntent.OpenGlobalSearch -> _effects.tryEmit(CatalogEffect.GlobalSearch)
            CatalogIntent.OpenSourceManage -> _effects.tryEmit(CatalogEffect.SourceManage)
            CatalogIntent.OpenImport -> _effects.tryEmit(CatalogEffect.Import)
        }
    }

    private fun applyState(state: CatalogQueryState) {
        when (state) {
            CatalogQueryState.Loading -> _uiState.update { it.copy(loading = true, loadFailed = false) }
            is CatalogQueryState.Failed -> _uiState.update { it.copy(loading = false, loadFailed = true) }
            is CatalogQueryState.Data -> _uiState.update { current ->
                current.copy(
                    loading = false,
                    loadFailed = false,
                    groups = state.snapshot.groups.toImmutableList(),
                    sources = state.snapshot.sources.map { CatalogSourceUi(it.id, it.name, it.group, it.hasLogin, it.responseTimeMillis) }.toImmutableList(),
                )
            }
        }
    }

    private fun command(block: suspend () -> CatalogCommandResult) {
        if (_uiState.value.commandInFlight) return
        viewModelScope.launch {
            _uiState.update { it.copy(commandInFlight = true) }
            val result = runCatching { block() }.getOrElse { CatalogCommandResult.Failure(it.message) }
            _uiState.update { it.copy(commandInFlight = false) }
            val text = when (result) {
                CatalogCommandResult.Success -> "操作成功"
                is CatalogCommandResult.Failure -> result.message ?: "操作失败"
            }
            _effects.emit(CatalogEffect.Message(text))
        }
    }

    private fun source(id: String) = _uiState.value.sources.firstOrNull { it.id == id }
}
