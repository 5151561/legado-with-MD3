package io.legado.app.feature.catalog.ui

import io.legado.app.feature.catalog.api.BookDetailSnapshot
import io.legado.app.feature.catalog.api.BookInsightCounts
import io.legado.app.feature.catalog.api.BookRemovalImpact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private val snapshot = BookDetailSnapshot(
        bookId = "book",
        name = "雪落长安",
        author = "柳仲卿",
        kinds = listOf("历史", "连载中"),
        inBookshelf = true,
        groupNames = listOf("历史"),
        totalChapterCount = 386,
        cachedChapterCount = 214,
        latestChapterTitle = "第 386 章 归途",
        progress = 0.62f,
        sourceName = "墨韵书屋",
        alternativeSourceCount = 12,
        insights = BookInsightCounts(28, 14, 9),
    )

    private val commands = RecordingBookDetailCommands()
    private val tocCommands = RecordingTocCommands()

    private fun viewModel(
        data: BookDetailSnapshot = snapshot,
        impact: BookRemovalImpact? = null,
    ) = BookDetailViewModel("book", FakeBookDetailQuery(data, impact), commands, tocCommands)

    @Test
    fun `header reads like the board`() = runTest(dispatcher) {
        val vm = viewModel()
        testScheduler.advanceUntilIdle()
        val header = vm.uiState.value.header!!

        assertEquals("柳仲卿 · 历史 · 连载中", header.byline)
        assertEquals("386 章 · 最新 第 386 章 归途", header.chapterSummary)
        assertEquals("在书架 · 历史 组", header.shelfLabel)
        assertEquals("62%", header.progressLabel)
    }

    @Test
    fun `a book outside the shelf has no shelf label`() = runTest(dispatcher) {
        val vm = viewModel(snapshot.copy(inBookshelf = false, groupNames = emptyList()))
        testScheduler.advanceUntilIdle()

        assertNull(vm.uiState.value.header!!.shelfLabel)
    }

    @Test
    fun `the catalog entry points at where batch download lives`() = runTest(dispatcher) {
        val vm = viewModel()
        testScheduler.advanceUntilIdle()
        val entries = vm.uiState.value.entries.associateBy { it.id }

        assertEquals("386 章 · 已缓存 214 · 批量下载在此", entries.getValue(BookDetailEntryId.Catalog).summary)
        assertEquals("28 / 14 / 9", entries.getValue(BookDetailEntryId.Insights).valueLabel)
    }

    @Test
    fun `delete local file is offered only for local books`() = runTest(dispatcher) {
        val web = viewModel()
        testScheduler.advanceUntilIdle()
        web.onIntent(BookDetailIntent.OpenMenu)
        assertFalse(web.uiState.value.menu!!.any { it.action == BookDetailMenuAction.DeleteLocalFile })

        val local = viewModel(snapshot.copy(isLocal = true))
        testScheduler.advanceUntilIdle()
        local.onIntent(BookDetailIntent.OpenMenu)
        val actions = local.uiState.value.menu!!.map { it.action }
        assertTrue(BookDetailMenuAction.DeleteLocalFile in actions)
        // 本地书没有书源变量可编辑。
        assertFalse(BookDetailMenuAction.EditVariables in actions)
    }

    @Test
    fun `the removal dialog spells out what is lost`() = runTest(dispatcher) {
        val vm = viewModel(
            impact = BookRemovalImpact(
                bookName = "雪落长安",
                progress = 0.62f,
                bookmarkCount = 14,
                noteCount = 2,
                cachedChapterCount = 214,
                cachedBytes = 19_084_083,
            ),
        )
        testScheduler.advanceUntilIdle()
        vm.onIntent(BookDetailIntent.OpenMenu)
        vm.onIntent(BookDetailIntent.SelectMenuAction(BookDetailMenuAction.RemoveFromShelf))
        testScheduler.advanceUntilIdle()

        val dialog = vm.uiState.value.activeDialog as BookDetailDialog.RemoveFromShelf
        assertEquals(
            "阅读进度（62%）、书签 14 条与笔记 2 条会一并删除。已缓存的 214 章正文（18.2 MB）也会清除。",
            dialog.impact,
        )
    }

    @Test
    fun `an unmeasurable cache size drops the size, not the sentence`() = runTest(dispatcher) {
        val vm = viewModel(
            impact = BookRemovalImpact(bookName = "n", cachedChapterCount = 214, cachedBytes = null),
        )
        testScheduler.advanceUntilIdle()
        vm.onIntent(BookDetailIntent.SelectMenuAction(BookDetailMenuAction.RemoveFromShelf))
        testScheduler.advanceUntilIdle()

        val dialog = vm.uiState.value.activeDialog as BookDetailDialog.RemoveFromShelf
        assertEquals("已缓存的 214 章正文也会清除。", dialog.impact)
    }

    @Test
    fun `deleting the local file stays unchecked until the user checks it`() = runTest(dispatcher) {
        val vm = viewModel(
            impact = BookRemovalImpact(bookName = "n", localFilePath = "/Books/a.epub"),
        )
        testScheduler.advanceUntilIdle()
        vm.onIntent(BookDetailIntent.SelectMenuAction(BookDetailMenuAction.RemoveFromShelf))
        testScheduler.advanceUntilIdle()

        assertFalse((vm.uiState.value.activeDialog as BookDetailDialog.RemoveFromShelf).deleteLocalFile)

        vm.onIntent(BookDetailIntent.SetDeleteLocalFile(true))
        vm.onIntent(BookDetailIntent.ConfirmDialog)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("book" to true), commands.removed)
    }

    @Test
    fun `download sends all-missing because the detail page has no toc`() = runTest(dispatcher) {
        val vm = viewModel()
        testScheduler.advanceUntilIdle()
        vm.onIntent(BookDetailIntent.Download)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("enqueue:allMissing"), tocCommands.calls)
    }

    @Test
    fun `a fully cached book is not queued again`() = runTest(dispatcher) {
        val vm = viewModel(snapshot.copy(cachedChapterCount = 386))
        testScheduler.advanceUntilIdle()
        vm.onIntent(BookDetailIntent.Download)
        testScheduler.advanceUntilIdle()

        assertEquals(emptyList<String>(), tocCommands.calls)
    }

    @Test
    fun `alternatives label degrades when there is nothing to switch to`() = runTest(dispatcher) {
        val vm = viewModel(snapshot.copy(alternativeSourceCount = 0))
        testScheduler.advanceUntilIdle()

        assertEquals("暂无候选源", vm.uiState.value.source!!.alternativesLabel)
    }

    @Test
    fun `a local book shows no source card at all`() = runTest(dispatcher) {
        val vm = viewModel(snapshot.copy(sourceName = null))
        testScheduler.advanceUntilIdle()

        assertNull(vm.uiState.value.source)
    }

}
