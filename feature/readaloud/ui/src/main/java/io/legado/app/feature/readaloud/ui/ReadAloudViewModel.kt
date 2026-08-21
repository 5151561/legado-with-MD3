package io.legado.app.feature.readaloud.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.legado.app.feature.readaloud.api.ReadAloudCommandResult
import io.legado.app.feature.readaloud.api.ReadAloudSessionGateway
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReadAloudViewModel(private val gateway: ReadAloudSessionGateway) : ViewModel() {
    private val _uiState = MutableStateFlow(gateway.current.toUiState())
    val uiState = _uiState.asStateFlow()
    private val _effects = MutableSharedFlow<ReadAloudEffect>(extraBufferCapacity = 8)
    val effects = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            gateway.session.collect { snapshot ->
                _uiState.update { snapshot.toUiState(commandInFlight = it.commandInFlight) }
            }
        }
    }

    fun onIntent(intent: ReadAloudIntent) {
        when (intent) {
            ReadAloudIntent.TogglePause -> command(gateway::togglePause)
            ReadAloudIntent.PreviousParagraph -> command(gateway::previousParagraph)
            ReadAloudIntent.NextParagraph -> command(gateway::nextParagraph)
            ReadAloudIntent.PreviousChapter -> command(gateway::previousChapter)
            ReadAloudIntent.NextChapter -> command(gateway::nextChapter)
            is ReadAloudIntent.SeekTo -> command { gateway.seekTo(intent.value) }
            is ReadAloudIntent.SetSpeed -> command { gateway.setSpeed(intent.value) }
            is ReadAloudIntent.SetTimer -> command { gateway.setTimer(intent.minutes) }
            ReadAloudIntent.OpenVoices -> _effects.tryEmit(ReadAloudEffect.Voices)
            ReadAloudIntent.OpenCache -> _effects.tryEmit(ReadAloudEffect.Cache)
            ReadAloudIntent.OpenSettings -> _effects.tryEmit(ReadAloudEffect.Settings)
            ReadAloudIntent.SwitchToClassic -> _effects.tryEmit(ReadAloudEffect.Classic)
        }
    }

    private fun command(block: suspend () -> ReadAloudCommandResult) {
        if (_uiState.value.commandInFlight) return
        viewModelScope.launch {
            _uiState.update { it.copy(commandInFlight = true) }
            val result = runCatching { block() }.getOrElse { ReadAloudCommandResult.Failure(it.message) }
            _uiState.update { it.copy(commandInFlight = false) }
            if (result is ReadAloudCommandResult.Failure) {
                _effects.emit(ReadAloudEffect.Message(result.message ?: "朗读操作失败"))
            }
        }
    }
}

private fun io.legado.app.feature.readaloud.api.ReadAloudSnapshot.toUiState(
    commandInFlight: Boolean = false,
) = ReadAloudUiState(
    loading = false,
    bookName = bookName,
    chapterTitle = chapterTitle,
    currentText = currentText,
    chapterPosition = chapterPosition,
    chapterLength = chapterLength.coerceAtLeast(1),
    engineName = engineName,
    voiceName = voiceName,
    speed = speed,
    timerMinutes = timerMinutes,
    status = status,
    commandInFlight = commandInFlight,
)
