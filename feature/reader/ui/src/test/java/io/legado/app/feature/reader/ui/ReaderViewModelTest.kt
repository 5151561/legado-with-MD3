package io.legado.app.feature.reader.ui

import io.legado.app.feature.reader.api.ReaderCommandResult
import io.legado.app.feature.reader.api.ReaderLoadState
import io.legado.app.feature.reader.api.ReaderProgress
import io.legado.app.feature.reader.api.ReaderSessionGateway
import io.legado.app.feature.reader.api.ReaderSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `session page is mapped from ssot`() = runTest(dispatcher) {
        val source = MutableStateFlow(ReaderSnapshot())
        val gateway = FakeReaderGateway(source)
        val viewModel = ReaderViewModel(gateway)

        source.value = ReaderSnapshot(
            bookName = "Book",
            pageText = "Page two",
            pageIndex = 1,
            pageCount = 3,
            loadState = ReaderLoadState.Content,
            contentRevision = 2,
        )
        testScheduler.advanceUntilIdle()

        assertEquals("Page two", viewModel.uiState.value.pageText)
        assertEquals(1, viewModel.uiState.value.pageIndex)
    }

    @Test
    fun `duplicate page command is ignored while one is running`() = runTest(dispatcher) {
        val source = MutableStateFlow(ReaderSnapshot(loadState = ReaderLoadState.Content))
        val gateway = FakeReaderGateway(source)
        val viewModel = ReaderViewModel(gateway)

        viewModel.onIntent(ReaderIntent.NextPage)
        viewModel.onIntent(ReaderIntent.NextPage)
        testScheduler.advanceUntilIdle()

        assertEquals(1, gateway.nextPageCalls)
    }
}

private class FakeReaderGateway(
    private val source: MutableStateFlow<ReaderSnapshot>,
) : ReaderSessionGateway {
    var nextPageCalls = 0
    override val session = source
    override val current get() = source.value
    override suspend fun nextPage(): ReaderCommandResult {
        nextPageCalls++
        return ReaderCommandResult.Success
    }
    override suspend fun previousPage() = ReaderCommandResult.Success
    override suspend fun nextChapter() = ReaderCommandResult.Success
    override suspend fun previousChapter() = ReaderCommandResult.Success
    override suspend fun moveToChapter(index: Int, position: Int) = ReaderCommandResult.Success
    override suspend fun saveProgress() = ReaderCommandResult.Success
    override suspend fun restoreProgress(progress: ReaderProgress) = ReaderCommandResult.Success
    override suspend fun syncProgress() = ReaderCommandResult.Success
    override suspend fun retry() = ReaderCommandResult.Success
}
