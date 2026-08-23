package io.legado.app.feature.catalog.api

import kotlinx.coroutines.flow.Flow

fun interface TocQuery {
    fun observeToc(request: TocRequest): Flow<TocQueryState>
}

interface TocCommands {
    /**
     * 入队下载。下载本身仍由 `CacheBookService` 执行，本命令只表达意图与作用对象；
     * 返回 [CatalogCommandResult.Success] 表示已入队，不表示已下完。
     */
    suspend fun enqueueDownload(bookId: String, selection: ChapterSelection): CatalogCommandResult

    /** 重下单章：先清掉该章缓存再入队，用于失败项的就地重试与「刷新本章」。 */
    suspend fun retryChapter(bookId: String, chapterId: String): CatalogCommandResult

    /**
     * 删除选中章节的正文缓存。危险操作，二次确认由 UI 负责。
     *
     * 这里收的是明确的章节集合而不是 [ChapterSelection]——[ChapterSelection.AllMissing]
     * 对删除没有意义，整本删缓存等价于全选后调用本命令。
     */
    suspend fun deleteCache(bookId: String, chapterIds: Set<String>): CatalogCommandResult

    /** 目录正序 / 倒序。按书持久化。 */
    suspend fun setReversed(bookId: String, reversed: Boolean): CatalogCommandResult
}
