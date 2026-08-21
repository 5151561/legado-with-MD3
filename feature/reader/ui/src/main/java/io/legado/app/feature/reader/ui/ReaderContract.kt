package io.legado.app.feature.reader.ui

import androidx.compose.runtime.Stable
import io.legado.app.feature.reader.api.ReaderError
import io.legado.app.feature.reader.api.ReaderLoadState

@Stable
data class ReaderUiState(
    val bookName: String = "",
    val chapterTitle: String = "",
    val chapterIndex: Int = 0,
    val chapterCount: Int = 0,
    val pageIndex: Int = 0,
    val pageCount: Int = 0,
    val pageText: String = "",
    val canGoPrevious: Boolean = false,
    val canGoNext: Boolean = false,
    val loadState: ReaderLoadState = ReaderLoadState.Idle,
    val error: ReaderError? = null,
    val contentRevision: Long = 0L,
    val commandInFlight: Boolean = false,
)

sealed interface ReaderIntent {
    data object PreviousPage : ReaderIntent
    data object NextPage : ReaderIntent
    data object Retry : ReaderIntent
}

sealed interface ReaderEffect {
    data class Message(val text: String?) : ReaderEffect
}
