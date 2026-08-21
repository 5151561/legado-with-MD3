package io.legado.app.feature.bookshelf.ui

import io.legado.app.feature.bookshelf.api.BookshelfBookSummary
import io.legado.app.feature.bookshelf.api.BookshelfCommandResult
import io.legado.app.feature.bookshelf.api.BookshelfCommands
import io.legado.app.feature.bookshelf.api.BookshelfError
import io.legado.app.feature.bookshelf.api.BookshelfPreferences
import io.legado.app.feature.bookshelf.api.BookshelfPreferencesGateway
import io.legado.app.feature.bookshelf.api.BookshelfQuery
import io.legado.app.feature.bookshelf.api.BookshelfQueryRequest
import io.legado.app.feature.bookshelf.api.BookshelfQueryState
import io.legado.app.feature.bookshelf.api.BookshelfSnapshot
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookshelfViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `query maps loading empty and retryable error explicitly`() = runTest(mainDispatcherRule.dispatcher) {
        val query = FakeQuery()
        val viewModel = createViewModel(query = query)
        runCurrent()
        assertTrue(viewModel.uiState.value.contentState is BookshelfContentState.Loading)

        query.states.value = BookshelfQueryState.Data(snapshot())
        runCurrent()
        assertTrue(viewModel.uiState.value.contentState is BookshelfContentState.Empty)

        query.states.value = BookshelfQueryState.Failure(BookshelfError.Retryable("offline"))
        runCurrent()
        val error = viewModel.uiState.value.contentState as BookshelfContentState.Error
        assertTrue(error.retryable)
    }

    @Test
    fun `successful delete waits for SSOT and duplicate confirmation writes once`() = runTest(mainDispatcherRule.dispatcher) {
        val book = book(progress = 0.25f)
        val query = FakeQuery(BookshelfQueryState.Data(snapshot(book)))
        val commands = FakeCommands()
        val viewModel = createViewModel(query, commands)
        runCurrent()

        viewModel.onIntent(BookshelfIntent.ToggleSelection(book.id))
        viewModel.onIntent(BookshelfIntent.RequestDelete)
        viewModel.onIntent(BookshelfIntent.ConfirmDelete(deleteOriginal = false))
        viewModel.onIntent(BookshelfIntent.ConfirmDelete(deleteOriginal = false))
        runCurrent()

        assertEquals(1, commands.deleteCalls.get())
        commands.pendingDelete.complete(BookshelfCommandResult.Success(setOf(book.id)))
        runCurrent()

        assertEquals(listOf(book.id), viewModel.uiState.value.books.map { it.id })
        query.states.value = BookshelfQueryState.Data(snapshot())
        runCurrent()
        assertTrue(viewModel.uiState.value.books.isEmpty())
    }

    @Test
    fun `failed delete keeps selection for retry`() = runTest(mainDispatcherRule.dispatcher) {
        val book = book(progress = 0.25f)
        val query = FakeQuery(BookshelfQueryState.Data(snapshot(book)))
        val commands = FakeCommands()
        val viewModel = createViewModel(query, commands)
        runCurrent()

        viewModel.onIntent(BookshelfIntent.ToggleSelection(book.id))
        viewModel.onIntent(BookshelfIntent.RequestDelete)
        viewModel.onIntent(BookshelfIntent.ConfirmDelete(deleteOriginal = false))
        runCurrent()
        commands.pendingDelete.complete(
            BookshelfCommandResult.Failure(BookshelfError.Retryable("locked"))
        )
        runCurrent()

        assertEquals(setOf(book.id), viewModel.uiState.value.selectedBookIds)
    }

    @Test
    fun `reader progress projection refreshes from query SSOT`() = runTest(mainDispatcherRule.dispatcher) {
        val query = FakeQuery(BookshelfQueryState.Data(snapshot(book(progress = 0.1f))))
        val viewModel = createViewModel(query)
        runCurrent()
        assertEquals(0.1f, viewModel.uiState.value.books.single().readingProgress)

        query.states.value = BookshelfQueryState.Data(snapshot(book(progress = 0.8f)))
        runCurrent()
        assertEquals(0.8f, viewModel.uiState.value.books.single().readingProgress)
    }

    private fun createViewModel(
        query: FakeQuery,
        commands: FakeCommands = FakeCommands(),
    ) = BookshelfViewModel(query, commands, FakePreferences())

    private class FakeQuery(
        initial: BookshelfQueryState = BookshelfQueryState.Loading,
    ) : BookshelfQuery {
        val states = MutableStateFlow(initial)
        override fun observeBookshelf(request: BookshelfQueryRequest): Flow<BookshelfQueryState> = states
    }

    private class FakePreferences : BookshelfPreferencesGateway {
        override val current = BookshelfPreferences()
        override val preferences = MutableStateFlow(current)
        override suspend fun selectGroup(groupId: Long) = Unit
    }

    private class FakeCommands : BookshelfCommands {
        val deleteCalls = AtomicInteger()
        val pendingDelete = CompletableDeferred<BookshelfCommandResult>()

        override suspend fun moveBooks(
            bookIds: Set<String>,
            groupId: Long,
        ): BookshelfCommandResult = BookshelfCommandResult.Success(bookIds)

        override suspend fun reorderBooks(
            groupId: Long,
            orderedBookIds: List<String>,
            descending: Boolean,
        ): BookshelfCommandResult = BookshelfCommandResult.Success(orderedBookIds.toSet())

        override suspend fun deleteBooks(
            bookIds: Set<String>,
            deleteOriginal: Boolean,
        ): BookshelfCommandResult {
            deleteCalls.incrementAndGet()
            return pendingDelete.await()
        }
    }

    private fun snapshot(vararg books: BookshelfBookSummary) = BookshelfSnapshot(
        selectedGroupId = -1L,
        groups = emptyList(),
        books = books.toList(),
    )

    private fun book(progress: Float) = BookshelfBookSummary(
        id = "book-1",
        name = "Book",
        author = "Author",
        origin = "source",
        originName = "Source",
        coverUrl = null,
        currentChapterTitle = "Chapter",
        latestChapterTitle = "Latest",
        currentChapterIndex = 1,
        totalChapterCount = 10,
        unreadChapterCount = 8,
        readingProgress = progress,
        lastReadAt = 1,
        latestChapterAt = 2,
        groupMask = 0,
        order = 0,
        isLocal = false,
        isAudio = false,
        isImage = false,
    )
}
