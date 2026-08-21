package io.legado.app.feature.readaloud.compat

import io.legado.app.feature.readaloud.api.ReadAloudCommandResult
import io.legado.app.feature.readaloud.api.ReadAloudSessionGateway
import io.legado.app.feature.readaloud.api.ReadAloudSnapshot
import io.legado.app.feature.readaloud.api.ReadAloudStatus
import io.legado.app.ui.book.readaloud.player.ReadAloudPlayerCoordinator
import io.legado.app.ui.book.readaloud.player.ReadAloudPlayerSourceState
import kotlinx.coroutines.flow.map

/** Service/UI bridge kept in :app until the playback service exposes a module-safe session API. */
class LegacyReadAloudAdapter(
    private val coordinator: ReadAloudPlayerCoordinator,
) : ReadAloudSessionGateway {
    override val session = coordinator.state.map(ReadAloudPlayerSourceState::toFeatureSnapshot)
    override val current: ReadAloudSnapshot get() = coordinator.snapshot().toFeatureSnapshot()

    override suspend fun togglePause() = command(coordinator::togglePause)
    override suspend fun previousParagraph() = command(coordinator::previousParagraph)
    override suspend fun nextParagraph() = command(coordinator::nextParagraph)
    override suspend fun previousChapter() = command(coordinator::previousChapter)
    override suspend fun nextChapter() = command(coordinator::nextChapter)
    override suspend fun seekTo(chapterPosition: Int) = command {
        coordinator.seekTo(chapterPosition, current.chapterLength)
    }
    override suspend fun setSpeed(value: Int) = suspendCommand { coordinator.setSpeed(value) }
    override suspend fun setTimer(minutes: Int) = suspendCommand { coordinator.setTimer(minutes) }

    private inline fun command(block: () -> Unit): ReadAloudCommandResult =
        runCatching(block).fold(
            onSuccess = { ReadAloudCommandResult.Success },
            onFailure = { ReadAloudCommandResult.Failure(it.message) },
        )

    private suspend inline fun suspendCommand(
        crossinline block: suspend () -> Unit,
    ): ReadAloudCommandResult = runCatching { block() }.fold(
        onSuccess = { ReadAloudCommandResult.Success },
        onFailure = { ReadAloudCommandResult.Failure(it.message) },
    )
}

private fun ReadAloudPlayerSourceState.toFeatureSnapshot() = ReadAloudSnapshot(
    bookName = bookName,
    chapterTitle = chapterTitle,
    currentText = playbackText,
    chapterPosition = chapterPosition,
    chapterLength = chapterLength.coerceAtLeast(1),
    engineName = engineName,
    voiceName = speakerName,
    speed = speed,
    timerMinutes = timerMinutes,
    status = when {
        bookUrl.isBlank() -> ReadAloudStatus.Idle
        isPaused -> ReadAloudStatus.Paused
        else -> ReadAloudStatus.Playing
    },
)
