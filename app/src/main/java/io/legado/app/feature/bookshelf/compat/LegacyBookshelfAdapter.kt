package io.legado.app.feature.bookshelf.compat

import io.legado.app.data.entities.BookGroup
import io.legado.app.data.repository.BookGroupRepository
import io.legado.app.data.repository.BookRepository
import io.legado.app.data.repository.BookshelfRepository
import io.legado.app.domain.gateway.BookGroupMutationGateway
import io.legado.app.domain.gateway.BookshelfDeleteOriginalGateway
import io.legado.app.domain.gateway.BookshelfSettingsGateway
import io.legado.app.domain.model.BookGroupUpdate
import io.legado.app.domain.model.NewBookGroup
import io.legado.app.domain.usecase.DeleteBooksUseCase
import io.legado.app.domain.usecase.ReorderBookGroupsUseCase
import io.legado.app.domain.usecase.ReorderBooksUseCase
import io.legado.app.domain.usecase.UpdateBooksGroupUseCase
import io.legado.app.feature.bookshelf.api.BookshelfBookSummary
import io.legado.app.feature.bookshelf.api.BookshelfCommandResult
import io.legado.app.feature.bookshelf.api.BookshelfCommands
import io.legado.app.feature.bookshelf.api.BookshelfError
import io.legado.app.feature.bookshelf.api.BookshelfGroup
import io.legado.app.feature.bookshelf.api.BookshelfGroupCommands
import io.legado.app.feature.bookshelf.api.BookshelfGroupDraft
import io.legado.app.feature.bookshelf.api.BookshelfPreferences
import io.legado.app.feature.bookshelf.api.BookshelfPreferencesGateway
import io.legado.app.feature.bookshelf.api.BookshelfQuery
import io.legado.app.feature.bookshelf.api.BookshelfQueryRequest
import io.legado.app.feature.bookshelf.api.BookshelfQueryState
import io.legado.app.feature.bookshelf.api.BookshelfSnapshot
import io.legado.app.feature.bookshelf.api.BookshelfSort
import io.legado.app.data.model.BookshelfBookRecord
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Phase 2 compatibility seam. It may only translate feature contracts to the listed legacy
 * repositories/use cases. Delete it after the Room/query seam can move to bookshelf:impl.
 */
