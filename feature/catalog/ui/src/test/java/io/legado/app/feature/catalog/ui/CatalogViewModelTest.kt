package io.legado.app.feature.catalog.ui

import io.legado.app.feature.catalog.api.CatalogCommandResult
import io.legado.app.feature.catalog.api.CatalogCommands
import io.legado.app.feature.catalog.api.CatalogQuery
import io.legado.app.feature.catalog.api.CatalogQueryState
import io.legado.app.feature.catalog.api.CatalogSnapshot
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
class CatalogViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun `empty data is not an error`() = runTest(dispatcher) {
        val query = CatalogQuery { flowOf(CatalogQueryState.Data(CatalogSnapshot(emptyList(), emptyList()))) }
        val commands = object : CatalogCommands {
            override suspend fun pinSource(sourceId: String) = CatalogCommandResult.Success
            override suspend fun deleteSource(sourceId: String) = CatalogCommandResult.Success
        }
        val viewModel = CatalogViewModel(query, commands)
        testScheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.loading)
        assertFalse(viewModel.uiState.value.loadFailed)
    }
}
