package io.legado.app.domain.usecase

import io.legado.app.domain.model.BookGroupAssignment
import io.legado.app.domain.model.BookOrderAssignment
import io.legado.app.domain.model.CacheableBook
import io.legado.app.domain.model.DeletableBook
import io.legado.app.domain.repository.BookDomainRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReorderBooksUseCaseTest {
    @Test
    fun `descending order preserves visible id sequence`() = runTest {
        val repository = RecordingRepository(setOf("a", "b", "c"))
        val result = ReorderBooksUseCase(repository).execute(listOf("b", "a", "c"), true)

        assertEquals(linkedSetOf("b", "a", "c"), result)
        assertEquals(
            listOf(
                BookOrderAssignment("b", 3),
                BookOrderAssignment("a", 2),
                BookOrderAssignment("c", 1),
            ),
            repository.updated,
        )
    }

    @Test
    fun `duplicate ids fail before a write`() = runTest {
        val repository = RecordingRepository(setOf("a"))
        val failure = runCatching {
            ReorderBooksUseCase(repository).execute(listOf("a", "a"), true)
        }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
        assertEquals(emptyList<BookOrderAssignment>(), repository.updated)
    }

    @Test
    fun `missing id fails without partial order write`() = runTest {
        val repository = RecordingRepository(setOf("a"))
        val failure = runCatching {
            ReorderBooksUseCase(repository).execute(listOf("a", "missing"), false)
        }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
        assertEquals(emptyList<BookOrderAssignment>(), repository.updated)
    }

    private class RecordingRepository(private val ids: Set<String>) : BookDomainRepository {
        var updated: List<BookOrderAssignment> = emptyList()

        override suspend fun getBookOrders(bookUrls: Set<String>) =
            bookUrls.filter { it in ids }.map { BookOrderAssignment(it, 0) }

        override suspend fun updateBookOrders(assignments: List<BookOrderAssignment>) {
            updated = assignments
        }

        override suspend fun getCacheableBooks(bookUrls: Set<String>): List<CacheableBook> = error("unused")
        override suspend fun getDeletableBooks(bookUrls: Set<String>): List<DeletableBook> = error("unused")
        override suspend fun getBookGroupAssignments(bookUrls: Set<String>): List<BookGroupAssignment> = error("unused")
        override suspend fun updateBookGroups(assignments: List<BookGroupAssignment>) = error("unused")
        override suspend fun removeGroupFromBooks(groupId: Long) = error("unused")
        override suspend fun deleteBooks(bookUrls: Set<String>) = error("unused")
        override suspend fun deleteChaptersByBook(bookUrl: String) = error("unused")
    }
}
