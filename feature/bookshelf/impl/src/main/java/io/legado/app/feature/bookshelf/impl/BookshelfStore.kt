package io.legado.app.feature.bookshelf.impl

import io.legado.app.data.model.BookshelfBookRecord
import io.legado.app.feature.bookshelf.api.BookshelfGroup
import io.legado.app.feature.bookshelf.api.BookshelfGroupDraft
import kotlinx.coroutines.flow.Flow

internal data class BookshelfDeletableBook(val bookUrl: String, val isLocal: Boolean)

/**
 * Narrow persistence port over the shared Room schema. It exists so the bookshelf command and
 * mapping contracts can be exercised without a device, and has exactly one production
 * implementation ([RoomBookshelfStore]).
 */
internal interface BookshelfStore {
    fun observeBooks(groupId: Long): Flow<List<BookshelfBookRecord>>
    fun observeVisibleGroups(): Flow<List<BookshelfGroup>>

    suspend fun bookExists(bookId: String): Boolean
    suspend fun moveToGroup(bookIds: Set<String>, groupId: Long)
    suspend fun bookOrders(bookIds: Set<String>): Map<String, Int>
    suspend fun applyBookOrders(orders: Map<String, Int>)

    suspend fun deletableBooks(bookIds: Set<String>): List<BookshelfDeletableBook>
    suspend fun deleteChaptersOf(bookId: String)
    suspend fun deleteBooks(bookIds: Set<String>)

    suspend fun insertGroup(draft: BookshelfGroupDraft, bookSort: Int)
    suspend fun updateGroup(group: BookshelfGroup, bookSort: Int)
    suspend fun deleteGroup(groupId: Long)
    suspend fun groupOrders(groupIds: Set<Long>): Map<Long, Int>
    suspend fun applyGroupOrders(orders: Map<Long, Int>)
}
