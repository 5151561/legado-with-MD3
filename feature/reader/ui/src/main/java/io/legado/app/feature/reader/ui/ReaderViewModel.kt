package io.legado.app.feature.reader.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.legado.app.feature.reader.api.ReaderCommandResult
import io.legado.app.feature.reader.api.ReaderSessionGateway
import io.legado.app.feature.reader.api.ReaderSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReaderViewModel(
    private val gateway: ReaderSessionGateway,
) : ViewModel() {
    private var commandInFlight = false
    private val _uiState = MutableStateFlow(gateway.current.toUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<ReaderEffect>(extraBufferCapacity = 4)
    val effects = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            gateway.session.collect { snapshot ->
                _uiState.update { current ->
                    snapshot.toUiState(commandInFlight = current.commandInFlight)
                }
            }
        }
    }

    fun onIntent(intent: ReaderIntent) {
        when (intent) {
            ReaderIntent.PreviousPage -> command(gateway::previousPage)
            ReaderIntent.NextPage -> command(gateway::nextPage)
            ReaderIntent.Retry -> command(gateway::retry)
        }
    }

    private fun command(block: suspend () -> ReaderCommandResult) {
        if (commandInFlight) return
        commandInFlight = true
        _uiState.update { it.copy(commandInFlight = true) }
        viewModelScope.launch {
            val result = try {
                block()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                ReaderCommandResult.Failure(
                    io.legado.app.feature.reader.api.ReaderError.Unexpected(throwable.message)
                )
            }
            commandInFlight = false
            _uiState.update { it.copy(commandInFlight = false) }
            if (result is ReaderCommandResult.Failure) {
                _effects.emit(ReaderEffect.Message(result.error.diagnosticMessage))
            }
        }
    }
}

private fun ReaderSnapshot.toUiState(commandInFlight: Boolean = false) = ReaderUiState(
    bookName = bookName,
    chapterTitle = chapterTitle,
    chapterIndex = chapterIndex,
    chapterCount = chapterCount,
    pageIndex = pageIndex,
    pageCount = pageCount,
    pageText = pageText,
    canGoPrevious = canGoPrevious,
    canGoNext = canGoNext,
    loadState = loadState,
    error = error,
    contentRevision = contentRevision,
    commandInFlight = commandInFlight,
)
