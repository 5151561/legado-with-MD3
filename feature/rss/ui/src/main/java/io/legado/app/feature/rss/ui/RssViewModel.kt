package io.legado.app.feature.rss.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.legado.app.feature.rss.api.RssCommandResult
import io.legado.app.feature.rss.api.RssCommands
import io.legado.app.feature.rss.api.RssQuery
import io.legado.app.feature.rss.api.RssQueryState
import io.legado.app.feature.rss.api.RssRequest
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
class RssViewModel(
    private val query: RssQuery,
    private val commands: RssCommands,
) : ViewModel() {
    private val request = MutableStateFlow(RssRequest())
    private val reload = MutableStateFlow(0L)
    private val _uiState = MutableStateFlow(RssUiState())
    val uiState = _uiState.asStateFlow()
    private val _effects = MutableSharedFlow<RssEffect>(extraBufferCapacity = 8)
    val effects = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(request, reload) { value, _ -> value }
                .flatMapLatest(query::observeSources)
                .collect(::applyState)
        }
    }

    fun onIntent(intent: RssIntent) {
        when (intent) {
            RssIntent.Retry -> reload.update { it + 1 }
            is RssIntent.Search -> {
                _uiState.update { it.copy(query = intent.value) }
                request.update { it.copy(query = intent.value) }
            }
            is RssIntent.SelectGroup -> {
                _uiState.update { it.copy(selectedGroup = intent.value, query = "") }
                request.update { it.copy(group = intent.value, query = "") }
            }
            is RssIntent.Open -> open(intent.sourceId)
            is RssIntent.Login -> _effects.tryEmit(RssEffect.Login(intent.sourceId))
            is RssIntent.Edit -> _effects.tryEmit(RssEffect.Edit(intent.sourceId))
            is RssIntent.Pin -> command { commands.pinSource(intent.sourceId) }
            is RssIntent.Disable -> command { commands.disableSource(intent.sourceId) }
            is RssIntent.Delete -> command { commands.deleteSource(intent.sourceId) }
            RssIntent.Favorites -> _effects.tryEmit(RssEffect.Favorites)
            RssIntent.Manage -> _effects.tryEmit(RssEffect.Manage)
            RssIntent.RuleSubscriptions -> _effects.tryEmit(RssEffect.RuleSubscriptions)
        }
    }

    private fun open(sourceId: String) {
        if (_uiState.value.commandInFlight) return
        viewModelScope.launch {
            _uiState.update { it.copy(commandInFlight = true) }
            commands.resolveOpenTarget(sourceId)
                .onSuccess { _effects.emit(RssEffect.Open(it)) }
                .onFailure { _effects.emit(RssEffect.Message(it.message ?: "打开订阅源失败")) }
            _uiState.update { it.copy(commandInFlight = false) }
        }
    }

    private fun command(block: suspend () -> RssCommandResult) {
        if (_uiState.value.commandInFlight) return
        viewModelScope.launch {
            _uiState.update { it.copy(commandInFlight = true) }
            val result = runCatching { block() }.getOrElse { RssCommandResult.Failure(it.message) }
            _uiState.update { it.copy(commandInFlight = false) }
            val message = when (result) {
                RssCommandResult.Success -> "操作成功"
                is RssCommandResult.Failure -> result.message ?: "操作失败"
            }
            _effects.emit(RssEffect.Message(message))
        }
    }

    private fun applyState(state: RssQueryState) {
        when (state) {
            RssQueryState.Loading -> _uiState.update { it.copy(loading = true, loadFailed = false) }
            is RssQueryState.Failed -> _uiState.update { it.copy(loading = false, loadFailed = true) }
            is RssQueryState.Data -> _uiState.update { current ->
                current.copy(
                    loading = false,
                    loadFailed = false,
                    groups = state.snapshot.groups.toImmutableList(),
                    sources = state.snapshot.sources.map {
                        RssSourceUi(it.id, it.name, it.icon, it.group, it.hasLogin)
                    }.toImmutableList(),
                )
            }
        }
    }
}
