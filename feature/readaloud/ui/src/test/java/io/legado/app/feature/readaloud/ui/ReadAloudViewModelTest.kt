package io.legado.app.feature.readaloud.ui

import io.legado.app.feature.readaloud.api.ReadAloudCommandResult
import io.legado.app.feature.readaloud.api.ReadAloudSessionGateway
import io.legado.app.feature.readaloud.api.ReadAloudSnapshot
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
class ReadAloudViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun `session progress is mapped from ssot`() = runTest(dispatcher) {
        val flow = MutableStateFlow(ReadAloudSnapshot(chapterPosition = 20, chapterLength = 100))
        val gateway = object : ReadAloudSessionGateway {
            override val session = flow
            override val current get() = flow.value
            override suspend fun togglePause() = ReadAloudCommandResult.Success
            override suspend fun previousParagraph() = ReadAloudCommandResult.Success
            override suspend fun nextParagraph() = ReadAloudCommandResult.Success
            override suspend fun previousChapter() = ReadAloudCommandResult.Success
            override suspend fun nextChapter() = ReadAloudCommandResult.Success
            override suspend fun seekTo(chapterPosition: Int) = ReadAloudCommandResult.Success
            override suspend fun setSpeed(value: Int) = ReadAloudCommandResult.Success
            override suspend fun setTimer(minutes: Int) = ReadAloudCommandResult.Success
        }
        val viewModel = ReadAloudViewModel(gateway)
        testScheduler.advanceUntilIdle()
        assertEquals(20, viewModel.uiState.value.chapterPosition)
    }
}
