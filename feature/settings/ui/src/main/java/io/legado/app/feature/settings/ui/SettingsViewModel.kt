package io.legado.app.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.legado.app.feature.settings.api.SettingsOverviewQuery
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val query: SettingsOverviewQuery,
) : ViewModel() {
    private var observeJob: Job? = null
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<SettingsEffect>(extraBufferCapacity = 8)
    val effects = _effects.asSharedFlow()

    init {
        observe()
    }

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            SettingsIntent.Retry -> observe()
            SettingsIntent.OpenTheme -> effect(SettingsEffect.Theme)
            SettingsIntent.OpenInterface -> effect(SettingsEffect.Interface)
            SettingsIntent.OpenDownloadCache -> effect(SettingsEffect.DownloadCache)
            SettingsIntent.OpenBackup -> effect(SettingsEffect.Backup)
            SettingsIntent.OpenRead -> effect(SettingsEffect.Read)
            SettingsIntent.OpenCover -> effect(SettingsEffect.Cover)
            SettingsIntent.OpenAi -> effect(SettingsEffect.Ai)
            SettingsIntent.OpenTranslation -> effect(SettingsEffect.Translation)
            SettingsIntent.OpenLab -> effect(SettingsEffect.Lab)
        }
    }

    private fun observe() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            _uiState.update { it.copy(loading = true, loadFailed = false) }
            query.observeOverview()
                .catch { _uiState.update { state -> state.copy(loading = false, loadFailed = true) } }
                .collect { overview ->
                    _uiState.value = SettingsUiState(
                        loading = false,
                        themeMode = overview.themeMode,
                        fontScale = overview.fontScale,
                        bitmapCacheSizeMb = overview.bitmapCacheSizeMb,
                        downloadThreadCount = overview.downloadThreadCount,
                        backupConfigured = overview.backupConfigured,
                        syncBookProgress = overview.syncBookProgress,
                    )
                }
        }
    }

    private fun effect(effect: SettingsEffect) {
        _effects.tryEmit(effect)
    }
}
