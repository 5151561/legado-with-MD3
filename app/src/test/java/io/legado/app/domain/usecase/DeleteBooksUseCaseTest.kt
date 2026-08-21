package io.legado.app.domain.usecase

import io.legado.app.domain.gateway.BookSourceCallbackGateway
import io.legado.app.domain.gateway.LocalBookGateway
import io.legado.app.domain.model.BookGroupAssignment
import io.legado.app.domain.model.CacheableBook
import io.legado.app.domain.model.DeletableBook
import io.legado.app.domain.repository.BookDomainRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteBooksUseCaseTest {

    @Test
    fun `empty selection performs no work`() = runTest {
        val fixture = Fixture()

        val deleted = fixture.useCase.execute(emptySet(), deleteOriginal = true)

        assertTrue(deleted.isEmpty())
        assertTrue(fixture.repository.events.isEmpty())
        assertTrue(fixture.localBooks.events.isEmpty())
        assertTrue(fixture.sourceCallbacks.events.isEmpty())
    }

    @Test
    fun `deletes resolved local and remote books through their compatibility callbacks`() = runTest {
        val fixture = Fixture(
            deletableBooks = listOf(
                DeletableBook("local", "local", isLocal = true),
                DeletableBook("remote", "source", isLocal = false),
            )
        )

        val deleted = fixture.useCase.execute(
            setOf("local", "remote"),
            deleteOriginal = true,
        )

        assertEquals(listOf("local", "remote"), deleted)
        assertEquals(listOf("deleteLocal:local:true"), fixture.localBooks.events)
        assertEquals(listOf("deleteRemote:remote"), fixture.sourceCallbacks.events)
        assertEquals(
            listOf(
                "lookup:local,remote",
                "deleteChapters:local",
                "deleteChapters:remote",
                "deleteBooks:local,remote",
            ),
            fixture.repository.events,
        )
    }

    @Test
    fun `returns and deletes only books resolved as deletable`() = runTest {
        val fixture = Fixture(
            deletableBooks = listOf(
                DeletableBook("found", "source", isLocal = false),
            )
        )

        val deleted = fixture.useCase.execute(
            setOf("found", "missing"),
            deleteOriginal = false,
        )

        assertEquals(listOf("found"), deleted)
        assertEquals(listOf("deleteRemote:found"), fixture.sourceCallbacks.events)
        assertEquals("deleteBooks:found", fixture.repository.events.last())
    }

    private class Fixture(deletableBooks: List<DeletableBook> = emptyList()) {
        val repository = RecordingBookRepository(deletableBooks)
        val localBooks = RecordingLocalBookGateway()
        val sourceCallbacks = RecordingBookSourceCallbackGateway()
        val useCase = DeleteBooksUseCase(repository, localBooks, sourceCallbacks)
    }

    private class RecordingBookRepository(
        private val deletableBooks: List<DeletableBook>,
    ) : BookDomainRepository {
        val events = mutableListOf<String>()

        override suspend fun getDeletableBooks(bookUrls: Set<String>): List<DeletableBook> {
            val ids = bookUrls.sorted().joinToString(",")
            events += "lookup:$ids"
            return deletableBooks
        }

        override suspend fun deleteChaptersByBook(bookUrl: String) {
            events += "deleteChapters:$bookUrl"
        }

        override suspend fun deleteBooks(bookUrls: Set<String>) {
            val ids = bookUrls.sorted().joinToString(",")
            events += "deleteBooks:$ids"
        }

        override suspend fun getCacheableBooks(bookUrls: Set<String>): List<CacheableBook> =
            error("Not used")

        override suspend fun getBookGroupAssignments(
            bookUrls: Set<String>,
        ): List<BookGroupAssignment> = error("Not used")

        override suspend fun updateBookGroups(assignments: List<BookGroupAssignment>) =
            error("Not used")

        override suspend fun removeGroupFromBooks(groupId: Long) = error("Not used")
    }

    private class RecordingLocalBookGateway : LocalBookGateway {
        val events = mutableListOf<String>()

        override suspend fun deleteBook(bookUrl: String, deleteOriginal: Boolean) {
            events += "deleteLocal:$bookUrl:$deleteOriginal"
        }
    }

    private class RecordingBookSourceCallbackGateway : BookSourceCallbackGateway {
        val events = mutableListOf<String>()

        override suspend fun onDeleteFromShelf(bookUrl: String) {
            events += "deleteRemote:$bookUrl"
        }
    }
}
