package io.legado.app.feature.bookshelf.api

/** Stable identifiers intentionally remain the existing bookUrl/groupId values. */
data class BookshelfQueryRequest(
    val groupId: Long = BookshelfGroup.AllId,
    val searchQuery: String = "",
    val sort: BookshelfSort = BookshelfSort.RecentReading,
    val descending: Boolean = true,
)

enum class BookshelfSort {
    RecentReading,
    LatestChapter,
    BookName,
    Manual,
    LastActivity,
    Author,
}

data class BookshelfSnapshot(
    val selectedGroupId: Long,
    val groups: List<BookshelfGroup>,
    val books: List<BookshelfBookSummary>,
)

data class BookshelfGroup(
    val id: Long,
    val name: String,
    val order: Int,
    val coverUrl: String? = null,
    val bookSort: BookshelfSort? = null,
    val isUserGroup: Boolean = id > 0,
    val isPrivate: Boolean = false,
    val isRefreshEnabled: Boolean = true,
    val isVisible: Boolean = true,
) {
    companion object {
        const val AllId = -1L
    }
}

data class BookshelfBookSummary(
    val id: String,
    val name: String,
    val author: String,
    val origin: String,
    val originName: String,
    val coverUrl: String?,
    val currentChapterTitle: String?,
    val latestChapterTitle: String?,
    val currentChapterIndex: Int,
    val totalChapterCount: Int,
    val unreadChapterCount: Int,
    val readingProgress: Float,
    val lastReadAt: Long,
    val latestChapterAt: Long,
    val groupMask: Long,
    val order: Int,
    val isLocal: Boolean,
    val isAudio: Boolean,
    val isImage: Boolean,
    val isUpdating: Boolean = false,
)

data class BookshelfPreferences(
    val selectedGroupId: Long = BookshelfGroup.AllId,
    val defaultSort: BookshelfSort = BookshelfSort.RecentReading,
    val descending: Boolean = true,
    val deleteOriginalDefault: Boolean = false,
)

data class BookshelfGroupDraft(
    val name: String,
    val sort: BookshelfSort? = null,
    val isRefreshEnabled: Boolean = true,
    val isPrivate: Boolean = false,
    val coverUrl: String? = null,
)

sealed interface BookshelfQueryState {
    data object Loading : BookshelfQueryState

    data class Data(
        val snapshot: BookshelfSnapshot,
        val warnings: List<BookshelfIssue> = emptyList(),
    ) : BookshelfQueryState

    data class Failure(
        val error: BookshelfError,
        val previous: BookshelfSnapshot? = null,
    ) : BookshelfQueryState
}

data class BookshelfIssue(
    val error: BookshelfError,
    val affectedBookIds: Set<String> = emptySet(),
)

sealed interface BookshelfCommandResult {
    data class Success(val changedBookIds: Set<String> = emptySet()) : BookshelfCommandResult

    data class Partial(
        val changedBookIds: Set<String>,
        val failed: Map<String, BookshelfError>,
    ) : BookshelfCommandResult

    data class Failure(val error: BookshelfError) : BookshelfCommandResult
}

sealed interface BookshelfError {
    val diagnostic: String?

    data class Retryable(override val diagnostic: String? = null) : BookshelfError
    data class InvalidRequest(override val diagnostic: String? = null) : BookshelfError
    data class NotFound(override val diagnostic: String? = null) : BookshelfError
    data class PermissionDenied(override val diagnostic: String? = null) : BookshelfError
    data class Conflict(override val diagnostic: String? = null) : BookshelfError
    data class Unexpected(override val diagnostic: String? = null) : BookshelfError
}
