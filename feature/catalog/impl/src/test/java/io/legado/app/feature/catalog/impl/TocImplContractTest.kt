package io.legado.app.feature.catalog.impl

import io.legado.app.feature.catalog.api.ChapterCacheState
import io.legado.app.feature.catalog.api.ChapterSelection
import io.legado.app.feature.catalog.api.TocCacheFilter
import io.legado.app.feature.catalog.api.TocQueryState
import io.legado.app.feature.catalog.api.TocRequest
import io.legado.app.feature.catalog.api.TocSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 目录契约（画板 S-06a / S-06b），跑在正式实现上。 */
class TocImplContractTest {

    private val store = FakeTocStore()
    private val cacheHost = RecordingChapterCacheHost()
    private val toc = DefaultTocRepository(store, cacheHost)

    private suspend fun snapshot(request: TocRequest = TocRequest("book")): TocSnapshot {
        val states = toc.observeToc(request).take(2).toList()
        assertEquals(TocQueryState.Loading, states.first())
        return (states[1] as TocQueryState.Data).snapshot
    }

    @Test
    fun `query emits loading before the first snapshot`() = runTest {
        store.chaptersFlow.value = listOf(chapter(0))

        assertEquals(1, snapshot().chapters.size)
    }

    @Test
    fun `a missing book is not retryable`() = runTest {
        store.bookFlow.value = null

        val states = toc.observeToc(TocRequest("book")).take(2).toList()

        assertEquals(TocQueryState.Failed(retryable = false), states[1])
    }

    @Test
    fun `counts describe the whole book while the list is filtered`() = runTest {
        store.chaptersFlow.value = (0..3).map(::chapter)
        cacheHost.setStates(
            0 to ChapterCacheState.Cached,
            1 to ChapterCacheState.NotCached,
            2 to ChapterCacheState.Failed,
            3 to ChapterCacheState.Cached,
        )

        val filtered = snapshot(TocRequest("book", filter = TocCacheFilter.Failed))

        assertEquals(1, filtered.chapters.size)
        assertEquals(4, filtered.totalChapterCount)
        assertEquals(2, filtered.cachedChapterCount)
        assertEquals(1, filtered.failedChapterCount)
    }

    @Test
    fun `not cached means not-yet-cached, failures included`() = runTest {
        store.chaptersFlow.value = (0..2).map(::chapter)
        cacheHost.setStates(
            0 to ChapterCacheState.Cached,
            1 to ChapterCacheState.NotCached,
            2 to ChapterCacheState.Failed,
        )

        val filtered = snapshot(TocRequest("book", filter = TocCacheFilter.NotCached))

        // 筹码上的数与筛出来的条数必须对得上，否则「未缓存 2」点进去只有 1 条。
        assertEquals(2, filtered.notCachedChapterCount)
        assertEquals(2, filtered.chapters.size)
    }

    @Test
    fun `reversing changes the order but not the chapter numbers`() = runTest {
        store.chaptersFlow.value = (0..2).map(::chapter)
        store.bookFlow.value = book().apply { setReverseToc(true) }

        val reversed = snapshot()

        assertEquals(listOf(2, 1, 0), reversed.chapters.map { it.index })
        assertTrue(reversed.reversed)
    }

    @Test
    fun `an unread book has no current chapter and no in-chapter progress`() = runTest {
        store.chaptersFlow.value = (0..2).map(::chapter)
        cacheHost.contentLength = 1000

        val fresh = snapshot()

        assertEquals(-1, fresh.currentChapterIndex)
        assertNull(fresh.currentChapterProgress)
        assertTrue(fresh.chapters.none { it.isCurrent })
    }

    @Test
    fun `in-chapter progress is null when the chapter is not cached`() = runTest {
        store.chaptersFlow.value = (0..2).map(::chapter)
        store.bookFlow.value = book(durChapterIndex = 1, durChapterPos = 500)
        cacheHost.contentLength = null

        assertNull(snapshot().currentChapterProgress)
    }

    @Test
    fun `in-chapter progress is the read position over the cached length`() = runTest {
        store.chaptersFlow.value = (0..2).map(::chapter)
        store.bookFlow.value = book(durChapterIndex = 1, durChapterPos = 500)
        cacheHost.contentLength = 1000

        assertEquals(0.5f, snapshot().currentChapterProgress!!, 0.0001f)
    }

    @Test
    fun `all-missing downloads everything that is not already cached`() = runTest {
        store.chaptersFlow.value = (0..3).map(::chapter)
        cacheHost.setStates(
            0 to ChapterCacheState.Cached,
            1 to ChapterCacheState.NotCached,
            2 to ChapterCacheState.Failed,
            3 to ChapterCacheState.Cached,
        )

        toc.enqueueDownload("book", ChapterSelection.AllMissing)

        assertEquals(listOf("enqueue[1, 2]"), cacheHost.calls)
    }

    @Test
    fun `an empty selection downloads nothing`() = runTest {
        store.chaptersFlow.value = (0..3).map(::chapter)

        toc.enqueueDownload("book", ChapterSelection.Ids(emptySet()))

        assertEquals(emptyList<String>(), cacheHost.calls)
    }

    @Test
    fun `retry clears the old result before queueing`() = runTest {
        store.chaptersFlow.value = (0..2).map(::chapter)

        toc.retryChapter("book", "chapter/1")

        // 顺序反了就会立刻命中上一次的缓存，重试等于没试。
        assertEquals(listOf("delete[1]", "enqueue[1]"), cacheHost.calls)
    }

    @Test
    fun `deleting cache maps chapter ids to indices`() = runTest {
        store.chaptersFlow.value = (0..3).map(::chapter)

        toc.deleteCache("book", setOf("chapter/0", "chapter/2"))

        assertEquals(listOf("delete[0, 2]"), cacheHost.calls)
    }

    @Test
    fun `reversing the toc is persisted on the book`() = runTest {
        toc.setReversed("book", true)

        assertTrue(store.bookFlow.value!!.getReverseToc())
    }
}
