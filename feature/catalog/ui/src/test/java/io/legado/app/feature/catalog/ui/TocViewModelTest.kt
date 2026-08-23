package io.legado.app.feature.catalog.ui

import io.legado.app.feature.catalog.api.ChapterCacheState
import io.legado.app.feature.catalog.api.ChapterFailureReason
import io.legado.app.feature.catalog.api.TocSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TocViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private val snapshot = TocSnapshot(
        bookId = "book",
        bookName = "雪落长安",
        chapters = listOf(
            tocChapter("0", state = ChapterCacheState.Cached, cachedBytes = 12_288),
            tocChapter("1", state = ChapterCacheState.NotCached),
            tocChapter("2", state = ChapterCacheState.Failed),
            tocChapter("3", state = ChapterCacheState.Downloading),
        ),
        totalChapterCount = 386,
        cachedChapterCount = 214,
        notCachedChapterCount = 172,
        failedChapterCount = 2,
    )

    private val commands = RecordingTocCommands()

    // ---- 目录管理页（S-06b） ----

    @Test
    fun `filters carry whole-book counts`() = runTest(dispatcher) {
        val vm = TocManageViewModel("book", FakeTocQuery(snapshot), commands)
        testScheduler.advanceUntilIdle()
        val state = vm.uiState.value

        assertEquals("雪落长安 · 386 章", state.subtitle)
        assertEquals(listOf(386, 172, 2), state.filters.map { it.count })
    }

    @Test
    fun `chapter notes explain the state instead of just flagging it`() = runTest(dispatcher) {
        val vm = TocManageViewModel("book", FakeTocQuery(snapshot), commands)
        testScheduler.advanceUntilIdle()
        val notes = vm.uiState.value.chapters.associate { it.id to it.note }

        assertEquals("已缓存 · 12 KB", notes["0"])
        assertEquals("未缓存", notes["1"])
        assertEquals("上次失败", notes["2"])
        assertEquals("下载中", notes["3"])
    }

    @Test
    fun `a known failure reason is spelled out`() = runTest(dispatcher) {
        val failed = snapshot.copy(
            chapters = listOf(
                tocChapter("2", state = ChapterCacheState.Failed)
                    .copy(failureReason = ChapterFailureReason.EmptyContent),
            )
        )
        val vm = TocManageViewModel("book", FakeTocQuery(failed), commands)
        testScheduler.advanceUntilIdle()

        assertEquals("上次失败 · 正文为空", vm.uiState.value.chapters.single().note)
    }

    @Test
    fun `only failed chapters are retryable`() = runTest(dispatcher) {
        val vm = TocManageViewModel("book", FakeTocQuery(snapshot), commands)
        testScheduler.advanceUntilIdle()

        assertEquals(setOf("2"), vm.uiState.value.chapters.filter { it.retryable }.map { it.id }.toSet())
    }

    @Test
    fun `range selection covers both endpoints regardless of order`() = runTest(dispatcher) {
        val vm = TocManageViewModel("book", FakeTocQuery(snapshot), commands)
        testScheduler.advanceUntilIdle()

        vm.onIntent(TocManageIntent.SelectRange("3", "1"))

        assertEquals(setOf("1", "2", "3"), vm.uiState.value.selected)
    }

    @Test
    fun `selection drops chapters the filter hid`() = runTest(dispatcher) {
        val vm = TocManageViewModel("book", FakeTocQuery(snapshot), commands)
        testScheduler.advanceUntilIdle()
        vm.onIntent(TocManageIntent.SelectAll)
        assertEquals(4, vm.uiState.value.selected.size)

        vm.onIntent(TocManageIntent.SelectFilter(TocFilter.Failed))
        testScheduler.advanceUntilIdle()

        // 底部条不能数出用户已经看不见的章节。
        assertEquals(setOf("2"), vm.uiState.value.selected)
    }

    @Test
    fun `downloading nothing is refused instead of sending an empty command`() = runTest(dispatcher) {
        val vm = TocManageViewModel("book", FakeTocQuery(snapshot), commands)
        testScheduler.advanceUntilIdle()

        vm.onIntent(TocManageIntent.DownloadSelected)
        testScheduler.advanceUntilIdle()

        assertEquals(emptyList<String>(), commands.calls)
    }

    @Test
    fun `deleting cache asks before it deletes`() = runTest(dispatcher) {
        val vm = TocManageViewModel("book", FakeTocQuery(snapshot), commands)
        testScheduler.advanceUntilIdle()
        vm.onIntent(TocManageIntent.ToggleChapter("0"))

        vm.onIntent(TocManageIntent.DeleteSelectedCache)
        testScheduler.advanceUntilIdle()
        // 危险动作先出确认,不直接执行。
        assertEquals(emptyList<String>(), commands.calls)

        vm.confirmDeleteSelectedCache()
        testScheduler.advanceUntilIdle()
        assertEquals(listOf("delete:[0]"), commands.calls)
    }

    // ---- 阅读器内目录（S-06a） ----

    @Test
    fun `the reader panel summarises the book, not the filtered page`() = runTest(dispatcher) {
        val vm = ReaderTocViewModel("book", FakeTocQuery(snapshot), commands)
        testScheduler.advanceUntilIdle()

        assertEquals("386 章 · 已缓存 214", vm.uiState.value.summary)
    }

    @Test
    fun `a cached chapter needs no explanation, an uncached one does`() = runTest(dispatcher) {
        val vm = ReaderTocViewModel("book", FakeTocQuery(snapshot), commands)
        testScheduler.advanceUntilIdle()
        val notes = vm.uiState.value.chapters.associate { it.id to it.note }

        assertNull(notes["0"])
        assertEquals("未缓存 · 点开即联网加载", notes["1"])
        assertEquals("上次加载失败 · 可换源", notes["2"])
    }

    @Test
    fun `in-chapter progress is attached to the current chapter only`() = runTest(dispatcher) {
        val withCurrent = snapshot.copy(
            chapters = listOf(
                tocChapter("0", state = ChapterCacheState.Cached),
                tocChapter("1", state = ChapterCacheState.Cached, isCurrent = true),
            ),
            currentChapterProgress = 0.62f,
        )
        val vm = ReaderTocViewModel("book", FakeTocQuery(withCurrent), commands)
        testScheduler.advanceUntilIdle()
        val chapters = vm.uiState.value.chapters.associateBy { it.id }

        assertNull(chapters.getValue("0").progress)
        assertEquals("62%", chapters.getValue("1").progressLabel)
    }

    @Test
    fun `toggling order flips the persisted flag`() = runTest(dispatcher) {
        val vm = ReaderTocViewModel("book", FakeTocQuery(snapshot.copy(reversed = true)), commands)
        testScheduler.advanceUntilIdle()

        vm.onIntent(ReaderTocIntent.ToggleOrder)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("reversed:false"), commands.calls)
    }

    @Test
    fun `a single chapter refresh redownloads that chapter`() = runTest(dispatcher) {
        val vm = ReaderTocViewModel("book", FakeTocQuery(snapshot), commands)
        testScheduler.advanceUntilIdle()

        vm.onIntent(ReaderTocIntent.SelectChapterAction("2", TocChapterAction.RefreshChapter))
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("retry:2"), commands.calls)
        assertNull(vm.uiState.value.chapterMenuFor)
    }

    @Test
    fun `back to current does nothing when the book was never opened`() = runTest(dispatcher) {
        val vm = ReaderTocViewModel("book", FakeTocQuery(snapshot), commands)
        testScheduler.advanceUntilIdle()

        vm.onIntent(ReaderTocIntent.BackToCurrent)
        testScheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.chapters.none { it.isCurrent })
    }
}
