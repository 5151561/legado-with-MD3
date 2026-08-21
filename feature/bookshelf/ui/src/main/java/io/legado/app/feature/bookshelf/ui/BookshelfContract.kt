package io.legado.app.feature.bookshelf.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import io.legado.app.feature.bookshelf.api.BookshelfError
import io.legado.app.feature.bookshelf.api.BookshelfSort
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Immutable
data class BookshelfBookUi(
    val id: String,
    val name: String,
    val author: String,
    val coverUrl: String?,
    val chapterTitle: String?,
    val unreadChapterCount: Int,
    val readingProgress: Float,
    val isLocal: Boolean,
    val isAudio: Boolean,
    val isImage: Boolean,
    val origin: String,
)

@Immutable
data class BookshelfGroupUi(
    val id: Long,
    val name: String,
    val isUserGroup: Boolean,
)

@Immutable
data class BookshelfOpenBookRequest(
    val id: String,
    val name: String,
    val author: String,
    val origin: String,
    val coverUrl: String?,
    val isLocal: Boolean,
    val isAudio: Boolean,
    val isImage: Boolean,
)

@Immutable
sealed interface BookshelfContentState {
    data object Loading : BookshelfContentState
    data object Content : BookshelfContentState
    data object Empty : BookshelfContentState
    data class Error(val retryable: Boolean) : BookshelfContentState
}

@Stable
data class BookshelfUiState(
    val contentState: BookshelfContentState = BookshelfContentState.Loading,
    val books: ImmutableList<BookshelfBookUi> = persistentListOf(),
    val groups: ImmutableList<BookshelfGroupUi> = persistentListOf(),
    val selectedGroupId: Long = -1L,
    val searchQuery: String = "",
    val searchVisible: Boolean = false,
    val sort: BookshelfSort = BookshelfSort.RecentReading,
    val descending: Boolean = true,
    val deleteOriginalDefault: Boolean = false,
    val selectedBookIds: ImmutableSet<String> = persistentSetOf(),
    val commandInFlight: Boolean = false,
    val pendingDeleteIds: ImmutableSet<String> = persistentSetOf(),
    val showMoveSheet: Boolean = false,
) {
    val isSelectionMode: Boolean get() = selectedBookIds.isNotEmpty()
}

sealed interface BookshelfIntent {
    data object RetryLoad : BookshelfIntent
    data class SelectGroup(val groupId: Long) : BookshelfIntent
    data object ToggleSearch : BookshelfIntent
    data class ChangeSearchQuery(val query: String) : BookshelfIntent
    data class ChangeSort(val sort: BookshelfSort) : BookshelfIntent
    data object ToggleSortDirection : BookshelfIntent
    data class OpenBook(val bookId: String) : BookshelfIntent
    data class OpenBookInfo(val bookId: String) : BookshelfIntent
    data class ToggleSelection(val bookId: String) : BookshelfIntent
    data object SelectAll : BookshelfIntent
    data object ClearSelection : BookshelfIntent
    data object RequestMove : BookshelfIntent
    data object DismissMove : BookshelfIntent
    data class MoveSelected(val groupId: Long) : BookshelfIntent
    data object RequestDelete : BookshelfIntent
    data object DismissDelete : BookshelfIntent
    data class ConfirmDelete(val deleteOriginal: Boolean) : BookshelfIntent
    data object NavigateToLocalImport : BookshelfIntent
    data object NavigateToRemoteImport : BookshelfIntent
    data object NavigateToGlobalSearch : BookshelfIntent
    data object NavigateToManage : BookshelfIntent
}

sealed interface BookshelfEffect {
    data class OpenBook(val request: BookshelfOpenBookRequest) : BookshelfEffect
    data class OpenBookInfo(val request: BookshelfOpenBookRequest) : BookshelfEffect
    data object OpenLocalImport : BookshelfEffect
    data object OpenRemoteImport : BookshelfEffect
    data class OpenGlobalSearch(val query: String) : BookshelfEffect
    data class OpenManage(val groupId: Long) : BookshelfEffect
    data class ShowMessage(val message: BookshelfMessage) : BookshelfEffect
}

sealed interface BookshelfMessage {
    data object Success : BookshelfMessage
    data class Partial(val changed: Int, val failed: Int) : BookshelfMessage
    data class Failure(val error: BookshelfError) : BookshelfMessage
}
