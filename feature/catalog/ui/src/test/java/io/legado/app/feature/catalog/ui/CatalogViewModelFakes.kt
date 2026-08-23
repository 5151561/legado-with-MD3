package io.legado.app.feature.catalog.ui

import io.legado.app.feature.catalog.api.BookDetailCommands
import io.legado.app.feature.catalog.api.BookDetailQuery
import io.legado.app.feature.catalog.api.BookDetailQueryState
import io.legado.app.feature.catalog.api.BookDetailRequest
import io.legado.app.feature.catalog.api.BookDetailSnapshot
import io.legado.app.feature.catalog.api.BookRemovalImpact
import io.legado.app.feature.catalog.api.CatalogCommandResult
import io.legado.app.feature.catalog.api.ChapterCacheState
import io.legado.app.feature.catalog.api.ChapterSelection
import io.legado.app.feature.catalog.api.TocChapterSnapshot
import io.legado.app.feature.catalog.api.TocCommands
import io.legado.app.feature.catalog.api.TocQuery
import io.legado.app.feature.catalog.api.TocQueryState
import io.legado.app.feature.catalog.api.TocRequest
import io.legado.app.feature.catalog.api.TocSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class FakeBookDetailQuery(
    snapshot: BookDetailSnapshot,
    var impact: BookRemovalImpact? = null,
) : BookDetailQuery {
    val snapshotFlow = MutableStateFlow(snapshot)

    override fun observeBookDetail(request: BookDetailRequest) =
        snapshotFlow.map { BookDetailQueryState.Data(it) as BookDetailQueryState }

    override suspend fun removalImpact(bookId: String) = impact
}

internal class RecordingBookDetailCommands : BookDetailCommands {
    val removed = mutableListOf<Pair<String, Boolean>>()
    var result: CatalogCommandResult = CatalogCommandResult.Success

    override suspend fun addToBookshelf(bookId: String) = result
    override suspend fun removeFromBookshelf(bookId: String, deleteLocalFile: Boolean): CatalogCommandResult {
        removed += bookId to deleteLocalFile
        return result
    }

    override suspend fun moveToGroup(bookId: String, groupId: Long) = result
    override suspend fun updateCover(bookId: String, coverUrl: String?) = result
    override suspend fun updateRemark(bookId: String, remark: String?) = result
}

internal class FakeTocQuery(snapshot: TocSnapshot) : TocQuery {
    val snapshotFlow = MutableStateFlow(snapshot)
    var lastRequest: TocRequest? = null

    override fun observeToc(request: TocRequest) = snapshotFlow.map {
        lastRequest = request
        TocQueryState.Data(it.filtered(request)) as TocQueryState
    }
}

/** 假实现也照 impl 的口径过滤，否则测出来的过滤行为只是测了假数据。 */
private fun TocSnapshot.filtered(request: TocRequest) = copy(
    chapters = chapters.filter {
        when (request.filter) {
            io.legado.app.feature.catalog.api.TocCacheFilter.All -> true
            io.legado.app.feature.catalog.api.TocCacheFilter.NotCached ->
                it.cacheState != ChapterCacheState.Cached

            io.legado.app.feature.catalog.api.TocCacheFilter.Failed ->
                it.cacheState == ChapterCacheState.Failed
        }
    }
)

internal class RecordingTocCommands : TocCommands {
    val calls = mutableListOf<String>()
    var result: CatalogCommandResult = CatalogCommandResult.Success

    override suspend fun enqueueDownload(bookId: String, selection: ChapterSelection): CatalogCommandResult {
        calls += when (selection) {
            ChapterSelection.AllMissing -> "enqueue:allMissing"
            is ChapterSelection.Ids -> "enqueue:${selection.chapterIds.sorted()}"
        }
        return result
    }

    override suspend fun retryChapter(bookId: String, chapterId: String): CatalogCommandResult {
        calls += "retry:$chapterId"
        return result
    }

    override suspend fun deleteCache(bookId: String, chapterIds: Set<String>): CatalogCommandResult {
        calls += "delete:${chapterIds.sorted()}"
        return result
    }

    override suspend fun setReversed(bookId: String, reversed: Boolean): CatalogCommandResult {
        calls += "reversed:$reversed"
        return result
    }
}

internal fun tocChapter(
    id: String,
    index: Int = id.toInt(),
    title: String = "第 ${index + 1} 章",
    state: ChapterCacheState = ChapterCacheState.NotCached,
    cachedBytes: Long? = null,
    isCurrent: Boolean = false,
) = TocChapterSnapshot(
    chapterId = id,
    index = index,
    title = title,
    cacheState = state,
    cachedBytes = cachedBytes,
    isCurrent = isCurrent,
)
