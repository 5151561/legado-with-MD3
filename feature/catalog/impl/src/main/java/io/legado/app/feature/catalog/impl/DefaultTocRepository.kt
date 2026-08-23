package io.legado.app.feature.catalog.impl

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.feature.catalog.api.CatalogCommandResult
import io.legado.app.feature.catalog.api.ChapterCacheState
import io.legado.app.feature.catalog.api.ChapterSelection
import io.legado.app.feature.catalog.api.TocCacheFilter
import io.legado.app.feature.catalog.api.TocChapterSnapshot
import io.legado.app.feature.catalog.api.TocCommands
import io.legado.app.feature.catalog.api.TocQuery
import io.legado.app.feature.catalog.api.TocQueryState
import io.legado.app.feature.catalog.api.TocRequest
import io.legado.app.feature.catalog.api.TocSnapshot
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

/**
 * 目录业务实现，同时服务阅读器内目录（S-06a）与目录管理页（S-06b）。
 *
 * Room 给目录本身，[CatalogChapterCacheHost] 给缓存态——落盘与下载队列都在 app shell。
 */
internal class DefaultTocRepository(
    private val store: TocStore,
    private val cacheHost: CatalogChapterCacheHost,
) : TocQuery, TocCommands {

    override fun observeToc(request: TocRequest) = flow {
        emit(TocQueryState.Loading)
        emitAll(
            combine(
                store.observeBook(request.bookId),
                store.observeChapters(request.bookId),
                cacheHost.observeChapterCache(request.bookId),
            ) { book, chapters, cache ->
                if (book == null) {
                    TocQueryState.Failed(retryable = false)
                } else {
                    TocQueryState.Data(snapshot(request, book, chapters, cache))
                }
            }.catch { emit(TocQueryState.Failed(retryable = true)) },
        )
    }

    private suspend fun snapshot(
        request: TocRequest,
        book: Book,
        chapters: List<BookChapter>,
        cache: Map<Int, ChapterCacheProjection>,
    ): TocSnapshot {
        val reversed = book.getReverseToc()
        val all = chapters.map { it.toSnapshot(book, cache[it.index]) }
        val ordered = if (reversed) all.asReversed() else all
        return TocSnapshot(
            bookId = book.bookUrl,
            bookName = book.name,
            chapters = ordered.filter { it.matches(request) },
            // 计数一律按全书，不随过滤变化——筹码上的数字要在过滤后仍然稳定。
            totalChapterCount = all.size,
            cachedChapterCount = all.count { it.cacheState == ChapterCacheState.Cached },
            // 与 TocCacheFilter.NotCached 同口径：「未缓存」是「不是已缓存」，
            // 失败与下载中都算在内。两处口径不同会让筹码上的数和筛出来的条数对不上。
            notCachedChapterCount = all.count { it.cacheState != ChapterCacheState.Cached },
            failedChapterCount = all.count { it.cacheState == ChapterCacheState.Failed },
            currentChapterIndex = if (book.hasStartedReading()) book.durChapterIndex else -1,
            currentChapterProgress = currentChapterProgress(book),
            reversed = reversed,
        )
    }

    /**
     * 章内进度按「读到的位置 / 已落盘正文字符数」算。
     *
     * 未缓存的章节没有可依据的长度——精确进度要读阅读会话，而 `ReaderSession` 接缝尚未模块化，
     * 这里返回 null 而不是猜一个数。
     */
    private suspend fun currentChapterProgress(book: Book): Float? {
        if (!book.hasStartedReading()) return null
        val length = cacheHost.cachedContentLength(book.bookUrl, book.durChapterIndex) ?: return null
        if (length <= 0) return null
        return (book.durChapterPos.toFloat() / length).coerceIn(0f, 1f)
    }

    override suspend fun enqueueDownload(
        bookId: String,
        selection: ChapterSelection,
    ): CatalogCommandResult = runCatching {
        val indices = resolveIndices(bookId, selection)
        if (indices.isNotEmpty()) cacheHost.enqueueDownload(bookId, indices)
    }.toResult()

    override suspend fun retryChapter(bookId: String, chapterId: String): CatalogCommandResult =
        runCatching {
            val index = indexOf(bookId, chapterId) ?: error("章节不存在")
            // 先清缓存再入队：失败重试与「刷新本章」都要求丢掉上一次的结果。
            cacheHost.deleteCache(bookId, listOf(index))
            cacheHost.enqueueDownload(bookId, listOf(index))
        }.toResult()

    override suspend fun deleteCache(
        bookId: String,
        chapterIds: Set<String>,
    ): CatalogCommandResult = runCatching {
        val indices = store.getChapters(bookId).filter { it.url in chapterIds }.map { it.index }
        if (indices.isNotEmpty()) cacheHost.deleteCache(bookId, indices)
    }.toResult()

    override suspend fun setReversed(bookId: String, reversed: Boolean): CatalogCommandResult =
        runCatching {
            val book = store.getBook(bookId) ?: error("书籍不存在")
            book.setReverseToc(reversed)
            store.updateBook(book)
        }.toResult()

    private suspend fun resolveIndices(bookId: String, selection: ChapterSelection): List<Int> =
        when (selection) {
            is ChapterSelection.Ids ->
                store.getChapters(bookId).filter { it.url in selection.chapterIds }.map { it.index }

            ChapterSelection.AllMissing -> {
                val cache = cacheHost.observeChapterCache(bookId).first()
                store.getChapters(bookId)
                    .map { it.index }
                    .filter { cache[it]?.state != ChapterCacheState.Cached }
            }
        }

    private suspend fun indexOf(bookId: String, chapterId: String): Int? =
        store.getChapters(bookId).firstOrNull { it.url == chapterId }?.index
}

/**
 * 是否读过。`durChapterTime` 默认是「此刻」而不是 0，判不了没读过；
 * 章号与章内位置同时为 0 才是真的还没开始。
 */
internal fun Book.hasStartedReading(): Boolean = durChapterIndex > 0 || durChapterPos > 0

private fun BookChapter.toSnapshot(book: Book, cache: ChapterCacheProjection?) = TocChapterSnapshot(
    chapterId = url,
    index = index,
    title = title,
    cacheState = cache?.state ?: ChapterCacheState.NotCached,
    cachedBytes = cache?.cachedBytes,
    downloadProgress = cache?.downloadProgress,
    failureReason = cache?.failureReason,
    isCurrent = book.hasStartedReading() && index == book.durChapterIndex,
    isRead = index < book.durChapterIndex,
)

private fun TocChapterSnapshot.matches(request: TocRequest): Boolean {
    val filterMatches = when (request.filter) {
        TocCacheFilter.All -> true
        TocCacheFilter.NotCached -> cacheState != ChapterCacheState.Cached
        TocCacheFilter.Failed -> cacheState == ChapterCacheState.Failed
    }
    val queryMatches = request.query.isBlank() || title.contains(request.query, ignoreCase = true)
    return filterMatches && queryMatches
}

private fun Result<Unit>.toResult(): CatalogCommandResult = fold(
    onSuccess = { CatalogCommandResult.Success },
    onFailure = { CatalogCommandResult.Failure(it.message) },
)
