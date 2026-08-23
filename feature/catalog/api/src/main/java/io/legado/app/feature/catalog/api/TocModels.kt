package io.legado.app.feature.catalog.api

/**
 * 目录的数据模型，同时服务阅读器内目录（画板 S-06a）与目录管理页（画板 S-06b）。
 *
 * 两块画板的职责不同——前者只跳转，后者做批量——但读的是同一份目录投影，
 * 差别在于各自折叠多少信息。因此本模型保留完整的缓存状态，由 UI 决定显示几档
 * （见 `catalog-behavior-inventory.md` §6.4）。
 */
enum class ChapterCacheState {
    NotCached,
    Cached,

    /** 已入队，尚未开始。 */
    Waiting,
    Downloading,
    Paused,
    Failed,
}

/** 章节缓存失败的原因。文案由 UI 决定，api 不带本地化字符串。 */
enum class ChapterFailureReason {
    /** 抓到了，但正文为空。 */
    EmptyContent,
    Network,
    Unknown,
}

data class TocChapterSnapshot(
    /** 章节标识，取 `BookChapter.url`。 */
    val chapterId: String,
    /** 在全书中的序号，与目录是否倒序无关。 */
    val index: Int,
    val title: String,
    val cacheState: ChapterCacheState,
    /** 已缓存正文字节；未缓存为 null。 */
    val cachedBytes: Long? = null,
    /** 下载进度 0f..1f；仅 [ChapterCacheState.Downloading] 有值。 */
    val downloadProgress: Float? = null,
    /** 仅 [ChapterCacheState.Failed] 有值。 */
    val failureReason: ChapterFailureReason? = null,
    val isCurrent: Boolean = false,
    val isRead: Boolean = false,
)

/** 目录管理页的过滤器。全部 / 未缓存 / 失败，与画板 S-06b 的三个筹码一一对应。 */
enum class TocCacheFilter {
    All,
    NotCached,
    Failed,
}

data class TocRequest(
    val bookId: String,
    val query: String = "",
    val filter: TocCacheFilter = TocCacheFilter.All,
)

data class TocSnapshot(
    val bookId: String,
    val bookName: String,
    /** 已按 [TocRequest] 的查询与过滤裁剪，且按 [reversed] 排好序。 */
    val chapters: List<TocChapterSnapshot> = emptyList(),
    /** 下列计数针对**全书**，不随过滤变化——筹码上的数字要在过滤后仍然稳定。 */
    val totalChapterCount: Int = 0,
    val cachedChapterCount: Int = 0,
    val notCachedChapterCount: Int = 0,
    val failedChapterCount: Int = 0,
    /** 当前阅读章节的序号；未开始阅读为 -1。 */
    val currentChapterIndex: Int = -1,
    /** 当前章的章内进度 0f..1f；无当前章时为 null。 */
    val currentChapterProgress: Float? = null,
    val reversed: Boolean = false,
)

sealed interface TocQueryState {
    data object Loading : TocQueryState
    data class Data(val snapshot: TocSnapshot) : TocQueryState
    data class Failed(val retryable: Boolean) : TocQueryState
}

/**
 * 下载命令的作用对象。
 *
 * 书籍详情页的一键下载没有目录在手，只能表达「全部未缓存」；目录管理页选了哪些就是哪些。
 * 用 null 兼表两者会让「一本都没选」和「全都要」撞在一起。
 */
sealed interface ChapterSelection {
    data object AllMissing : ChapterSelection
    data class Ids(val chapterIds: Set<String>) : ChapterSelection
}
