package io.legado.app.feature.bookshelf.impl

import io.legado.app.constant.BookType
import io.legado.app.data.model.BookshelfBookRecord
import io.legado.app.feature.bookshelf.api.BookshelfGroup
import io.legado.app.feature.bookshelf.api.BookshelfGroupDraft
import io.legado.app.feature.bookshelf.api.BookshelfPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory stand-in for the Room SSOT so the API contract can run on the JVM. */
internal class FakeBookshelfStore(
    books: List<BookshelfBookRecord> = emptyList(),
    groups: List<BookshelfGroup> = emptyList(),
) : BookshelfStore {

    val books = MutableStateFlow(books)
    val groups = MutableStateFlow(groups)
    val removedChapters = mutableListOf<String>()
    var failOn: String? = null
    var failure: Throwable = IllegalStateException("boom")
    var booksFlow: Flow<List<BookshelfBookRecord>> = this.books

    private fun gate(op: String) {
        if (failOn == op) throw failure
    }

    override fun observeBooks(groupId: Long): Flow<List<BookshelfBookRecord>> =
        booksFlow.map { list -> list.filter { groupId < 0 || it.group == groupId } }

    override fun observeVisibleGroups(): Flow<List<BookshelfGroup>> = groups

    override suspend fun bookExists(bookId: String): Boolean =
        books.value.any { it.bookUrl == bookId }

    override suspend fun moveToGroup(bookIds: Set<String>, groupId: Long) {
        gate("moveToGroup")
        books.value = books.value.map {
            if (it.bookUrl in bookIds) it.copy(group = groupId) else it
        }
    }

    override suspend fun bookOrders(bookIds: Set<String>): Map<String, Int> =
        books.value.filter { it.bookUrl in bookIds }.associate { it.bookUrl to it.order }

    override suspend fun applyBookOrders(orders: Map<String, Int>) {
        gate("applyBookOrders")
        books.value = books.value.map { book ->
            orders[book.bookUrl]?.let { book.copy(order = it) } ?: book
        }
    }

    override suspend fun deletableBooks(bookIds: Set<String>): List<BookshelfDeletableBook> {
        gate("deletableBooks")
        return books.value.filter { it.bookUrl in bookIds }
            .map { BookshelfDeletableBook(it.bookUrl, it.isLocalLikeBookEntity()) }
    }

    override suspend fun deleteChaptersOf(bookId: String) {
        gate("deleteChaptersOf")
        removedChapters += bookId
    }

    override suspend fun deleteBooks(bookIds: Set<String>) {
        gate("deleteBooks")
        books.value = books.value.filterNot { it.bookUrl in bookIds }
    }

    override suspend fun insertGroup(draft: BookshelfGroupDraft, bookSort: Int) {
        gate("insertGroup")
        val id = (groups.value.maxOfOrNull { it.id } ?: 0L) + 1
        groups.value = groups.value + BookshelfGroup(
            id = id,
            name = draft.name,
            order = groups.value.size,
            coverUrl = draft.coverUrl,
            bookSort = bookSort.takeIf { it >= 0 }?.let(BookshelfSortCodec::fromStored),
            isPrivate = draft.isPrivate,
            isRefreshEnabled = draft.isRefreshEnabled,
        )
    }

    override suspend fun updateGroup(group: BookshelfGroup, bookSort: Int) {
        gate("updateGroup")
        groups.value = groups.value.map { if (it.id == group.id) group else it }
    }

    override suspend fun deleteGroup(groupId: Long) {
        gate("deleteGroup")
        groups.value = groups.value.filterNot { it.id == groupId }
    }

    override suspend fun groupOrders(groupIds: Set<Long>): Map<Long, Int> =
        groups.value.filter { it.id in groupIds }.associate { it.id to it.order }

    override suspend fun applyGroupOrders(orders: Map<Long, Int>) {
        gate("applyGroupOrders")
        groups.value = groups.value.map { group ->
            orders[group.id]?.let { group.copy(order = it) } ?: group
        }
    }
}

internal class FakePreferencesHost(
    initial: BookshelfPreferences = BookshelfPreferences(),
) : BookshelfPreferencesHost {
    private val state = MutableStateFlow(initial)
    override val current: BookshelfPreferences get() = state.value
    override val preferences: Flow<BookshelfPreferences> = state
    override suspend fun selectGroup(groupId: Long) {
        state.value = state.value.copy(selectedGroupId = groupId)
    }

    override suspend fun setDeleteOriginalDefault(deleteOriginal: Boolean) {
        state.value = state.value.copy(deleteOriginalDefault = deleteOriginal)
    }
}

internal class RecordingRemovalHost : BookshelfBookRemovalHost {
    val removed = mutableListOf<Triple<String, Boolean, Boolean>>()
    override suspend fun onBookRemoved(bookUrl: String, isLocal: Boolean, deleteOriginal: Boolean) {
        removed += Triple(bookUrl, isLocal, deleteOriginal)
    }
}

internal fun bookRecord(
    id: String,
    name: String = id,
    author: String = "author",
    group: Long = 0,
    order: Int = 0,
    durChapterIndex: Int = 0,
    totalChapterNum: Int = 1,
    durChapterTime: Long = 0,
    latestChapterTime: Long = 0,
    type: Int = 0,
    origin: String = "source",
) = BookshelfBookRecord(
    bookUrl = id,
    name = name,
    author = author,
    origin = origin,
    originName = "Source",
    coverUrl = null,
    customCoverUrl = null,
    durChapterTitle = null,
    durChapterTime = durChapterTime,
    durChapterPos = 0,
    latestChapterTitle = null,
    latestChapterTime = latestChapterTime,
    lastCheckCount = 0,
    totalChapterNum = totalChapterNum,
    durChapterIndex = durChapterIndex,
    type = type,
    group = group,
    order = order,
)

/**
 * `RoomBookshelfStore` reads `Book.isLocalBook()`, which falls back to `origin` for the legacy
 * `type == 0` rows. The shelf projection has no such fallback, so the fake mirrors the entity rule.
 */
private fun BookshelfBookRecord.isLocalLikeBookEntity(): Boolean = if (type == 0) {
    origin == BookType.localTag || origin.startsWith(BookType.webDavTag)
} else {
    isLocal
}
