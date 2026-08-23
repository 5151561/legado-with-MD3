package io.legado.app.di

import io.legado.app.data.dao.BookChapterDao
import io.legado.app.data.dao.BookDao
import io.legado.app.data.dao.SearchBookDao
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.domain.gateway.BookCacheDownloadGateway
import io.legado.app.domain.gateway.ReadAloudSettingsGateway
import io.legado.app.domain.usecase.AddToBookshelfUseCase
import io.legado.app.domain.usecase.DeleteBooksUseCase
import io.legado.app.feature.catalog.api.ChapterCacheState
import io.legado.app.feature.catalog.api.ChapterFailureReason
import io.legado.app.feature.catalog.api.RelatedBookSummary
import io.legado.app.feature.catalog.impl.CatalogBookshelfHost
import io.legado.app.feature.catalog.impl.CatalogChapterCacheHost
import io.legado.app.feature.catalog.impl.CatalogReadAloudPreferencesHost
import io.legado.app.feature.catalog.impl.CatalogRelatedBooksHost
import io.legado.app.feature.catalog.impl.CatalogSourceRemovalHost
import io.legado.app.feature.catalog.impl.ChapterCacheProjection
import io.legado.app.help.book.BookHelp
import io.legado.app.help.source.SourceHelp
import io.legado.app.model.CacheBook
import io.legado.app.utils.FileUtils
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

/**
 * App shell seam required by `:feature:catalog:impl`. Source deletion keeps its existing owner in
 * [SourceHelp] because it also clears runtime source variables and the `SourceConfig` entry.
 */
class AppCatalogSourceRemovalHost : CatalogSourceRemovalHost {
    override suspend fun deleteSource(sourceId: String) = withContext(Dispatchers.IO) {
        SourceHelp.deleteBookSource(sourceId)
    }
}

/**
 * 章节缓存接缝的落地。
 *
 * 缓存态由两处合成，与 `BookCacheManageViewModel` 同口径：
 * 落盘用 [BookHelp.isChapterCacheComplete]，运行时态用 [CacheBook] 的下载队列。
 * 两处的优先级也保持一致——正在下载的章节不算已缓存，暂停优先于等待。
 */
