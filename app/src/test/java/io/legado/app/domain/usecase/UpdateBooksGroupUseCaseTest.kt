package io.legado.app.domain.usecase

import io.legado.app.domain.model.BookGroupAssignment
import io.legado.app.domain.model.CacheableBook
import io.legado.app.domain.model.DeletableBook
import io.legado.app.domain.repository.BookDomainRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateBooksGroupUseCaseTest {

    @Test
    fun `empty selection performs no lookup or write`() = runTest {
        val repository = RecordingBookRepository(emptyList())
        val useCase = UpdateBooksGroupUseCase(repository)

        useCase.replaceGroup(emptySet(), groupId = 8L)

        assertEquals(0, repository.lookupCount)
        assertTrue(repository.updates.isEmpty())
    }

    @Test
    fun `replace group writes only changed assignments`() = runTest {
        val repository = RecordingBookRepository(
            listOf(
                BookGroupAssignment("already-there", group = 8L),
                BookGroupAssignment("move-me", group = 2L),
            )
        )
        val useCase = UpdateBooksGroupUseCase(repository)

        useCase.replaceGroup(setOf("already-there", "move-me"), groupId = 8L)

        assertEquals(1, repository.lookupCount)
        assertEquals(
            listOf(BookGroupAssignment("move-me", group = 8L)),
            repository.updates.single(),
        )
    }

    @Test
    fun `transform receives the persisted group for every resolved book`() = runTest {
        val repository = RecordingBookRepository(
            listOf(
                BookGroupAssignment("first", group = 1L),
                BookGroupAssignment("second", group = 4L),
            )
        )
        val useCase = UpdateBooksGroupUseCase(repository)

        useCase.updateGroups(setOf("first", "second")) { current -> current + 10L }

        assertEquals(
            listOf(
                BookGroupAssignment("first", group = 11L),
                BookGroupAssignment("second", group = 14L),
            ),
            repository.updates.single(),
        )
    }

    private class RecordingBookRepository(
        private val assignments: List<BookGroupAssignment>,
    ) : BookDomainRepository {
        var lookupCount = 0
        val updates = mutableListOf<List<BookGroupAssignment>>()

        override suspend fun getBookGroupAssignments(
            bookUrls: Set<String>,
        ): List<BookGroupAssignment> {
            lookupCount += 1
            return assignments
        }

        override suspend fun updateBookGroups(assignments: List<BookGroupAssignment>) {
            updates += assignments
        }

        override suspend fun getCacheableBooks(bookUrls: Set<String>): List<CacheableBook> =
            error("Not used")

        override suspend fun getDeletableBooks(bookUrls: Set<String>): List<DeletableBook> =
            error("Not used")

        override suspend fun removeGroupFromBooks(groupId: Long) = error("Not used")
        override suspend fun deleteBooks(bookUrls: Set<String>) = error("Not used")
        override suspend fun deleteChaptersByBook(bookUrl: String) = error("Not used")
    }
}
