package io.legado.app.feature.reader.compat

import io.legado.app.data.entities.BookProgress
import io.legado.app.feature.reader.api.ReaderBoundary
import io.legado.app.feature.reader.api.ReaderCommandResult
import io.legado.app.feature.reader.api.ReaderError
import io.legado.app.feature.reader.api.ReaderLoadState
import io.legado.app.feature.reader.api.ReaderProgress
import io.legado.app.feature.reader.api.ReaderSessionGateway
import io.legado.app.feature.reader.api.ReaderSnapshot
import io.legado.app.model.LegacyReaderSnapshot
import io.legado.app.model.ReadBook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Temporary app-owned bridge from the module-safe reader API to the existing ReaderSession SSOT.
 * It performs no independent writes and never publishes mutable runtime objects.
 */
class LegacyReaderAdapter : ReaderSessionGateway {
    override val session = ReadBook.snapshot
        .map(::toFeatureSnapshot)
        .distinctUntilChanged()

    override val current: ReaderSnapshot
        get() = toFeatureSnapshot(ReadBook.snapshot.value)

    override suspend fun nextPage(): ReaderCommandResult = onMain {
        when {
            ReadBook.moveToNextPage() -> ReaderCommandResult.Success
            ReadBook.moveToNextChapter(upContent = true) -> ReaderCommandResult.Success
            else -> ReaderCommandResult.BoundaryReached(ReaderBoundary.EndOfBook)
        }
    }

    override suspend fun previousPage(): ReaderCommandResult = onMain {
        when {
            ReadBook.moveToPrevPage() -> ReaderCommandResult.Success
            ReadBook.moveToPrevChapter(upContent = true) -> ReaderCommandResult.Success
            else -> ReaderCommandResult.BoundaryReached(ReaderBoundary.StartOfBook)
        }
    }

    override suspend fun nextChapter(): ReaderCommandResult = onMain {
        if (ReadBook.moveToNextChapter(upContent = true)) ReaderCommandResult.Success
        else ReaderCommandResult.BoundaryReached(ReaderBoundary.EndOfBook)
    }

    override suspend fun previousChapter(): ReaderCommandResult = onMain {
        if (ReadBook.moveToPrevChapter(upContent = true, toLast = false)) {
            ReaderCommandResult.Success
        } else {
            ReaderCommandResult.BoundaryReached(ReaderBoundary.StartOfBook)
        }
    }

    override suspend fun moveToChapter(index: Int, position: Int): ReaderCommandResult = onMain {
        val chapterCount = ReadBook.snapshot.value.simulatedChapterCount
        if (index !in 0 until chapterCount || position < 0) {
            return@onMain ReaderCommandResult.Failure(
                ReaderError.InvalidProgress("chapter=$index position=$position count=$chapterCount")
            )
        }
        ReadBook.openChapter(index, position)
        ReaderCommandResult.Success
    }

    override suspend fun saveProgress(): ReaderCommandResult = onMain {
        if (ReadBook.book == null) return@onMain noActiveSession()
        ReadBook.saveRead()
        ReaderCommandResult.Success
    }

    override suspend fun restoreProgress(progress: ReaderProgress): ReaderCommandResult = onMain {
        val book = ReadBook.book ?: return@onMain noActiveSession()
        if (book.name != progress.bookName || book.author != progress.author) {
            return@onMain ReaderCommandResult.Failure(
                ReaderError.InvalidProgress("progress belongs to another book")
            )
        }
        if (progress.chapterIndex !in 0 until ReadBook.snapshot.value.simulatedChapterCount ||
            progress.chapterPosition < 0
        ) {
            return@onMain ReaderCommandResult.Failure(
                ReaderError.InvalidProgress("progress is outside current book")
            )
        }
        ReadBook.setProgress(
            BookProgress(
                name = progress.bookName,
                author = progress.author,
                durChapterIndex = progress.chapterIndex,
                durChapterPos = progress.chapterPosition,
                durChapterTime = progress.updatedAtMillis,
                durChapterTitle = progress.chapterTitle,
            )
        )
        ReaderCommandResult.Success
    }

    override suspend fun syncProgress(): ReaderCommandResult = onMain {
        if (ReadBook.book == null) return@onMain noActiveSession()
        ReadBook.syncProgress()
        ReaderCommandResult.Success
    }

    override suspend fun retry(): ReaderCommandResult = onMain {
        if (ReadBook.book == null) return@onMain noActiveSession()
        ReadBook.upMsg(null)
        ReadBook.loadContent(resetPageOffset = true)
        ReaderCommandResult.Success
    }

    private suspend fun onMain(block: () -> ReaderCommandResult): ReaderCommandResult =
        withContext(Dispatchers.Main.immediate) {
            runCatching(block).fold(
                onSuccess = { it },
                onFailure = {
                    ReaderCommandResult.Failure(ReaderError.Unexpected(it.message))
                },
            )
        }

    private fun noActiveSession() = ReaderCommandResult.Failure(
        ReaderError.ContentUnavailable("no active reader session", retryable = false)
    )
}

private fun toFeatureSnapshot(source: LegacyReaderSnapshot): ReaderSnapshot {
    val message = source.message
    val loadState = when {
        source.bookUrl == null -> ReaderLoadState.Idle
        message != null -> ReaderLoadState.Failure
        source.pageText.isNotBlank() -> ReaderLoadState.Content
        else -> ReaderLoadState.Loading
    }
    return ReaderSnapshot(
        sessionId = source.bookUrl,
        bookName = source.bookName.orEmpty(),
        chapterTitle = source.chapterTitle,
        chapterIndex = source.chapterIndex,
        chapterCount = source.simulatedChapterCount,
        chapterPosition = source.chapterPos,
        pageIndex = source.pageIndex,
        pageCount = source.pageCount,
        pageText = source.pageText,
        canGoPrevious = source.chapterIndex > 0 || source.pageIndex > 0,
        canGoNext = source.chapterIndex < source.simulatedChapterCount - 1 ||
            source.pageIndex < source.pageCount - 1,
        loadState = loadState,
        error = message?.let(ReaderError::ContentUnavailable),
        contentRevision = source.contentRevision,
    )
}
