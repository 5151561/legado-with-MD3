package io.legado.app.feature.ai.ui

import io.legado.app.feature.ai.api.AiCommandResult
import io.legado.app.feature.ai.api.AiCommands
import io.legado.app.feature.ai.api.AiOverview
import io.legado.app.feature.ai.api.AiOverviewQuery
import io.legado.app.feature.ai.api.AiQueryState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()
    @Test fun `empty overview stays content`() = runTest(dispatcher) {
        val vm = AiViewModel(
            AiOverviewQuery { flowOf(AiQueryState.Data(AiOverview(emptyList(), emptyList(), 0))) },
            AiCommands { AiCommandResult.Success },
        )
        testScheduler.advanceUntilIdle()
        assertEquals(0, vm.uiState.value.providerCount)
    }
}
