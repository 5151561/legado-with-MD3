package io.legado.app.feature.catalog.ui

import io.legado.app.feature.catalog.api.SourceCatalogCount
import io.legado.app.feature.catalog.api.SourceCatalogKind
import io.legado.app.feature.catalog.api.SourceHubQuery
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
class SourceHubViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `book source summary carries count, enabled and health`() = runTest(dispatcher) {
        val viewModel = SourceHubViewModel(
            SourceHubQuery {
                flowOf(listOf(SourceCatalogCount(SourceCatalogKind.BookSource, 312, enabled = 208, unhealthy = 3)))
            }
        )
        testScheduler.advanceUntilIdle()

        val summary = viewModel.uiState.value.groups
            .flatMap { it.entries }
            .single { it.id == SourceHubEntryId.BookSources }
            .summary

        assertEquals("312 个 · 启用 208 · 3 个失效", summary)
    }

    @Test
    fun `no health signal means the clause is dropped, not written as zero`() = runTest(dispatcher) {
        val viewModel = SourceHubViewModel(
            SourceHubQuery {
                flowOf(listOf(SourceCatalogCount(SourceCatalogKind.BookSource, 312, enabled = 208, unhealthy = null)))
            }
        )
        testScheduler.advanceUntilIdle()

        assertEquals(
            "312 个 · 启用 208",
            viewModel.uiState.value.groups.flatMap { it.entries }
                .single { it.id == SourceHubEntryId.BookSources }.summary,
        )
    }

    @Test
    fun `zero failures is also dropped — a healthy hub says nothing about health`() = runTest(dispatcher) {
        val viewModel = SourceHubViewModel(
            SourceHubQuery {
                flowOf(listOf(SourceCatalogCount(SourceCatalogKind.BookSource, 312, enabled = 208, unhealthy = 0)))
            }
        )
        testScheduler.advanceUntilIdle()

        assertEquals(
            "312 个 · 启用 208",
            viewModel.uiState.value.groups.flatMap { it.entries }
                .single { it.id == SourceHubEntryId.BookSources }.summary,
        )
    }

    @Test
    fun `sources are counted in 个 and rules in 条`() = runTest(dispatcher) {
        val viewModel = SourceHubViewModel(
            SourceHubQuery {
                flowOf(
                    listOf(
                        SourceCatalogCount(SourceCatalogKind.RssSource, 8, enabled = 8),
                        SourceCatalogCount(SourceCatalogKind.ReplaceRule, 23, enabled = 19),
                    )
                )
            }
        )
        testScheduler.advanceUntilIdle()
        val entries = viewModel.uiState.value.groups.flatMap { it.entries }.associate { it.id to it.summary }

        assertEquals("8 个 · 启用 8", entries[SourceHubEntryId.RssSources])
        assertEquals("23 条 · 启用 19", entries[SourceHubEntryId.ReplaceRules])
    }

    @Test
    fun `the default tts engine is named only when there is one`() = runTest(dispatcher) {
        val withDefault = SourceHubViewModel(
            SourceHubQuery {
                flowOf(listOf(SourceCatalogCount(SourceCatalogKind.HttpTts, 4, defaultName = "云雀")))
            }
        )
        val without = SourceHubViewModel(
            SourceHubQuery { flowOf(listOf(SourceCatalogCount(SourceCatalogKind.HttpTts, 4))) }
        )
        testScheduler.advanceUntilIdle()

        fun summaryOf(vm: SourceHubViewModel) = vm.uiState.value.groups.flatMap { it.entries }
            .single { it.id == SourceHubEntryId.HttpTts }.summary

        assertEquals("4 个 · 默认「云雀」", summaryOf(withDefault))
        assertEquals("4 个", summaryOf(without))
    }

    @Test
    fun `the two highlight rules say what they act on`() = runTest(dispatcher) {
        val viewModel = SourceHubViewModel(
            SourceHubQuery {
                flowOf(
                    listOf(
                        SourceCatalogCount(SourceCatalogKind.ContentHighlightRule, 6, enabled = 6),
                        SourceCatalogCount(SourceCatalogKind.TagHighlightRule, 4, enabled = 4),
                    )
                )
            }
        )
        testScheduler.advanceUntilIdle()
        val entries = viewModel.uiState.value.groups.flatMap { it.entries }.associate { it.id to it.summary }

        // 名字相近、去处不明是 D-00 要解的问题，作用对象必须写进摘要。
        assertEquals("6 条 · 启用 6 · 作用于正文文本", entries[SourceHubEntryId.ContentHighlight])
        assertEquals("4 条 · 启用 4 · 作用于书架标签", entries[SourceHubEntryId.TagHighlight])
    }

    @Test
    fun `built-in txt toc rules are spelled out`() = runTest(dispatcher) {
        val viewModel = SourceHubViewModel(
            SourceHubQuery {
                flowOf(listOf(SourceCatalogCount(SourceCatalogKind.TxtTocRule, 9, builtIn = 6)))
            }
        )
        testScheduler.advanceUntilIdle()

        assertEquals(
            "9 条 · 含内置 6 条",
            viewModel.uiState.value.groups.flatMap { it.entries }
                .single { it.id == SourceHubEntryId.TxtTocRules }.summary,
        )
    }
}
