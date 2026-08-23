package io.legado.app.feature.catalog.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TocApiContractTest {

    @Test
    fun `chapter identity is the chapter url`() {
        val chapter = TocChapterSnapshot(chapterId = "https://example.com/c/51", index = 50, title = "第五十一章 雪停", cacheState = ChapterCacheState.NotCached)
        assertEquals("https://example.com/c/51", chapter.chapterId)
    }

    @Test
    fun `cache state keeps the six states the download queue actually has`() {
        // 画板只画三档，但停止 / 暂停将来补回时 api 不该跟着改。
        assertEquals(6, ChapterCacheState.entries.size)
        assertTrue(ChapterCacheState.Paused in ChapterCacheState.entries)
        assertTrue(ChapterCacheState.Waiting in ChapterCacheState.entries)
    }

    @Test
    fun `failure reason is an enum so the copy stays in the ui`() {
        val failed = TocChapterSnapshot(
            chapterId = "c",
            index = 52,
            title = "第五十三章 旧友",
            cacheState = ChapterCacheState.Failed,
            failureReason = ChapterFailureReason.EmptyContent,
        )
        assertEquals(ChapterFailureReason.EmptyContent, failed.failureReason)
    }

    @Test
    fun `chapter index is independent from list order`() {
        val snapshot = TocSnapshot(
            bookId = "b",
            bookName = "雪落长安",
            chapters = listOf(
                TocChapterSnapshot("c2", index = 1, title = "第二章", cacheState = ChapterCacheState.Cached),
                TocChapterSnapshot("c1", index = 0, title = "第一章", cacheState = ChapterCacheState.Cached),
            ),
            reversed = true,
        )
        // 倒序只改列表顺序，不改章节序号——否则跳转与进度会算错。
        assertEquals(1, snapshot.chapters.first().index)
    }

    @Test
    fun `counts describe the whole book not the filtered page`() {
        val snapshot = TocSnapshot(
            bookId = "b",
            bookName = "雪落长安",
            chapters = listOf(TocChapterSnapshot("c", 52, "第五十三章", ChapterCacheState.Failed)),
            totalChapterCount = 386,
            cachedChapterCount = 214,
            notCachedChapterCount = 172,
            failedChapterCount = 2,
        )
        // 过滤到「失败」只剩一条，但筹码上的数字必须还是全书口径。
        assertEquals(1, snapshot.chapters.size)
        assertEquals(386, snapshot.totalChapterCount)
        assertEquals(2, snapshot.failedChapterCount)
    }

    @Test
    fun `unread book has no current chapter`() {
        val snapshot = TocSnapshot(bookId = "b", bookName = "n")
        assertEquals(-1, snapshot.currentChapterIndex)
        assertNull(snapshot.currentChapterProgress)
    }

    @Test
    fun `empty selection is not the same as all missing`() {
        // 详情页一键下载没有目录在手，只能表达「全部未缓存」；
        // 若两者共用一个可空集合，「一本都没选」会被当成「全都要」。
        val nothing: ChapterSelection = ChapterSelection.Ids(emptySet())
        val everythingMissing: ChapterSelection = ChapterSelection.AllMissing
        assertNotEquals(nothing, everythingMissing)
    }
}
