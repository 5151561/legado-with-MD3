package io.legado.app.feature.settings.ui

import io.legado.app.feature.settings.api.SettingsOverview
import io.legado.app.feature.settings.api.SettingsOverviewQuery
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
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `overview maps to content state`() = runTest(dispatcher) {
        val query = SettingsOverviewQuery {
            flowOf(SettingsOverview("dark", 12, 64, 8, true, true))
        }
        val viewModel = SettingsViewModel(query)
        testScheduler.advanceUntilIdle()

        assertEquals("dark", viewModel.uiState.value.themeMode)
        assertEquals(64, viewModel.uiState.value.bitmapCacheSizeMb)
    }
}