class AppCatalogChapterCacheHost(
    private val bookDao: BookDao,
    private val bookChapterDao: BookChapterDao,
    private val downloadGateway: BookCacheDownloadGateway,
) : CatalogChapterCacheHost {

    override fun observeChapterCache(bookId: String): Flow<Map<Int, ChapterCacheProjection>> =
        CacheBook.downloadStateFlow
            .map { buildProjection(bookId) }
            .onStart { emit(buildProjection(bookId)) }
            .flowOn(Dispatchers.IO)

    private fun buildProjection(bookId: String): Map<Int, ChapterCacheProjection> {
        val book = bookDao.getBook(bookId) ?: return emptyMap()
        val chapters = bookChapterDao.getChapterList(bookId)
        val model = CacheBook.cacheBookMap[bookId]
        val bookState = CacheBook.downloadStateFlow.value.books[bookId]
        val failedIndices = CacheBook.errorIndices(bookId)
        return chapters.associate { chapter ->
            val paused = model?.isPaused(chapter.index) == true
            val waiting = !paused && model?.isWaiting(chapter.index) == true
            val downloading = !paused && model?.isDownloading(chapter.index) == true
            val failed = chapter.index in failedIndices
            val cached = !downloading &&
                (chapter.isVolume || BookHelp.isChapterCacheComplete(book, chapter))
            val state = when {
                downloading -> ChapterCacheState.Downloading
                paused -> ChapterCacheState.Paused
                waiting -> ChapterCacheState.Waiting
                cached -> ChapterCacheState.Cached
                failed -> ChapterCacheState.Failed
                else -> ChapterCacheState.NotCached
            }
            chapter.index to ChapterCacheProjection(
                state = state,
                cachedBytes = if (cached) chapterFile(book, chapter)?.length() else null,
                downloadProgress = bookState?.chapterProgress
                    ?.get(chapter.index)
                    ?.fraction
                    ?.takeIf { downloading },
                // 队列只记「这一章失败了」，不记为什么失败。分因由需要队列携带错误类型，
                // 见 docs/dev/catalog-behavior-inventory.md §4.3。
                failureReason = ChapterFailureReason.Unknown.takeIf { state == ChapterCacheState.Failed },
            )
        }
    }

    override suspend fun enqueueDownload(bookId: String, chapterIndices: List<Int>) {
        downloadGateway.start(bookId, chapterIndices)
    }

    override suspend fun deleteCache(bookId: String, chapterIndices: List<Int>) =
        withContext(Dispatchers.IO) {
            val book = bookDao.getBook(bookId) ?: return@withContext
            val wanted = chapterIndices.toSet()
            bookChapterDao.getChapterList(bookId)
                .filter { it.index in wanted }
                .forEach { BookHelp.delContent(book, it) }
        }

    override suspend fun cachedBytes(bookId: String): Long? = withContext(Dispatchers.IO) {
        val book = bookDao.getBook(bookId) ?: return@withContext null
        val folder = File(FileUtils.getPath(File(BookHelp.cachePath), book.getFolderName()))
        if (!folder.isDirectory) return@withContext null
        folder.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    override suspend fun cachedContentLength(bookId: String, chapterIndex: Int): Int? =
        withContext(Dispatchers.IO) {
            val book = bookDao.getBook(bookId) ?: return@withContext null
            val chapter = bookChapterDao.getChapter(bookId, chapterIndex)
                ?: return@withContext null
            BookHelp.getCachedContentLength(book, chapter)
        }

    private fun chapterFile(book: Book, chapter: BookChapter): File? {
        val file = File(
            FileUtils.getPath(File(BookHelp.cachePath), book.getFolderName(), chapter.getFileName())
        )
        return file.takeIf { it.isFile }
    }
}

/**
 * 加入 / 移出书架。两条链路都已有唯一 owner：加入走 [AddToBookshelfUseCase]，
 * 移出走 [DeleteBooksUseCase]（它负责清阅读会话、章节表与本地文件）。
 */
class AppCatalogBookshelfHost(
    private val bookDao: BookDao,
    private val searchBookDao: SearchBookDao,
    private val addToBookshelfUseCase: AddToBookshelfUseCase,
    private val deleteBooksUseCase: DeleteBooksUseCase,
) : CatalogBookshelfHost {

    override suspend fun addToBookshelf(bookId: String) = withContext(Dispatchers.IO) {
        // 已在书架的书没什么可做；不在书架时书还留在 searchBooks 暂存表里。
        if (bookDao.getBook(bookId) != null) return@withContext
        val searchBook = searchBookDao.getSearchBook(bookId) ?: error("书籍不存在")
        addToBookshelfUseCase.execute(searchBook)
    }

    override suspend fun removeFromBookshelf(bookId: String, deleteLocalFile: Boolean) {
        deleteBooksUseCase.execute(setOf(bookId), deleteLocalFile)
    }
}

/**
 * 默认 HTTP TTS 引擎的偏好读取，转发给既有的 [ReadAloudSettingsGateway]。
 *
 * `ttsEngine` 一个字段承载三种引擎的选择：系统引擎与云引擎存的是 JSON，
 * HTTP TTS 存的是引擎 id 原文。这里原样透出，由 Room 决定查不查得到——
 * 选的是别的引擎时自然查不到名字，就没有默认引擎可写。
 */
class AppCatalogReadAloudPreferencesHost(
    private val readAloudSettingsGateway: ReadAloudSettingsGateway,
) : CatalogReadAloudPreferencesHost {
    override fun observeDefaultHttpTtsId() =
        readAloudSettingsGateway.settings.map { it.ttsEngine }
}

/**
 * 相关推荐尚未接线。
 *
 * 数据来自书源的 `ruleBookInfo.relatedBooks` 规则 + 网络抓取。旧详情页删除时，
 * 那段私有加载逻辑一并没了，需要照规则重写成用例。
 * 在那之前返回空列表——已登记在 docs/dev/catalog-behavior-inventory.md §4.1。
 */
class AppCatalogRelatedBooksHost : CatalogRelatedBooksHost {
    override suspend fun relatedBooks(bookId: String): List<RelatedBookSummary> = emptyList()
}
