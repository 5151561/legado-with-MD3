package io.legado.app.feature.catalog.impl

import io.legado.app.constant.BookType
import io.legado.app.feature.catalog.api.BookDetailQueryState
import io.legado.app.feature.catalog.api.BookDetailRequest
import io.legado.app.feature.catalog.api.BookDetailSnapshot
import io.legado.app.feature.catalog.api.ChapterCacheState
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 书籍详情契约（画板 S-04 / S-04a），跑在正式实现上。 */
class BookDetailImplContractTest {

    private val store = FakeBookDetailStore()
    private val cacheHost = RecordingChapterCacheHost()
    private val bookshelfHost = RecordingBookshelfHost()
    private val detail = DefaultBookDetailRepository(
        store,
        cacheHost,
        bookshelfHost,
        FakeRelatedBooksHost(),
    )

    private suspend fun snapshot(): BookDetailSnapshot {
        val states = detail.observeBookDetail(BookDetailRequest("book")).take(2).toList()
        assertEquals(BookDetailQueryState.Loading, states.first())
        return (states[1] as BookDetailQueryState.Data).snapshot
    }

    @Test
    fun `a book that is only a search result still opens`() = runTest {
        // 五个入口共用一个路由，从搜索进来时书还没入库。
        store.bookFlow.value = null
        store.searchBook = book(name = "雪落长安")

        val snapshot = snapshot()

        assertEquals("雪落长安", snapshot.name)
        assertFalse(snapshot.inBookshelf)
        assertEquals(emptyList<String>(), snapshot.groupNames)
    }

    @Test
    fun `a book that is nowhere is not retryable`() = runTest {
        store.bookFlow.value = null
        store.searchBook = null

        val states = detail.observeBookDetail(BookDetailRequest("book")).take(2).toList()

        assertEquals(BookDetailQueryState.Failed(retryable = false), states[1])
    }

    @Test
    fun `being in the books table is what in-bookshelf means`() = runTest {
        store.groups = listOf("历史")

        val snapshot = snapshot()

        assertTrue(snapshot.inBookshelf)
        assertEquals(listOf("历史"), snapshot.groupNames)
    }

    @Test
    fun `an unread book has no progress`() = runTest {
        store.chaptersFlow.value = (0..9).map(::chapter)

        assertNull(snapshot().progress)
    }

    @Test
    fun `progress is chapters read over chapters total`() = runTest {
        store.chaptersFlow.value = (0..9).map(::chapter)
        store.bookFlow.value = book(durChapterIndex = 5)

        assertEquals(0.5f, snapshot().progress!!, 0.0001f)
    }

    @Test
    fun `cached chapter count comes from the cache host, not the chapter table`() = runTest {
        store.chaptersFlow.value = (0..3).map(::chapter)
        cacheHost.setStates(
            0 to ChapterCacheState.Cached,
            1 to ChapterCacheState.Cached,
            2 to ChapterCacheState.NotCached,
            3 to ChapterCacheState.Downloading,
        )

        assertEquals(2, snapshot().cachedChapterCount)
    }

    @Test
    fun `a local book has no source name`() = runTest {
        store.bookFlow.value = localBook("book")

        val snapshot = snapshot()

        assertTrue(snapshot.isLocal)
        assertNull(snapshot.sourceName)
    }

    @Test
    fun `removal impact offers the local file only for local books`() = runTest {
        store.chaptersFlow.value = (0..3).map(::chapter)
        store.bookmarks = 14
        store.notes = 3
        cacheHost.totalCachedBytes = 18_200_000

        val webImpact = detail.removalImpact("book")!!
        assertNull(webImpact.localFilePath)
        assertEquals(14, webImpact.bookmarkCount)
        assertEquals(3, webImpact.noteCount)
        assertEquals(18_200_000L, webImpact.cachedBytes)

        store.bookFlow.value = localBook("/Books/雪落长安.epub")
        assertEquals("/Books/雪落长安.epub", detail.removalImpact("book")!!.localFilePath)
    }

    @Test
    fun `unmeasurable cache size stays null`() = runTest {
        cacheHost.totalCachedBytes = null

        assertNull(detail.removalImpact("book")!!.cachedBytes)
    }

    @Test
    fun `removal impact of a missing book is null`() = runTest {
        store.bookFlow.value = null

        assertNull(detail.removalImpact("book"))
    }

    @Test
    fun `deleting the local file is a separate intent from leaving the shelf`() = runTest {
        detail.removeFromBookshelf("book")
        detail.removeFromBookshelf("book", deleteLocalFile = true)

        assertEquals(listOf("book" to false, "book" to true), bookshelfHost.removed)
    }

    @Test
    fun `blank remark clears the remark instead of storing whitespace`() = runTest {
        detail.updateRemark("book", "   ")

        assertNull(store.bookFlow.value!!.remark)
    }

    @Test
    fun `moving to a group writes the group on the book`() = runTest {
        detail.moveToGroup("book", 42L)

        assertEquals(42L, store.bookFlow.value!!.group)
    }
}
