package io.legado.app.feature.catalog.impl

import io.legado.app.constant.BookType
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.SourceCatalogCounts
import io.legado.app.feature.catalog.api.BookInsightCounts
import io.legado.app.feature.catalog.api.ChapterCacheState
import io.legado.app.feature.catalog.api.RelatedBookSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

internal fun chapter(index: Int, title: String = "第 ${index + 1} 章") = BookChapter(
    url = "chapter/$index",
    title = title,
    bookUrl = "book",
    index = index,
)

internal fun book(
    bookUrl: String = "book",
    name: String = "雪落长安",
    author: String = "柳仲卿",
    durChapterIndex: Int = 0,
    durChapterPos: Int = 0,
    origin: String = "https://example.com",
    type: Int = BookType.text,
) = Book(
    bookUrl = bookUrl,
    name = name,
    author = author,
    origin = origin,
    type = type,
    durChapterIndex = durChapterIndex,
    durChapterPos = durChapterPos,
)

/** 本地书由 `type` 位判定，`origin` 只是旧数据的兜底——两处都要设，否则不是本地书。 */
internal fun localBook(bookUrl: String) = book(
    bookUrl = bookUrl,
    origin = BookType.localTag,
    type = BookType.text or BookType.local,
)

internal class FakeTocStore : TocStore {
    val bookFlow = MutableStateFlow<Book?>(book())
    val chaptersFlow = MutableStateFlow(emptyList<BookChapter>())

    override fun observeBook(bookId: String): Flow<Book?> = bookFlow
    override fun observeChapters(bookId: String): Flow<List<BookChapter>> = chaptersFlow
    override suspend fun getBook(bookId: String): Book? = bookFlow.value
    override suspend fun getChapters(bookId: String): List<BookChapter> = chaptersFlow.value
    override suspend fun updateBook(book: Book) {
        bookFlow.value = book
    }
}

internal class FakeBookDetailStore : BookDetailStore {
    val bookFlow = MutableStateFlow<Book?>(book())
    val chaptersFlow = MutableStateFlow(emptyList<BookChapter>())
    var searchBook: Book? = null
    var groups: List<String> = emptyList()
    var bookmarks = 0
    var notes = 0
    var alternatives = 0

    override fun observeBook(bookId: String): Flow<Book?> = bookFlow
    override fun observeChapters(bookId: String): Flow<List<BookChapter>> = chaptersFlow
    override suspend fun getBook(bookId: String): Book? = bookFlow.value
    override suspend fun getSearchBook(bookId: String): Book? = searchBook
    override suspend fun getChapters(bookId: String): List<BookChapter> = chaptersFlow.value
    override suspend fun updateBook(book: Book) {
        bookFlow.value = book
    }

    override suspend fun groupNames(group: Long): List<String> = groups
    override suspend fun sourceName(sourceId: String): String? = "墨韵书屋"
    override suspend fun alternativeSourceCount(name: String, author: String): Int = alternatives
    override suspend fun insightCounts(bookId: String) = BookInsightCounts(28, 14, 9)
    override suspend fun bookmarkCount(name: String, author: String): Int = bookmarks
    override suspend fun noteCount(name: String, author: String): Int = notes
}

/** 记录命令的顺序：重试必须先删缓存再入队，否则重试会立刻命中旧结果。 */
internal class RecordingChapterCacheHost : CatalogChapterCacheHost {
    val calls = mutableListOf<String>()
    val projection = MutableStateFlow(emptyMap<Int, ChapterCacheProjection>())
    var totalCachedBytes: Long? = null
    var contentLength: Int? = null

    fun setStates(vararg states: Pair<Int, ChapterCacheState>) {
        projection.value = states.associate { (index, state) ->
            index to ChapterCacheProjection(state)
        }
    }

    override fun observeChapterCache(bookId: String) = projection

    override suspend fun enqueueDownload(bookId: String, chapterIndices: List<Int>) {
        calls += "enqueue$chapterIndices"
    }

    override suspend fun deleteCache(bookId: String, chapterIndices: List<Int>) {
        calls += "delete$chapterIndices"
    }

    override suspend fun cachedBytes(bookId: String): Long? = totalCachedBytes
    override suspend fun cachedContentLength(bookId: String, chapterIndex: Int): Int? = contentLength
}

internal class RecordingBookshelfHost : CatalogBookshelfHost {
    val added = mutableListOf<String>()
    val removed = mutableListOf<Pair<String, Boolean>>()

    override suspend fun addToBookshelf(bookId: String) {
        added += bookId
    }

    override suspend fun removeFromBookshelf(bookId: String, deleteLocalFile: Boolean) {
        removed += bookId to deleteLocalFile
    }
}

internal class FakeRelatedBooksHost(
    private val books: List<RelatedBookSummary> = emptyList(),
) : CatalogRelatedBooksHost {
    override suspend fun relatedBooks(bookId: String): List<RelatedBookSummary> = books
}

internal class FakeSourceCatalogStore(private val counts: SourceCatalogCounts) : SourceCatalogStore {
    val httpTtsNames = mutableMapOf<String, String>()

    override fun observeCounts() = flowOf(counts)

    override fun observeHttpTtsName(id: String?) = flowOf(id?.let(httpTtsNames::get))
}
