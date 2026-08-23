package io.legado.app.feature.catalog.impl

import io.legado.app.data.AppDatabase
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.feature.catalog.api.BookInsightCounts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

internal interface BookDetailStore {
    fun observeBook(bookId: String): Flow<Book?>
    fun observeChapters(bookId: String): Flow<List<BookChapter>>
    suspend fun getBook(bookId: String): Book?

    /** 尚未入库的书（从搜索进详情）落在 `searchBooks` 暂存表里。 */
    suspend fun getSearchBook(bookId: String): Book?

    suspend fun getChapters(bookId: String): List<BookChapter>
    suspend fun updateBook(book: Book)
    suspend fun groupNames(group: Long): List<String>
    suspend fun sourceName(sourceId: String): String?
    suspend fun alternativeSourceCount(name: String, author: String): Int
    suspend fun insightCounts(bookId: String): BookInsightCounts
    suspend fun bookmarkCount(name: String, author: String): Int
    suspend fun noteCount(name: String, author: String): Int
}

internal class RoomBookDetailStore(private val database: AppDatabase) : BookDetailStore {

    override fun observeBook(bookId: String): Flow<Book?> = database.bookDao.flowGetBook(bookId)

    override fun observeChapters(bookId: String): Flow<List<BookChapter>> =
        database.bookChapterDao.getChapterListFlow(bookId)

    override suspend fun getBook(bookId: String): Book? = io { database.bookDao.getBook(bookId) }

    override suspend fun getSearchBook(bookId: String): Book? =
        io { database.searchBookDao.getSearchBook(bookId)?.toBook() }

    override suspend fun getChapters(bookId: String): List<BookChapter> =
        io { database.bookChapterDao.getChapterList(bookId) }

    override suspend fun updateBook(book: Book) = io { database.bookDao.update(book) }

    override suspend fun groupNames(group: Long): List<String> =
        io { database.bookGroupDao.getGroupNames(group) }

    override suspend fun sourceName(sourceId: String): String? =
        io { database.bookSourceDao.getBookSourcePart(sourceId)?.bookSourceName }

    override suspend fun alternativeSourceCount(name: String, author: String): Int =
        io { database.searchBookDao.countEnabledSources(name, author) }

    override suspend fun insightCounts(bookId: String): BookInsightCounts = io {
        BookInsightCounts(
            characters = database.bookKnowledgeDao.countCharacterProfiles(bookId),
            knowledge = database.bookKnowledgeDao.countKnowledgeEntries(bookId),
            events = database.bookKnowledgeDao.countCharacterEvents(bookId),
        )
    }

    override suspend fun bookmarkCount(name: String, author: String): Int =
        io { database.bookmarkDao.countByBook(name, author) }

    override suspend fun noteCount(name: String, author: String): Int =
        io { database.bookMarkingDao.countByBook(name, author) }

    private suspend fun <T> io(block: suspend () -> T): T = withContext(Dispatchers.IO) { block() }
}
