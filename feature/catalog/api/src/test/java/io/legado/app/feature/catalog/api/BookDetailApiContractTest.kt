package io.legado.app.feature.catalog.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class BookDetailApiContractTest {

    @Test
    fun `book identity is the book url`() {
        val snapshot = BookDetailSnapshot(bookId = "https://example.com/book/1", name = "雪落长安", author = "柳仲卿")
        assertEquals("https://example.com/book/1", snapshot.bookId)
    }

    @Test
    fun `unread book has no progress`() {
        val snapshot = BookDetailSnapshot(bookId = "b", name = "n", author = "a")
        // null 与 0f 不同义：0f 是「读了但还在第一页」，null 是「还没开始」。
        assertNull(snapshot.progress)
    }

    @Test
    fun `removal impact separates local file from shelf removal`() {
        val webBook = BookRemovalImpact(bookName = "雪落长安", cachedChapterCount = 214)
        // 非本地书没有可删的文件，确认框就不该出现那个勾选。
        assertNull(webBook.localFilePath)

        val localBook = webBook.copy(localFilePath = "/Books/雪落长安.epub")
        assertEquals("/Books/雪落长安.epub", localBook.localFilePath)
    }

    @Test
    fun `unmeasurable cache size is null so the sentence can be dropped`() {
        val impact = BookRemovalImpact(bookName = "n", cachedChapterCount = 214)
        // 统计不到体积时显示「0 MB」是错的——UI 应当省略这一分句。
        assertNull(impact.cachedBytes)
    }

    @Test
    fun `remove from bookshelf does not delete local file by default`() {
        val recorded = mutableListOf<Pair<String, Boolean>>()
        val commands = object : BookDetailCommands {
            override suspend fun addToBookshelf(bookId: String) = CatalogCommandResult.Success
            override suspend fun removeFromBookshelf(bookId: String, deleteLocalFile: Boolean): CatalogCommandResult {
                recorded += bookId to deleteLocalFile
                return CatalogCommandResult.Success
            }

            override suspend fun moveToGroup(bookId: String, groupId: Long) = CatalogCommandResult.Success
            override suspend fun updateCover(bookId: String, coverUrl: String?) = CatalogCommandResult.Success
            override suspend fun updateRemark(bookId: String, remark: String?) = CatalogCommandResult.Success
        }

        kotlinx.coroutines.runBlocking { commands.removeFromBookshelf("b") }

        assertFalse(recorded.single().second)
    }
}
