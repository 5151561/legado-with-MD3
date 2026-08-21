package io.legado.app.feature.readaloud.ui

import androidx.compose.runtime.Stable
import io.legado.app.feature.readaloud.api.ReadAloudStatus

@Stable
data class ReadAloudUiState(
    val loading: Boolean = true,
    val bookName: String = "",
    val chapterTitle: String = "",
    val currentText: String = "",
    val chapterPosition: Int = 0,
    val chapterLength: Int = 1,
    val engineName: String = "",
    val voiceName: String = "",
    val speed: Int = 10,
    val timerMinutes: Int = 0,
    val status: ReadAloudStatus = ReadAloudStatus.Idle,
    val commandInFlight: Boolean = false,
)

sealed interface ReadAloudIntent {
    data object TogglePause : ReadAloudIntent
    data object PreviousParagraph : ReadAloudIntent
    data object NextParagraph : ReadAloudIntent
    data object PreviousChapter : ReadAloudIntent
    data object NextChapter : ReadAloudIntent
    data class SeekTo(val value: Int) : ReadAloudIntent
    data class SetSpeed(val value: Int) : ReadAloudIntent
    data class SetTimer(val minutes: Int) : ReadAloudIntent
    data object OpenVoices : ReadAloudIntent
    data object OpenCache : ReadAloudIntent
    data object OpenSettings : ReadAloudIntent
    data object SwitchToClassic : ReadAloudIntent
}

sealed interface ReadAloudEffect {
    data object Voices : ReadAloudEffect
    data object Cache : ReadAloudEffect
    data object Settings : ReadAloudEffect
    data object Classic : ReadAloudEffect
    data class Message(val text: String) : ReadAloudEffect
}
