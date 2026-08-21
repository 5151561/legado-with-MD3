package io.legado.app.feature.readaloud.api

import kotlinx.coroutines.flow.Flow

enum class ReadAloudStatus { Idle, Playing, Paused }

data class ReadAloudSnapshot(
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
)

sealed interface ReadAloudCommandResult {
    data object Success : ReadAloudCommandResult
    data class Failure(val message: String?) : ReadAloudCommandResult
}

interface ReadAloudSessionGateway {
    val session: Flow<ReadAloudSnapshot>
    val current: ReadAloudSnapshot
    suspend fun togglePause(): ReadAloudCommandResult
    suspend fun previousParagraph(): ReadAloudCommandResult
    suspend fun nextParagraph(): ReadAloudCommandResult
    suspend fun previousChapter(): ReadAloudCommandResult
    suspend fun nextChapter(): ReadAloudCommandResult
    suspend fun seekTo(chapterPosition: Int): ReadAloudCommandResult
    suspend fun setSpeed(value: Int): ReadAloudCommandResult
    suspend fun setTimer(minutes: Int): ReadAloudCommandResult
}
