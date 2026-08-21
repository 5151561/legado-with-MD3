package io.legado.app.data.repository

import io.legado.app.data.AppDatabase
import io.legado.app.data.dao.BookChapterDao
import io.legado.app.data.dao.BookDao
import io.legado.app.data.dao.GroupBookCount
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.model.BookshelfBookRecord
import io.legado.app.help.book.applyTagGroupRulesForBook
import io.legado.app.model.ReadBook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class BookRepository(
    private val bookDao: BookDao,
    private val bookChapterDao: BookChapterDao,
    private val appDb: AppDatabase,
) {
    private val entityPersistence = BookEntityPersistence(bookDao, bookChapterDao)
    fun flowBook(bookUrl: String): Flow<Book?> {
        return bookDao.flowGetBook(bookUrl)
    }

    fun flowChapters(bookUrl: String): Flow<List<BookChapter>> {
        return bookChapterDao.getChapterListFlow(bookUrl)
    }

    fun getAllBooks(): Flow<List<Book>> {
        return bookDao.flowAll()
    }

    suspend fun getBookCoverByNameAndAuthor(bookName: String, bookAuthor: String): String? {
        return withContext(Dispatchers.IO) {
            bookDao.getBook(bookName, bookAuthor)?.getDisplayCover()
        }
    }

    suspend fun getChapterTitle(bookName: String, bookAuthor: String, chapterIndex: Int): String? {
        return withContext(Dispatchers.IO) {
            val book = bookDao.getBook(bookName, bookAuthor)
            val bookUrl = book?.bookUrl
            if (bookUrl.isNullOrEmpty()) return@withContext null

            bookChapterDao.getChapterTitleByUrlAndIndex(bookUrl, chapterIndex)
        }
    }

    suspend fun getBook(bookUrl: String): Book? {
        return withContext(Dispatchers.IO) {
            bookDao.getBook(bookUrl)
        }
    }

    suspend fun getBook(name: String, author: String): Book? {
        return withContext(Dispatchers.IO) {
            bookDao.getBook(name, author)
        }
    }

    suspend fun getShelfBookConflict(name: String, author: String): Book? {
        return withContext(Dispatchers.IO) {
            bookDao.getShelfBookConflict(name, author)
        }
    }

    fun flowBookShelfByGroup(groupId: Long): Flow<List<BookshelfBookRecord>> {
        return bookDao.flowBookShelfByGroup(groupId)
    }

    fun flowSystemGroupCounts(): Flow<List<GroupBookCount>> {
        return bookDao.flowSystemGroupCounts()
    }

    fun flowAllBookShelfCount(): Flow<Int> {
        return bookDao.flowAllBookShelfCount()
    }

    fun flowUserGroupBookCount(groupId: Long): Flow<Int> {
        return bookDao.flowUserGroupBookCount(groupId)
    }

    fun flowGroupPreview(groupId: Long): Flow<List<BookshelfBookRecord>> {
        return bookDao.flowGroupPreview(groupId)
    }

    suspend fun getChapter(bookUrl: String, index: Int): BookChapter? {
        return withContext(Dispatchers.IO) {
            bookChapterDao.getChapter(bookUrl, index)
        }
    }

    suspend fun getChapterCount(bookUrl: String): Int {
        return withContext(Dispatchers.IO) {
            bookChapterDao.getChapterCount(bookUrl)
        }
    }

    suspend fun getVolumeCount(bookUrl: String): Int {
        return withContext(Dispatchers.IO) {
            bookChapterDao.getVolumeCount(bookUrl)
        }
    }

    suspend fun getChapters(bookUrl: String): List<BookChapter> {
        return withContext(Dispatchers.IO) {
            bookChapterDao.getChapterList(bookUrl)
        }
    }

    suspend fun getChapters(bookUrl: String, start: Int, end: Int): List<BookChapter> {
        return withContext(Dispatchers.IO) {
            bookChapterDao.getChapterList(bookUrl, start, end)
        }
    }

    suspend fun update(vararg book: Book) {
        withContext(Dispatchers.IO) {
            bookDao.update(*book)
        }
    }

    suspend fun getMinOrder(): Int {
        return withContext(Dispatchers.IO) {
            bookDao.minOrder
        }
    }

    suspend fun insert(book: Book) {
        withContext(Dispatchers.IO) {
            bookDao.insert(book)
        }
    }

    /**
     * Book 的唯一兼容保存入口。保持既有标签规则与 insert-or-update 语义，避免 Room entity
     * 主动取得全局数据库实例。正式 feature impl 建立后由同一契约继续约束。
     */
    suspend fun save(book: Book) {
        withContext(Dispatchers.IO) {
            entityPersistence.save(book, ::applyTagGroupRulesForBook)
        }
    }

    suspend fun insertChapters(vararg chapters: BookChapter) {
        withContext(Dispatchers.IO) {
            bookChapterDao.insert(*chapters)
        }
    }

    suspend fun getHasUpdateBooks(): List<Book> {
        return withContext(Dispatchers.IO) {
            bookDao.hasUpdateBooks
        }
    }

    suspend fun getAll(): List<Book> {
        return withContext(Dispatchers.IO) {
            bookDao.getAll()
        }
    }

    suspend fun getLastReadBook(): Book? {
        return withContext(Dispatchers.IO) {
            bookDao.lastReadBook
        }
    }

    suspend fun replace(oldBook: Book, newBook: Book) {
        withContext(Dispatchers.IO) {
            bookDao.replace(oldBook, newBook)
        }
    }

    suspend fun delete(vararg book: Book) {
        withContext(Dispatchers.IO) {
            bookDao.delete(*book)
        }
    }

    /** 保持旧 Book.delete() 的会话清理、章节删除、书籍删除顺序。 */
    suspend fun deleteBook(book: Book) {
        withContext(Dispatchers.IO) {
            if (ReadBook.isCurrentBook(book.bookUrl)) {
                ReadBook.clearCurrentBook()
            }
            entityPersistence.delete(book)
        }
    }

    suspend fun deleteChaptersByBook(bookUrl: String) {
        withContext(Dispatchers.IO) {
            bookChapterDao.delByBook(bookUrl)
        }
    }

    suspend fun replaceChaptersAndUpdateBook(book: Book, chapters: List<BookChapter>) {
        withContext(Dispatchers.IO) {
            appDb.runInTransaction {
                bookChapterDao.delByBook(book.bookUrl)
                bookChapterDao.insert(*chapters.toTypedArray())
                bookDao.update(book)
            }
        }
    }

}

/** Package-visible persistence contract used by repository tests without constructing Room. */
internal class BookEntityPersistence(
    private val bookDao: BookDao,
    private val bookChapterDao: BookChapterDao,
) {
    fun save(book: Book, beforeSave: (Book) -> Unit) {
        beforeSave(book)
        if (bookDao.has(book.bookUrl)) {
            bookDao.update(book)
        } else {
            bookDao.insert(book)
        }
    }

    fun delete(book: Book) {
        bookChapterDao.delByBook(book.bookUrl)
        bookDao.delete(book)
    }
}
