package io.legado.app.feature.catalog.impl

import io.legado.app.data.AppDatabase
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

internal interface TocStore {
    fun observeBook(bookId: String): Flow<Book?>
    fun observeChapters(bookId: String): Flow<List<BookChapter>>
    suspend fun getBook(bookId: String): Book?
    suspend fun getChapters(bookId: String): List<BookChapter>
    suspend fun updateBook(book: Book)
}

internal class RoomTocStore(private val database: AppDatabase) : TocStore {

    override fun observeBook(bookId: String): Flow<Book?> = database.bookDao.flowGetBook(bookId)

    override fun observeChapters(bookId: String): Flow<List<BookChapter>> =
        database.bookChapterDao.getChapterListFlow(bookId)

    override suspend fun getBook(bookId: String): Book? = io { database.bookDao.getBook(bookId) }

    override suspend fun getChapters(bookId: String): List<BookChapter> =
        io { database.bookChapterDao.getChapterList(bookId) }

    override suspend fun updateBook(book: Book) = io { database.bookDao.update(book) }

    private suspend fun <T> io(block: suspend () -> T): T = withContext(Dispatchers.IO) { block() }
}
