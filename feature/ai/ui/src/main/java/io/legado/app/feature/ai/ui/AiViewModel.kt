package io.legado.app.feature.ai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.legado.app.feature.ai.api.AiCommandResult
import io.legado.app.feature.ai.api.AiCommands
import io.legado.app.feature.ai.api.AiOverviewQuery
import io.legado.app.feature.ai.api.AiQueryState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AiViewModel(
    private val query: AiOverviewQuery,
    private val commands: AiCommands,
) : ViewModel() {
    private var observeJob: Job? = null
    private val _uiState = MutableStateFlow(AiUiState())
    val uiState = _uiState.asStateFlow()
    private val _effects = MutableSharedFlow<AiEffect>(extraBufferCapacity = 8)
    val effects = _effects.asSharedFlow()

    init { observe() }

    fun onIntent(intent: AiIntent) {
        when (intent) {
            AiIntent.Retry -> observe()
            is AiIntent.SetDefaultModel -> setDefault(intent.modelId)
            AiIntent.OpenChat -> _effects.tryEmit(AiEffect.Chat)
            AiIntent.AddProvider -> _effects.tryEmit(AiEffect.AddProvider)
            is AiIntent.EditProvider -> _effects.tryEmit(AiEffect.EditProvider(intent.providerId))
            is AiIntent.EditModel -> _effects.tryEmit(AiEffect.EditModel(intent.providerId, intent.modelId))
            AiIntent.OpenSummaryPrompt -> _effects.tryEmit(AiEffect.SummaryPrompt)
            AiIntent.OpenPromptSettings -> _effects.tryEmit(AiEffect.PromptSettings)
        }
    }

    private fun observe() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            query.observeOverview().collect { state ->
                when (state) {
                    AiQueryState.Loading -> _uiState.update { it.copy(loading = true, loadFailed = false) }
                    is AiQueryState.Failed -> _uiState.update { it.copy(loading = false, loadFailed = true) }
                    is AiQueryState.Data -> {
                        val providerNames = state.overview.providers.associate { it.id to it.name }
                        _uiState.update { current -> current.copy(
                            loading = false,
                            loadFailed = false,
                            providerCount = state.overview.providers.size,
                            presetCount = state.overview.presetCount,
                            models = state.overview.models.map {
                                AiModelUi(
                                    id = it.id,
                                    providerId = it.providerId,
                                    providerName = providerNames[it.providerId].orEmpty(),
                                    name = it.name,
                                    modelId = it.modelId,
                                    enabled = it.enabled,
                                    isDefault = it.isDefault,
                                )
                            }.toImmutableList(),
                        ) }
                    }
                }
            }
        }
    }

    private fun setDefault(modelId: String) {
        if (_uiState.value.commandInFlight) return
        viewModelScope.launch {
            _uiState.update { it.copy(commandInFlight = true) }
            val result = runCatching { commands.setDefaultModel(modelId) }.getOrElse { AiCommandResult.Failure(it.message) }
            _uiState.update { it.copy(commandInFlight = false) }
            val text = when (result) {
                AiCommandResult.Success -> "默认模型已更新"
                is AiCommandResult.Failure -> result.message ?: "模型更新失败"
            }
            _effects.emit(AiEffect.Message(text))
        }
    }
}
