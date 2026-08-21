package io.legado.app.feature.reader.api

import kotlinx.coroutines.flow.Flow

enum class ReaderLoadState {
    Idle,
    Loading,
    Content,
    Failure,
}

enum class ReaderBoundary {
    StartOfBook,
    EndOfBook,
}

data class ReaderProgress(
    val bookName: String,
    val author: String,
    val chapterIndex: Int,
    val chapterPosition: Int,
    val updatedAtMillis: Long,
    val chapterTitle: String? = null,
)

data class ReaderSnapshot(
    val sessionId: String? = null,
    val bookName: String = "",
    val chapterTitle: String = "",
    val chapterIndex: Int = 0,
    val chapterCount: Int = 0,
    val chapterPosition: Int = 0,
    val pageIndex: Int = 0,
    val pageCount: Int = 0,
    val pageText: String = "",
    val canGoPrevious: Boolean = false,
    val canGoNext: Boolean = false,
    val loadState: ReaderLoadState = ReaderLoadState.Idle,
    val error: ReaderError? = null,
    val contentRevision: Long = 0L,
)

sealed interface ReaderError {
    val diagnosticMessage: String?
    val retryable: Boolean

    data class ContentUnavailable(
        override val diagnosticMessage: String?,
        override val retryable: Boolean = true,
    ) : ReaderError

    data class InvalidProgress(
        override val diagnosticMessage: String?,
        override val retryable: Boolean = false,
    ) : ReaderError

    data class Unexpected(
        override val diagnosticMessage: String?,
        override val retryable: Boolean = true,
    ) : ReaderError
}

sealed interface ReaderCommandResult {
    data object Success : ReaderCommandResult
    data class BoundaryReached(val boundary: ReaderBoundary) : ReaderCommandResult
    data class RemoteProgressAvailable(val progress: ReaderProgress) : ReaderCommandResult
    data class Failure(val error: ReaderError) : ReaderCommandResult
}

/**
 * The only module-safe owner of text-reader progress commands.
 *
 * Implementations may bridge the legacy runtime during migration, but callers never receive
 * mutable Book/TextPage/TextChapter instances, DAO types, file paths, or Android UI objects.
 */
interface ReaderSessionGateway {
    val session: Flow<ReaderSnapshot>
    val current: ReaderSnapshot

    suspend fun nextPage(): ReaderCommandResult
    suspend fun previousPage(): ReaderCommandResult
    suspend fun nextChapter(): ReaderCommandResult
    suspend fun previousChapter(): ReaderCommandResult
    suspend fun moveToChapter(index: Int, position: Int = 0): ReaderCommandResult
    suspend fun saveProgress(): ReaderCommandResult
    suspend fun restoreProgress(progress: ReaderProgress): ReaderCommandResult
    suspend fun syncProgress(): ReaderCommandResult
    suspend fun retry(): ReaderCommandResult
}
