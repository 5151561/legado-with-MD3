package io.legado.app.feature.rss.ui

import io.legado.app.feature.rss.api.RssCommandResult
import io.legado.app.feature.rss.api.RssCommands
import io.legado.app.feature.rss.api.RssOpenTarget
import io.legado.app.feature.rss.api.RssQuery
import io.legado.app.feature.rss.api.RssQueryState
import io.legado.app.feature.rss.api.RssSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RssViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun `empty snapshot renders empty instead of failure`() = runTest(dispatcher) {
        val commands = object : RssCommands {
            override suspend fun resolveOpenTarget(sourceId: String) =
                Result.success<RssOpenTarget>(RssOpenTarget.Sort(sourceId))
            override suspend fun pinSource(sourceId: String) = RssCommandResult.Success
            override suspend fun disableSource(sourceId: String) = RssCommandResult.Success
            override suspend fun deleteSource(sourceId: String) = RssCommandResult.Success
        }
        val viewModel = RssViewModel(
            RssQuery { flowOf(RssQueryState.Data(RssSnapshot(emptyList(), emptyList()))) },
            commands,
        )
        testScheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.loadFailed)
    }
}
