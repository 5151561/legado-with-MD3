package io.legado.app.feature.bookshelf.impl

import androidx.room.withTransaction
import io.legado.app.data.AppDatabase
import io.legado.app.data.entities.BookGroup
import io.legado.app.data.model.BookshelfBookRecord
import io.legado.app.feature.bookshelf.api.BookshelfGroup
import io.legado.app.feature.bookshelf.api.BookshelfGroupDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Room-backed bookshelf persistence. Reads stay on the Room flows so a write is only observable
 * after the SSOT emits it; every blocking DAO call runs off the main thread.
 */
internal class RoomBookshelfStore(private val database: AppDatabase) : BookshelfStore {

    private val bookDao get() = database.bookDao
    private val groupDao get() = database.bookGroupDao
    private val chapterDao get() = database.bookChapterDao
    private val tagGroupRuleDao get() = database.tagGroupRuleDao

    override fun observeBooks(groupId: Long): Flow<List<BookshelfBookRecord>> =
        bookDao.flowBookShelfByGroup(groupId)

    override fun observeVisibleGroups(): Flow<List<BookshelfGroup>> =
        groupDao.flowShow().map { groups -> groups.map(BookGroup::toFeatureGroup) }

    override suspend fun bookExists(bookId: String): Boolean = io { bookDao.getBook(bookId) != null }

    override suspend fun moveToGroup(bookIds: Set<String>, groupId: Long) = io {
        val books = bookDao.books(bookIds).filter { it.group != groupId }
        if (books.isNotEmpty()) {
            bookDao.update(*books.map { it.copy(group = groupId) }.toTypedArray())
        }
    }

    override suspend fun bookOrders(bookIds: Set<String>): Map<String, Int> = io {
        bookDao.books(bookIds).associate { it.bookUrl to it.order }
    }

    override suspend fun applyBookOrders(orders: Map<String, Int>) = io {
        val books = bookDao.books(orders.keys).mapNotNull { book ->
            orders[book.bookUrl]?.takeIf { it != book.order }?.let { book.copy(order = it) }
        }
        if (books.isNotEmpty()) bookDao.update(*books.toTypedArray())
    }

    override suspend fun deletableBooks(bookIds: Set<String>): List<BookshelfDeletableBook> = io {
        bookDao.books(bookIds).map { BookshelfDeletableBook(it.bookUrl, it.isLocalBook()) }
    }

    override suspend fun deleteChaptersOf(bookId: String) = io { chapterDao.delByBook(bookId) }

    override suspend fun deleteBooks(bookIds: Set<String>) = io {
        val books = bookDao.books(bookIds)
        if (books.isNotEmpty()) bookDao.delete(*books.toTypedArray())
    }

    override suspend fun insertGroup(draft: BookshelfGroupDraft, bookSort: Int) = transaction {
        val groupId = groupDao.getUnusedId()
        if (groupDao.getByID(groupId) == null) bookDao.removeGroup(groupId)
        groupDao.insert(
            BookGroup(
                groupId = groupId,
                groupName = draft.name,
                cover = draft.coverUrl,
                bookSort = bookSort,
                enableRefresh = draft.isRefreshEnabled,
                isPrivate = draft.isPrivate,
                order = groupDao.maxOrder.plus(1),
            )
        )
    }

    override suspend fun updateGroup(group: BookshelfGroup, bookSort: Int) = transaction {
        groupDao.update(
            BookGroup(
                groupId = group.id,
                groupName = group.name,
                cover = group.coverUrl,
                order = group.order,
                enableRefresh = group.isRefreshEnabled,
                show = group.isVisible,
                bookSort = bookSort,
                isPrivate = group.isPrivate,
            )
        )
    }

    override suspend fun deleteGroup(groupId: Long) = transaction {
        val group = groupDao.getByID(groupId) ?: return@transaction
        tagGroupRuleDao.getByGroupName(group.groupName)?.let { tagGroupRuleDao.delete(it) }
        bookDao.removeGroup(groupId)
        groupDao.delete(group)
    }

    override suspend fun groupOrders(groupIds: Set<Long>): Map<Long, Int> = io {
        groupIds.mapNotNull { groupDao.getByID(it) }.associate { it.groupId to it.order }
    }

    override suspend fun applyGroupOrders(orders: Map<Long, Int>) = io {
        val groups = orders.keys.mapNotNull { id ->
            groupDao.getByID(id)?.let { group -> group.copy(order = orders.getValue(id)) }
        }
        if (groups.isNotEmpty()) groupDao.update(*groups.toTypedArray())
    }

    private suspend fun <T> io(block: suspend () -> T): T = withContext(Dispatchers.IO) { block() }

    private suspend fun <T> transaction(block: suspend () -> T): T =
        withContext(Dispatchers.IO) { database.withTransaction { block() } }
}

private fun io.legado.app.data.dao.BookDao.books(bookIds: Set<String>) =
    if (bookIds.isEmpty()) emptyList() else bookIds.mapNotNull { getBook(it) }

private fun BookGroup.toFeatureGroup() = BookshelfGroup(
    id = groupId,
    name = groupName,
    order = order,
    coverUrl = cover,
    bookSort = bookSort.takeIf { it >= 0 }?.let(BookshelfSortCodec::fromStored),
    isUserGroup = groupId > 0,
    isPrivate = isPrivate,
    isRefreshEnabled = enableRefresh,
    isVisible = show,
)
