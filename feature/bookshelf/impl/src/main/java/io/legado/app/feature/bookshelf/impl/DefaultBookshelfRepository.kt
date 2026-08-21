package io.legado.app.feature.bookshelf.impl

import io.legado.app.data.model.BookshelfBookRecord
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow

/**
 * The bookshelf business implementation. Room is the single source of truth: every command writes
 * through [BookshelfStore] and the UI observes the resulting Room flow, never an optimistic copy.
 */
internal class DefaultBookshelfRepository(
    private val store: BookshelfStore,
    private val preferencesHost: BookshelfPreferencesHost,
    private val removalHost: BookshelfBookRemovalHost,
) : BookshelfQuery, BookshelfPreferencesGateway, BookshelfCommands, BookshelfGroupCommands {

    override val current: BookshelfPreferences get() = preferencesHost.current

    override val preferences: Flow<BookshelfPreferences> = preferencesHost.preferences

    override suspend fun selectGroup(groupId: Long) = preferencesHost.selectGroup(groupId)

    override fun observeBookshelf(request: BookshelfQueryRequest): Flow<BookshelfQueryState> = flow {
        var previous: BookshelfSnapshot? = null
        emit(BookshelfQueryState.Loading)
        try {
            combine(
                store.observeBooks(request.groupId),
                store.observeVisibleGroups(),
            ) { books, groups ->
                val query = request.searchQuery.trim()
                val visible = books.sortedForShelf(request.sort, request.descending)
                    .let { sorted -> if (query.isEmpty()) sorted else sorted.filter { it.matches(query) } }
                BookshelfSnapshot(
                    selectedGroupId = request.groupId,
                    groups = groups,
                    books = visible.map(BookshelfBookRecord::toFeatureSummary),
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

    override suspend fun moveBooks(bookIds: Set<String>, groupId: Long): BookshelfCommandResult {
        if (bookIds.isEmpty()) return BookshelfCommandResult.Success()
        if (groupId < 0) return BookshelfCommandResult.Failure(
            BookshelfError.InvalidRequest("A user group id must be non-negative")
        )
        return commandResult {
            store.moveToGroup(bookIds, groupId)
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
        return commandResult {
            val existing = store.bookOrders(orderedBookIds.toSet())
            require(existing.size == orderedBookIds.size) { "One or more books were not found" }
            val maxOrder = orderedBookIds.size
            val orders = orderedBookIds.withIndex().associate { (index, bookId) ->
                bookId to if (descending) maxOrder - index else index + 1
            }
            store.applyBookOrders(orders)
            orders.keys
        }
    }

    override suspend fun deleteBooks(
        bookIds: Set<String>,
        deleteOriginal: Boolean,
    ): BookshelfCommandResult {
        if (bookIds.isEmpty()) return BookshelfCommandResult.Success()
        return try {
            preferencesHost.setDeleteOriginalDefault(deleteOriginal)
            val books = store.deletableBooks(bookIds)
            books.forEach { book ->
                removalHost.onBookRemoved(book.bookUrl, book.isLocal, deleteOriginal)
                store.deleteChaptersOf(book.bookUrl)
            }
            val removed = books.mapTo(linkedSetOf()) { it.bookUrl }
            store.deleteBooks(removed)
            BookshelfCommandResult.Success(removed)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            val remaining = bookIds.filterTo(linkedSetOf()) { store.bookExists(it) }
            classifyDeleteFailure(bookIds, remaining, throwable.toFeatureError())
        }
    }

    override suspend fun createGroup(draft: BookshelfGroupDraft): BookshelfCommandResult {
        if (draft.name.isBlank()) return BookshelfCommandResult.Failure(
            BookshelfError.InvalidRequest("Group name must not be blank")
        )
        return commandResult {
            store.insertGroup(
                draft.copy(name = draft.name.trim()),
                bookSort = draft.sort?.let(BookshelfSortCodec::toStored) ?: -1,
            )
            emptySet()
        }
    }

    override suspend fun updateGroup(group: BookshelfGroup): BookshelfCommandResult = commandResult {
        store.updateGroup(group, bookSort = group.bookSort?.let(BookshelfSortCodec::toStored) ?: -1)
        emptySet()
    }

    override suspend fun deleteGroup(groupId: Long): BookshelfCommandResult {
        if (groupId <= 0) return BookshelfCommandResult.Failure(
            BookshelfError.InvalidRequest("System groups cannot be deleted")
        )
        return commandResult {
            store.deleteGroup(groupId)
            emptySet()
        }
    }

    override suspend fun reorderGroups(orderedGroupIds: List<Long>): BookshelfCommandResult {
        if (orderedGroupIds.any { it <= 0 } ||
            orderedGroupIds.distinct().size != orderedGroupIds.size
        ) {
            return BookshelfCommandResult.Failure(
                BookshelfError.InvalidRequest("Only unique user groups can be reordered")
            )
        }
        if (orderedGroupIds.isEmpty()) return BookshelfCommandResult.Success()
        return commandResult {
            val existing = store.groupOrders(orderedGroupIds.toSet())
            require(existing.size == orderedGroupIds.size) { "One or more book groups were not found" }
            store.applyGroupOrders(
                orderedGroupIds.withIndex().associate { (index, groupId) -> groupId to index }
            )
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