class LegacyBookshelfAdapter(
    private val bookRepository: BookRepository,
    private val groupRepository: BookGroupRepository,
    private val sortingRepository: BookshelfRepository,
    private val settingsGateway: BookshelfSettingsGateway,
    private val deleteOriginalGateway: BookshelfDeleteOriginalGateway,
    private val updateBooksGroup: UpdateBooksGroupUseCase,
    private val deleteBooks: DeleteBooksUseCase,
    private val reorderBooks: ReorderBooksUseCase,
    private val groupMutation: BookGroupMutationGateway,
    private val reorderGroups: ReorderBookGroupsUseCase,
) : BookshelfQuery, BookshelfPreferencesGateway, BookshelfCommands, BookshelfGroupCommands {

    override val current: BookshelfPreferences
        get() = settingsGateway.currentSettings.toFeaturePreferences(deleteOriginalGateway.current)

    override val preferences: Flow<BookshelfPreferences> = settingsGateway.settings
        .map { it.toFeaturePreferences(deleteOriginalGateway.current) }

    override suspend fun selectGroup(groupId: Long) {
        settingsGateway.update { it.copy(saveTabPosition = groupId) }
    }

    override fun observeBookshelf(request: BookshelfQueryRequest): Flow<BookshelfQueryState> = flow {
        var previous: BookshelfSnapshot? = null
        emit(BookshelfQueryState.Loading)
        try {
            combine(
                bookRepository.flowBookShelfByGroup(request.groupId),
                groupRepository.flowShow(),
            ) { books, groups ->
                val sorted = sortingRepository.sortBooks(
                    books,
                    null,
                    request.sort.toLegacySort(),
                    if (request.descending) 1 else 0,
                )
                val query = request.searchQuery.trim()
                val visibleBooks = if (query.isEmpty()) sorted else sorted.filter { it.matches(query) }
                BookshelfSnapshot(
                    selectedGroupId = request.groupId,
                    groups = groups.map(BookGroup::toFeatureGroup),
                    books = visibleBooks.map(BookshelfBookRecord::toFeatureSummary),
                )
            }.collect { snapshot ->
                previous = snapshot
                emit(BookshelfQueryState.Data(snapshot))
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            emit(BookshelfQueryState.Failure(throwable.toFeatureError(), previous))
        }
    }

    override suspend fun moveBooks(
        bookIds: Set<String>,
        groupId: Long,
    ): BookshelfCommandResult {
        if (bookIds.isEmpty()) return BookshelfCommandResult.Success()
        if (groupId < 0) return BookshelfCommandResult.Failure(
            BookshelfError.InvalidRequest("A user group id must be non-negative")
        )
        return commandResult {
            updateBooksGroup.replaceGroup(bookIds, groupId)
            bookIds
        }
    }

    override suspend fun reorderBooks(
        groupId: Long,
        orderedBookIds: List<String>,
        descending: Boolean,
    ): BookshelfCommandResult {
        if (orderedBookIds.isEmpty()) return BookshelfCommandResult.Success()
        if (orderedBookIds.distinct().size != orderedBookIds.size) {
            return BookshelfCommandResult.Failure(
                BookshelfError.InvalidRequest("Duplicate book ids in manual order")
            )
        }
        return commandResult { reorderBooks.execute(orderedBookIds, descending) }
    }

    override suspend fun deleteBooks(
        bookIds: Set<String>,
        deleteOriginal: Boolean,
    ): BookshelfCommandResult {
        if (bookIds.isEmpty()) return BookshelfCommandResult.Success()
        return try {
            deleteOriginalGateway.update(deleteOriginal)
            BookshelfCommandResult.Success(deleteBooks.execute(bookIds, deleteOriginal).toSet())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            val remaining = bookIds.filterTo(linkedSetOf()) { bookRepository.getBook(it) != null }
            classifyDeleteFailure(bookIds, remaining, throwable.toFeatureError())
        }
    }

    override suspend fun createGroup(draft: BookshelfGroupDraft): BookshelfCommandResult {
        if (draft.name.isBlank()) return BookshelfCommandResult.Failure(
            BookshelfError.InvalidRequest("Group name must not be blank")
        )
        return commandResult {
            groupMutation.addGroup(
                NewBookGroup(
                    groupName = draft.name.trim(),
                    bookSort = draft.sort?.toLegacySort() ?: -1,
                    enableRefresh = draft.isRefreshEnabled,
                    isPrivate = draft.isPrivate,
                    cover = draft.coverUrl,
                    pattern = null,
                )
            )
            emptySet()
        }
    }

    override suspend fun updateGroup(group: BookshelfGroup): BookshelfCommandResult = commandResult {
        groupMutation.saveGroup(
            BookGroupUpdate(
                groupId = group.id,
                groupName = group.name,
                cover = group.coverUrl,
                order = group.order,
                enableRefresh = group.isRefreshEnabled,
                show = group.isVisible,
                bookSort = group.bookSort?.toLegacySort() ?: -1,
                isPrivate = group.isPrivate,
            ),
            ruleToSave = null,
            ruleIdToDelete = null,
        )
        emptySet()
    }

    override suspend fun deleteGroup(groupId: Long): BookshelfCommandResult {
        if (groupId <= 0) return BookshelfCommandResult.Failure(
            BookshelfError.InvalidRequest("System groups cannot be deleted")
        )
        return commandResult {
            groupMutation.deleteGroup(groupId)
            emptySet()
        }
    }

    override suspend fun reorderGroups(orderedGroupIds: List<Long>): BookshelfCommandResult {
        if (orderedGroupIds.any { it <= 0 } || orderedGroupIds.distinct().size != orderedGroupIds.size) {
            return BookshelfCommandResult.Failure(
                BookshelfError.InvalidRequest("Only unique user groups can be reordered")
            )
        }
        return commandResult {
            reorderGroups.execute(orderedGroupIds)
            emptySet()
        }
    }

    private suspend fun commandResult(block: suspend () -> Set<String>): BookshelfCommandResult = try {
        BookshelfCommandResult.Success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        BookshelfCommandResult.Failure(throwable.toFeatureError())
    }
}

private fun io.legado.app.domain.model.settings.BookshelfSettings.toFeaturePreferences(
    deleteOriginalDefault: Boolean,
) =
    BookshelfPreferences(
        selectedGroupId = saveTabPosition,
        defaultSort = bookshelfSort.toFeatureSort(),
        descending = bookshelfSortOrder == 1,
        deleteOriginalDefault = deleteOriginalDefault,
    )

private fun BookGroup.toFeatureGroup() = BookshelfGroup(
    id = groupId,
    name = groupName,
    order = order,
    coverUrl = cover,
    bookSort = bookSort.takeIf { it >= 0 }?.toFeatureSort(),
    isUserGroup = groupId > 0,
    isPrivate = isPrivate,
    isRefreshEnabled = enableRefresh,
    isVisible = show,
)

internal fun BookshelfBookRecord.toFeatureSummary(): BookshelfBookSummary {
    val lastChapterIndex = (totalChapterNum - 1).coerceAtLeast(0)
    val progress = if (lastChapterIndex == 0) 0f else {
        (durChapterIndex.coerceIn(0, lastChapterIndex).toFloat() / lastChapterIndex).coerceIn(0f, 1f)
    }
    return BookshelfBookSummary(
        id = bookUrl,
        name = name,
        author = author,
        origin = origin,
        originName = originName,
        coverUrl = getDisplayCover(),
        currentChapterTitle = durChapterTitle,
        latestChapterTitle = latestChapterTitle,
        currentChapterIndex = durChapterIndex,
        totalChapterCount = totalChapterNum,
        unreadChapterCount = getUnreadChapterNum(),
        readingProgress = progress,
        lastReadAt = durChapterTime,
        latestChapterAt = latestChapterTime,
        groupMask = group,
        order = order,
        isLocal = isLocal,
        isAudio = isAudio,
        isImage = isImage,
    )
}

private fun Int.toFeatureSort(): BookshelfSort = when (this) {
    1 -> BookshelfSort.LatestChapter
    2 -> BookshelfSort.BookName
    3 -> BookshelfSort.Manual
    4 -> BookshelfSort.LastActivity
    5 -> BookshelfSort.Author
    else -> BookshelfSort.RecentReading
}

private fun BookshelfSort.toLegacySort(): Int = when (this) {
    BookshelfSort.RecentReading -> 0
    BookshelfSort.LatestChapter -> 1
    BookshelfSort.BookName -> 2
    BookshelfSort.Manual -> 3
    BookshelfSort.LastActivity -> 4
    BookshelfSort.Author -> 5
}

private fun BookshelfBookRecord.matches(query: String): Boolean =
    name.contains(query, ignoreCase = true) ||
        author.contains(query, ignoreCase = true) ||
        originName.contains(query, ignoreCase = true) ||
        customTag.orEmpty().contains(query, ignoreCase = true) ||
        kind.orEmpty().contains(query, ignoreCase = true)

private fun Throwable.toFeatureError(): BookshelfError = when (this) {
    is SecurityException -> BookshelfError.PermissionDenied(message)
    is IllegalArgumentException -> BookshelfError.InvalidRequest(message)
    is IOException -> BookshelfError.Retryable(message)
    else -> BookshelfError.Unexpected(message)
}

internal fun classifyDeleteFailure(
    requested: Set<String>,
    remaining: Set<String>,
    error: BookshelfError,
): BookshelfCommandResult {
    val changed = requested - remaining
    return when {
        changed.isEmpty() -> BookshelfCommandResult.Failure(error)
        remaining.isEmpty() -> BookshelfCommandResult.Success(changed)
        else -> BookshelfCommandResult.Partial(
            changedBookIds = changed,
            failed = remaining.associateWith { error },
        )
    }
}
