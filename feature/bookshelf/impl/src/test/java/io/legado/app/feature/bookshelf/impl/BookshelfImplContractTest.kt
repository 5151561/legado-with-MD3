package io.legado.app.feature.bookshelf.impl

import io.legado.app.feature.bookshelf.api.BookshelfCommandResult
import io.legado.app.feature.bookshelf.api.BookshelfError
import io.legado.app.feature.bookshelf.api.BookshelfGroup
import io.legado.app.feature.bookshelf.api.BookshelfGroupDraft
import io.legado.app.feature.bookshelf.api.BookshelfQueryRequest
import io.legado.app.feature.bookshelf.api.BookshelfQueryState
import io.legado.app.feature.bookshelf.api.BookshelfSort
import java.io.IOException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The bookshelf API contract, executed against the formal implementation. These cases previously
 * ran against the deleted `LegacyBookshelfAdapter`; the expectations are unchanged.
 */
class BookshelfImplContractTest {

    private val store = FakeBookshelfStore()
    private val preferences = FakePreferencesHost()
    private val removals = RecordingRemovalHost()
    private val bookshelf = DefaultBookshelfRepository(store, preferences, removals)

    @Test
    fun `query emits loading before the first SSOT snapshot`() = runTest {
        store.books.value = listOf(bookRecord("a"))

        val states = bookshelf.observeBookshelf(BookshelfQueryRequest()).take(2).toList()

        assertEquals(BookshelfQueryState.Loading, states.first())
        val data = states[1] as BookshelfQueryState.Data
        assertEquals(listOf("a"), data.snapshot.books.map { it.id })
    }

    @Test
    fun `an empty shelf is data, not a failure`() = runTest {
        val state = bookshelf.observeBookshelf(BookshelfQueryRequest()).take(2).toList()[1]

        assertTrue(state is BookshelfQueryState.Data)
        assertEquals(emptyList<String>(), (state as BookshelfQueryState.Data).snapshot.books)
    }

    @Test
    fun `a query error is retryable and keeps the previous snapshot`() = runTest {
        store.booksFlow = flow {
            emit(listOf(bookRecord("a")))
            throw IOException("offline")
        }

        val states = bookshelf.observeBookshelf(BookshelfQueryRequest()).take(3).toList()

        val failure = states[2] as BookshelfQueryState.Failure
        assertTrue(failure.error is BookshelfError.Retryable)
        assertEquals(listOf("a"), failure.previous?.books?.map { it.id })
    }

    @Test
    fun `search filters the sorted snapshot without touching the SSOT`() = runTest {
        store.books.value = listOf(bookRecord("a", name = "Dune"), bookRecord("b", name = "Emma"))

        val state = bookshelf
            .observeBookshelf(BookshelfQueryRequest(searchQuery = " dun "))
            .take(2).toList()[1] as BookshelfQueryState.Data

        assertEquals(listOf("a"), state.snapshot.books.map { it.id })
        assertEquals(2, store.books.value.size)
    }

    @Test
    fun `manual sort honours the requested direction`() = runTest {
        store.books.value = listOf(bookRecord("a", order = 2), bookRecord("b", order = 1))

        val ascending = bookshelf
            .observeBookshelf(BookshelfQueryRequest(sort = BookshelfSort.Manual, descending = false))
            .take(2).toList()[1] as BookshelfQueryState.Data

        assertEquals(listOf("b", "a"), ascending.snapshot.books.map { it.id })
    }

    @Test
    fun `every sort mode keeps each book exactly once in both directions`() = runTest {
        val shelf = listOf(
            bookRecord("a", name = "阿", author = "李", order = 3, durChapterTime = 30, latestChapterTime = 10),
            bookRecord("b", name = "波", author = "王", order = 1, durChapterTime = 10, latestChapterTime = 30),
            bookRecord("c", name = "刺", author = "张", order = 2, durChapterTime = 20, latestChapterTime = 20),
        )
        store.books.value = shelf
        val expected = shelf.map { it.bookUrl }.sorted()

        BookshelfSort.entries.forEach { sort ->
            listOf(true, false).forEach { descending ->
                val snapshot = bookshelf
                    .observeBookshelf(BookshelfQueryRequest(sort = sort, descending = descending))
                    .take(2).toList()[1] as BookshelfQueryState.Data

                assertEquals(
                    "sort=$sort descending=$descending",
                    expected,
                    snapshot.snapshot.books.map { it.id }.sorted(),
                )
            }
        }
    }

    @Test
    fun `room projection maps reader progress without exposing the record`() {
        val summary = bookRecord("book", durChapterIndex = 4, totalChapterNum = 10)
            .toFeatureSummary()

        assertEquals("book", summary.id)
        assertEquals(5, summary.unreadChapterCount)
        assertEquals(4f / 9f, summary.readingProgress, 0f)
    }

    @Test
    fun `moving books to a system group is rejected before any write`() = runTest {
        store.books.value = listOf(bookRecord("a", group = 0))

        val result = bookshelf.moveBooks(setOf("a"), BookshelfGroup.AllId)

        assertTrue(result is BookshelfCommandResult.Failure)
        assertEquals(0L, store.books.value.single().group)
    }

    @Test
    fun `an empty move is a no-op success`() = runTest {
        assertEquals(BookshelfCommandResult.Success(), bookshelf.moveBooks(emptySet(), 1))
    }

