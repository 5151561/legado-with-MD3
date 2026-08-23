package io.legado.app.feature.catalog.impl

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.feature.catalog.api.BookDetailCommands
import io.legado.app.feature.catalog.api.BookDetailQuery
import io.legado.app.feature.catalog.api.BookDetailQueryState
import io.legado.app.feature.catalog.api.BookDetailRequest
import io.legado.app.feature.catalog.api.BookDetailSnapshot
import io.legado.app.feature.catalog.api.BookRemovalImpact
import io.legado.app.feature.catalog.api.CatalogCommandResult
import io.legado.app.feature.catalog.api.ChapterCacheState
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

/**
 * 书籍详情业务实现（画板 S-04 / S-04a）。
 *
 * Room 给书籍本体与各项计数；加入 / 移出书架与相关推荐留在 app shell——
 * 前者连带清阅读会话与本地文件，后者要跑书源规则并联网。
 */
internal class DefaultBookDetailRepository(
    private val store: BookDetailStore,
    private val cacheHost: CatalogChapterCacheHost,
    private val bookshelfHost: CatalogBookshelfHost,
    private val relatedBooksHost: CatalogRelatedBooksHost,
) : BookDetailQuery, BookDetailCommands {

    override fun observeBookDetail(request: BookDetailRequest) = flow {
        emit(BookDetailQueryState.Loading)
        emitAll(
            combine(
                store.observeBook(request.bookId),
                store.observeChapters(request.bookId),
            ) { shelfBook, chapters ->
                val book = shelfBook ?: store.getSearchBook(request.bookId)
                if (book == null) {
                    BookDetailQueryState.Failed(retryable = false)
                } else {
                    BookDetailQueryState.Data(
                        snapshot(book, chapters, inBookshelf = shelfBook != null),
                    )
                }
            }.catch { emit(BookDetailQueryState.Failed(retryable = true)) },
        )
    }

    /**
     * [inBookshelf] 由「books 表里有没有」决定。
     *
     * 从搜索进详情时书还没入库，落到 `searchBooks` 的暂存记录上——五个入口共用一个路由，
     * 页面就不能只认书架里的书。
     */
    private suspend fun snapshot(
        book: Book,
        chapters: List<BookChapter>,
        inBookshelf: Boolean,
    ): BookDetailSnapshot {
        val isLocal = book.isLocalBook()
        return BookDetailSnapshot(
            bookId = book.bookUrl,
            name = book.name,
            author = book.author,
            kinds = book.getKindList(),
            intro = book.getDisplayIntro(),
            coverUrl = book.getDisplayCover(),
            isLocal = isLocal,
            inBookshelf = inBookshelf,
            groupNames = if (inBookshelf) store.groupNames(book.group) else emptyList(),
            totalChapterCount = chapters.size,
            cachedChapterCount = cachedCount(book.bookUrl, chapters),
            latestChapterTitle = book.latestChapterTitle,
            currentChapterIndex = book.durChapterIndex,
            currentChapterTitle = book.durChapterTitle,
            progress = progress(book, chapters.size),
            sourceName = if (isLocal) null else store.sourceName(book.origin),
            alternativeSourceCount = store.alternativeSourceCount(book.name, book.author),
            insights = store.insightCounts(book.bookUrl),
            related = relatedBooksHost.relatedBooks(book.bookUrl),
        )
    }

    /** 全书进度按「读到第几章 / 共几章」算，与目录页的章内进度是两个口径。 */
    private fun progress(book: Book, totalChapters: Int): Float? {
        if (!book.hasStartedReading() || totalChapters <= 0) return null
        return (book.durChapterIndex.toFloat() / totalChapters).coerceIn(0f, 1f)
    }

    private suspend fun cachedCount(bookId: String, chapters: List<BookChapter>): Int {
        if (chapters.isEmpty()) return 0
        val cache = cacheHost.observeChapterCache(bookId).first()
        return chapters.count { cache[it.index]?.state == ChapterCacheState.Cached }
    }

    override suspend fun removalImpact(bookId: String): BookRemovalImpact? {
        val book = store.getBook(bookId) ?: return null
        val chapters = store.getChapters(bookId)
        return BookRemovalImpact(
            bookName = book.name,
            progress = progress(book, chapters.size),
            bookmarkCount = store.bookmarkCount(book.name, book.author),
            noteCount = store.noteCount(book.name, book.author),
            cachedChapterCount = cachedCount(bookId, chapters),
            cachedBytes = cacheHost.cachedBytes(bookId),
            // 本地书的 bookUrl 就是文件 uri；非本地书没有可删的文件，
            // 确认框里那个勾选也就不该出现。
            localFilePath = book.bookUrl.takeIf { book.isLocalBook() },
        )
    }

    override suspend fun addToBookshelf(bookId: String): CatalogCommandResult =
        runCatching { bookshelfHost.addToBookshelf(bookId) }.toResult()

    override suspend fun removeFromBookshelf(
        bookId: String,
        deleteLocalFile: Boolean,
    ): CatalogCommandResult =
        runCatching { bookshelfHost.removeFromBookshelf(bookId, deleteLocalFile) }.toResult()

    override suspend fun moveToGroup(bookId: String, groupId: Long): CatalogCommandResult =
        updateBook(bookId) { it.group = groupId }

    override suspend fun updateCover(bookId: String, coverUrl: String?): CatalogCommandResult =
        updateBook(bookId) { it.customCoverUrl = coverUrl }

    override suspend fun updateRemark(bookId: String, remark: String?): CatalogCommandResult =
        updateBook(bookId) { it.remark = remark?.takeIf(String::isNotBlank) }

    private suspend fun updateBook(
        bookId: String,
        block: (Book) -> Unit,
    ): CatalogCommandResult = runCatching {
        val book = store.getBook(bookId) ?: error("书籍不存在")
        block(book)
        store.updateBook(book)
    }.toResult()
}

private fun Result<Unit>.toResult(): CatalogCommandResult = fold(
    onSuccess = { CatalogCommandResult.Success },
    onFailure = { CatalogCommandResult.Failure(it.message) },
)
