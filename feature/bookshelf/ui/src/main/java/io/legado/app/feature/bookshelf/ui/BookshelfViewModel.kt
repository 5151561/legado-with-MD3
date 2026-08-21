package io.legado.app.feature.bookshelf.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.legado.app.feature.bookshelf.api.BookshelfBookSummary
import io.legado.app.feature.bookshelf.api.BookshelfCommandResult
import io.legado.app.feature.bookshelf.api.BookshelfCommands
import io.legado.app.feature.bookshelf.api.BookshelfError
import io.legado.app.feature.bookshelf.api.BookshelfPreferencesGateway
import io.legado.app.feature.bookshelf.api.BookshelfQuery
import io.legado.app.feature.bookshelf.api.BookshelfQueryRequest
import io.legado.app.feature.bookshelf.api.BookshelfQueryState
import io.legado.app.feature.bookshelf.api.BookshelfSnapshot
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class BookshelfViewModel(
    private val query: BookshelfQuery,
    private val commands: BookshelfCommands,
    private val preferences: BookshelfPreferencesGateway,
) : ViewModel() {
    private val initialPreferences = preferences.current
    private val request = MutableStateFlow(
        BookshelfQueryRequest(
            groupId = initialPreferences.selectedGroupId,
            sort = initialPreferences.defaultSort,
            descending = initialPreferences.descending,
        )
    )
    private val reloadToken = MutableStateFlow(0L)
    private var latestSnapshot: BookshelfSnapshot? = null

    private val _uiState = MutableStateFlow(
        BookshelfUiState(
            selectedGroupId = initialPreferences.selectedGroupId,
            sort = initialPreferences.defaultSort,
            descending = initialPreferences.descending,
            deleteOriginalDefault = initialPreferences.deleteOriginalDefault,
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<BookshelfEffect>(extraBufferCapacity = 8)
    val effects = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(request, reloadToken) { value, _ -> value }
                .flatMapLatest(query::observeBookshelf)
                .collect(::applyQueryState)
        }
    }

    fun onIntent(intent: BookshelfIntent) {
        when (intent) {
            BookshelfIntent.RetryLoad -> reloadToken.update { it + 1 }
            is BookshelfIntent.SelectGroup -> selectGroup(intent.groupId)
            BookshelfIntent.ToggleSearch -> toggleSearch()
            is BookshelfIntent.ChangeSearchQuery -> changeSearchQuery(intent.query)
            is BookshelfIntent.ChangeSort -> changeSort(intent.sort)
            BookshelfIntent.ToggleSortDirection -> request.update {
                it.copy(descending = !it.descending)
            }
            is BookshelfIntent.OpenBook -> openBook(intent.bookId)
            is BookshelfIntent.OpenBookInfo -> openBookInfo(intent.bookId)
            is BookshelfIntent.ToggleSelection -> toggleSelection(intent.bookId)
            BookshelfIntent.SelectAll -> _uiState.update { state ->
                state.copy(selectedBookIds = state.books.map { it.id }.toImmutableSet())
            }
            BookshelfIntent.ClearSelection -> clearSelection()
            BookshelfIntent.RequestMove -> _uiState.update {
                it.copy(showMoveSheet = it.selectedBookIds.isNotEmpty())
            }
            BookshelfIntent.DismissMove -> _uiState.update { it.copy(showMoveSheet = false) }
            is BookshelfIntent.MoveSelected -> moveSelected(intent.groupId)
            BookshelfIntent.RequestDelete -> _uiState.update {
                it.copy(pendingDeleteIds = it.selectedBookIds)
            }
            BookshelfIntent.DismissDelete -> _uiState.update {
                it.copy(pendingDeleteIds = kotlinx.collections.immutable.persistentSetOf())
            }
            is BookshelfIntent.ConfirmDelete -> deleteSelected(intent.deleteOriginal)
            BookshelfIntent.NavigateToLocalImport -> _effects.tryEmit(BookshelfEffect.OpenLocalImport)
            BookshelfIntent.NavigateToRemoteImport -> _effects.tryEmit(BookshelfEffect.OpenRemoteImport)
            BookshelfIntent.NavigateToGlobalSearch -> _effects.tryEmit(
                BookshelfEffect.OpenGlobalSearch(_uiState.value.searchQuery.trim())
            )
            BookshelfIntent.NavigateToManage -> _effects.tryEmit(
                BookshelfEffect.OpenManage(_uiState.value.selectedGroupId)
            )
        }
    }

    private fun selectGroup(groupId: Long) {
        if (request.value.groupId == groupId) return
        clearSelection()
        request.update { it.copy(groupId = groupId) }
        _uiState.update { it.copy(selectedGroupId = groupId) }
        viewModelScope.launch { preferences.selectGroup(groupId) }
    }

    private fun toggleSearch() {
        _uiState.update { state ->
            if (state.searchVisible) state.copy(searchVisible = false, searchQuery = "")
            else state.copy(searchVisible = true)
        }
        if (!_uiState.value.searchVisible) request.update { it.copy(searchQuery = "") }
    }

    private fun changeSearchQuery(value: String) {
        _uiState.update { it.copy(searchQuery = value) }
        request.update { it.copy(searchQuery = value) }
    }

    private fun changeSort(sort: io.legado.app.feature.bookshelf.api.BookshelfSort) {
        _uiState.update { it.copy(sort = sort) }
        request.update { it.copy(sort = sort) }
    }

    private fun openBook(bookId: String) {
        if (_uiState.value.isSelectionMode) {
            toggleSelection(bookId)
            return
        }
        val book = latestSnapshot?.books?.firstOrNull { it.id == bookId } ?: return
        _effects.tryEmit(BookshelfEffect.OpenBook(book.toOpenRequest()))
    }

    private fun openBookInfo(bookId: String) {
        val book = latestSnapshot?.books?.firstOrNull { it.id == bookId } ?: return
        _effects.tryEmit(BookshelfEffect.OpenBookInfo(book.toOpenRequest()))
    }

    private fun toggleSelection(bookId: String) {
        _uiState.update { state ->
            val selected = if (bookId in state.selectedBookIds) {
                state.selectedBookIds - bookId
            } else {
                state.selectedBookIds + bookId
            }
            state.copy(selectedBookIds = selected.toImmutableSet())
        }
    }

    private fun clearSelection() {
        _uiState.update {
            it.copy(
                selectedBookIds = kotlinx.collections.immutable.persistentSetOf(),
                pendingDeleteIds = kotlinx.collections.immutable.persistentSetOf(),
                showMoveSheet = false,
            )
        }
    }

    private fun moveSelected(groupId: Long) = runCommand {
        val selected = _uiState.value.selectedBookIds
        _uiState.update { it.copy(showMoveSheet = false) }
        commands.moveBooks(selected, groupId)
    }

    private fun deleteSelected(deleteOriginal: Boolean) = runCommand {
        val selected = _uiState.value.pendingDeleteIds
        _uiState.update {
            it.copy(pendingDeleteIds = kotlinx.collections.immutable.persistentSetOf())
        }
        commands.deleteBooks(selected, deleteOriginal)
    }

    private fun runCommand(block: suspend () -> BookshelfCommandResult) {
        if (_uiState.value.commandInFlight) return
        _uiState.update { it.copy(commandInFlight = true) }
        viewModelScope.launch {
            val result = try {
                block()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                BookshelfCommandResult.Failure(BookshelfError.Unexpected(throwable.message))
            }
            when (result) {
                is BookshelfCommandResult.Success -> {
                    clearSelection()
                    _effects.emit(BookshelfEffect.ShowMessage(BookshelfMessage.Success))
                }
                is BookshelfCommandResult.Partial -> {
                    _uiState.update { state ->
                        state.copy(
                            selectedBookIds = (state.selectedBookIds - result.changedBookIds)
                                .toImmutableSet()
                        )
                    }
                    _effects.emit(
                        BookshelfEffect.ShowMessage(
                            BookshelfMessage.Partial(result.changedBookIds.size, result.failed.size)
                        )
                    )
                }
                is BookshelfCommandResult.Failure -> _effects.emit(
                    BookshelfEffect.ShowMessage(BookshelfMessage.Failure(result.error))
                )
            }
            _uiState.update { it.copy(commandInFlight = false) }
        }
    }

    private fun applyQueryState(state: BookshelfQueryState) {
        when (state) {
            BookshelfQueryState.Loading -> _uiState.update {
                it.copy(contentState = BookshelfContentState.Loading)
            }
            is BookshelfQueryState.Data -> {
                latestSnapshot = state.snapshot
                val books = state.snapshot.books.map(BookshelfBookSummary::toUi).toImmutableList()
                _uiState.update { current ->
                    current.copy(
                        contentState = if (books.isEmpty()) {
                            BookshelfContentState.Empty
                        } else {
                            BookshelfContentState.Content
                        },
                        books = books,
                        groups = state.snapshot.groups.map {
                            BookshelfGroupUi(it.id, it.name, it.isUserGroup)
                        }.toImmutableList(),
                        selectedGroupId = state.snapshot.selectedGroupId,
                        selectedBookIds = current.selectedBookIds
                            .intersect(books.mapTo(hashSetOf()) { it.id })
                            .toImmutableSet(),
                        sort = request.value.sort,
                        descending = request.value.descending,
                    )
                }
            }
            is BookshelfQueryState.Failure -> {
                state.previous?.let { latestSnapshot = it }
                _uiState.update { current ->
                    val previousBooks = state.previous?.books
                        ?.map(BookshelfBookSummary::toUi)
                        ?.toImmutableList()
                    if (previousBooks != null && previousBooks.isNotEmpty()) {
                        current.copy(
                            contentState = BookshelfContentState.Content,
                            books = previousBooks,
                        )
                    } else {
                        current.copy(
                            contentState = BookshelfContentState.Error(
                                retryable = state.error is BookshelfError.Retryable
                            ),
                            books = kotlinx.collections.immutable.persistentListOf(),
                        )
                    }
                }
                if (state.previous != null) {
                    _effects.tryEmit(BookshelfEffect.ShowMessage(BookshelfMessage.Failure(state.error)))
                }
            }
        }
    }
}

private fun BookshelfBookSummary.toUi() = BookshelfBookUi(
    id = id,
    name = name,
    author = author,
    coverUrl = coverUrl,
    chapterTitle = currentChapterTitle ?: latestChapterTitle,
    unreadChapterCount = unreadChapterCount,
    readingProgress = readingProgress,
    isLocal = isLocal,
    isAudio = isAudio,
    isImage = isImage,
    origin = origin,
)

private fun BookshelfBookSummary.toOpenRequest() = BookshelfOpenBookRequest(
    id = id,
    name = name,
    author = author,
    origin = origin,
    coverUrl = coverUrl,
    isLocal = isLocal,
    isAudio = isAudio,
    isImage = isImage,
)