    @Test
    fun `repeating a move keeps the SSOT idempotent`() = runTest {
        store.books.value = listOf(bookRecord("a"))

        bookshelf.moveBooks(setOf("a"), 3)
        val second = bookshelf.moveBooks(setOf("a"), 3)

        assertEquals(BookshelfCommandResult.Success(setOf("a")), second)
        assertEquals(3L, store.books.value.single().group)
    }

    @Test
    fun `a failed move reports the classified error`() = runTest {
        store.books.value = listOf(bookRecord("a"))
        store.failOn = "moveToGroup"
        store.failure = IOException("db busy")

        val result = bookshelf.moveBooks(setOf("a"), 1) as BookshelfCommandResult.Failure

        assertTrue(result.error is BookshelfError.Retryable)
    }

    @Test
    fun `reorder rejects duplicates and assigns dense order values`() = runTest {
        store.books.value = listOf(bookRecord("a"), bookRecord("b"))

        assertTrue(bookshelf.reorderBooks(0, listOf("a", "a"), false) is BookshelfCommandResult.Failure)

        val result = bookshelf.reorderBooks(0, listOf("b", "a"), descending = false)

        assertEquals(BookshelfCommandResult.Success(setOf("b", "a")), result)
        assertEquals(mapOf("b" to 1, "a" to 2), store.books.value.associate { it.bookUrl to it.order })
    }

    @Test
    fun `reordering an unknown book is an invalid request`() = runTest {
        store.books.value = listOf(bookRecord("a"))

        val result = bookshelf.reorderBooks(0, listOf("a", "ghost"), false)

        assertTrue((result as BookshelfCommandResult.Failure).error is BookshelfError.InvalidRequest)
    }

    @Test
    fun `delete removes chapters, runs host side effects and stores the preference`() = runTest {
        store.books.value = listOf(bookRecord("a", type = 0, origin = "loc_book"), bookRecord("b"))

        val result = bookshelf.deleteBooks(setOf("a"), deleteOriginal = true)

        assertEquals(BookshelfCommandResult.Success(setOf("a")), result)
        assertEquals(listOf("a"), store.removedChapters)
        assertEquals(listOf(Triple("a", true, true)), removals.removed)
        assertEquals(listOf("b"), store.books.value.map { it.bookUrl })
        assertTrue(preferences.current.deleteOriginalDefault)
    }

    @Test
    fun `a delete that fails before any removal is a failure`() = runTest {
        store.books.value = listOf(bookRecord("a"))
        store.failOn = "deletableBooks"
        store.failure = SecurityException("denied")

        val result = bookshelf.deleteBooks(setOf("a"), deleteOriginal = false)

        assertTrue((result as BookshelfCommandResult.Failure).error is BookshelfError.PermissionDenied)
    }

    @Test
    fun `a delete that fails after partial removal is partial`() = runTest {
        val error = BookshelfError.Retryable("locked")

        val result = classifyDeleteFailure(setOf("deleted", "remaining"), setOf("remaining"), error)

        assertTrue(result is BookshelfCommandResult.Partial)
        result as BookshelfCommandResult.Partial
        assertEquals(setOf("deleted"), result.changedBookIds)
        assertEquals(setOf("remaining"), result.failed.keys)
    }

    @Test
    fun `group commands validate their inputs before writing`() = runTest {
        assertTrue(bookshelf.createGroup(BookshelfGroupDraft(" ")) is BookshelfCommandResult.Failure)
        assertTrue(bookshelf.deleteGroup(BookshelfGroup.AllId) is BookshelfCommandResult.Failure)
        assertTrue(bookshelf.reorderGroups(listOf(1, 1)) is BookshelfCommandResult.Failure)
        assertTrue(bookshelf.reorderGroups(listOf(1, -2)) is BookshelfCommandResult.Failure)
        assertEquals(emptyList<BookshelfGroup>(), store.groups.value)
    }

    @Test
    fun `creating a group trims the name and keeps the requested sort`() = runTest {
        val result = bookshelf.createGroup(
            BookshelfGroupDraft(name = " Fiction ", sort = BookshelfSort.BookName)
        )

        assertEquals(BookshelfCommandResult.Success(), result)
        val group = store.groups.value.single()
        assertEquals("Fiction", group.name)
        assertEquals(BookshelfSort.BookName, group.bookSort)
    }

    @Test
    fun `reordering groups writes dense ascending order values`() = runTest {
        bookshelf.createGroup(BookshelfGroupDraft("a"))
        bookshelf.createGroup(BookshelfGroupDraft("b"))
        val ids = store.groups.value.map { it.id }

        val result = bookshelf.reorderGroups(ids.reversed())

        assertEquals(BookshelfCommandResult.Success(), result)
        assertEquals(
            mapOf(ids[1] to 0, ids[0] to 1),
            store.groups.value.associate { it.id to it.order },
        )
    }

    @Test
    fun `reordering an unknown group is an invalid request`() = runTest {
        val result = bookshelf.reorderGroups(listOf(42L))

        assertTrue((result as BookshelfCommandResult.Failure).error is BookshelfError.InvalidRequest)
    }

    @Test
    fun `selecting a group is the only preference write the bookshelf owns`() = runTest {
        bookshelf.selectGroup(7)

        assertEquals(7L, bookshelf.current.selectedGroupId)
    }
}
