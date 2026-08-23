package io.legado.app.feature.catalog.impl

import io.legado.app.feature.catalog.api.ChapterCacheState
import io.legado.app.feature.catalog.api.ChapterFailureReason
import io.legado.app.feature.catalog.api.RelatedBookSummary
import kotlinx.coroutines.flow.Flow

/**
 * App shell seam for the catalog implementation. Deleting a book source also drops its runtime
 * source variables and its `SourceConfig` entry, so deletion keeps its existing single owner.
 */
interface CatalogSourceRemovalHost {
    suspend fun deleteSource(sourceId: String)
}

/** 某一章的缓存态投影，键为章节序号。 */
data class ChapterCacheProjection(
    val state: ChapterCacheState,
    /** 已落盘正文占用字节；未缓存为 null。 */
    val cachedBytes: Long? = null,
    /** 下载进度 0f..1f；仅下载中有值。 */
    val downloadProgress: Float? = null,
    val failureReason: ChapterFailureReason? = null,
)

/**
 * 章节缓存接缝。
 *
 * 缓存态由两处合成：落盘文件（`BookHelp`）与内存下载队列（`CacheBook`）。
 * 两者都需要 `Context` 与前台服务，因此留在 app shell，impl 只消费投影、只发命令——
 * 与「下载仍由 `CacheBookService` 执行」的既定分工一致。
 */
interface CatalogChapterCacheHost {

    /** 一本书的章节缓存态，随下载队列变化重新发射。键为章节序号。 */
    fun observeChapterCache(bookId: String): Flow<Map<Int, ChapterCacheProjection>>

    /** 入队下载。返回即表示已入队，不表示已下完。 */
    suspend fun enqueueDownload(bookId: String, chapterIndices: List<Int>)

    suspend fun deleteCache(bookId: String, chapterIndices: List<Int>)

    /** 整本已缓存正文占用字节；统计不到返回 null，调用方应当省略体积那一句而不是显示 0。 */
    suspend fun cachedBytes(bookId: String): Long?

    /** 某章已落盘正文的字符数，用于换算章内进度；未缓存返回 null。 */
    suspend fun cachedContentLength(bookId: String, chapterIndex: Int): Int?
}

/**
 * 加入 / 移出书架。
 *
 * 移出会连带清阅读会话、章节表与本地文件，这条链路在 app shell 已有唯一 owner，
 * impl 不复制一份。
 */
interface CatalogBookshelfHost {
    suspend fun addToBookshelf(bookId: String)
    suspend fun removeFromBookshelf(bookId: String, deleteLocalFile: Boolean)
}

/**
 * 朗读偏好接缝。
 *
 * 默认 HTTP TTS 引擎是偏好项，`:core:preferences` 建立前经本接缝读取；
 * 建立之后本接缝随之删除。接缝只出「选中的是哪个 id」，名字由 Room 查——
 * 偏好里存的就是 id，让接缝去查名字等于把两件事绑在一起。
 */
fun interface CatalogReadAloudPreferencesHost {
    fun observeDefaultHttpTtsId(): Flow<String?>
}

/**
 * 相关推荐。数据来自书源的 `ruleBookInfo.relatedBooks` 规则 + 网络抓取，
 * 规则引擎不进 impl。取不到时返回空列表，不是错误。
 */
fun interface CatalogRelatedBooksHost {
    suspend fun relatedBooks(bookId: String): List<RelatedBookSummary>
}
