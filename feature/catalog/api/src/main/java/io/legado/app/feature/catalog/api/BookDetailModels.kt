package io.legado.app.feature.catalog.api

/**
 * 书籍详情（画板 S-04 / S-04a）的数据模型。
 *
 * 契约里没有「从哪个入口进来」这类参数：书架、搜索、阅读器、阅读记录、导入五个入口
 * 共用同一个路由与同一份状态，入口差异只体现在导航。
 */
data class BookDetailRequest(
    /** 书籍标识，取 `Book.bookUrl`。 */
    val bookId: String,
)

/** 人物 · 知识 · 事件三项的条数，详情页合并成一行入口展示。 */
data class BookInsightCounts(
    val characters: Int = 0,
    val knowledge: Int = 0,
    val events: Int = 0,
)

/** 相关推荐里的一本书。详情页只需要能导航过去，不需要完整书籍信息。 */
data class RelatedBookSummary(
    val bookId: String,
    val title: String,
)

data class BookDetailSnapshot(
    val bookId: String,
    val name: String,
    val author: String,
    /** 分类标签，保持书源给出的顺序。连载状态也在其中——`Book` 没有独立字段。 */
    val kinds: List<String> = emptyList(),
    val intro: String? = null,
    val coverUrl: String? = null,
    val isLocal: Boolean = false,
    val inBookshelf: Boolean = false,
    /** 所属分组名。不在书架或未分组时为空列表。 */
    val groupNames: List<String> = emptyList(),
    val totalChapterCount: Int = 0,
    val cachedChapterCount: Int = 0,
    val latestChapterTitle: String? = null,
    /** 当前阅读到的章节序号；未开始阅读为 0。 */
    val currentChapterIndex: Int = 0,
    val currentChapterTitle: String? = null,
    /** 全书阅读进度 0f..1f；null 表示尚未开始阅读。 */
    val progress: Float? = null,
    /** 当前书源名；本地书为 null。 */
    val sourceName: String? = null,
    /** 可换的候选源数量。换源本身走画板 S-08 的统一组件，不在本 api。 */
    val alternativeSourceCount: Int = 0,
    val insights: BookInsightCounts = BookInsightCounts(),
    val related: List<RelatedBookSummary> = emptyList(),
)

sealed interface BookDetailQueryState {
    data object Loading : BookDetailQueryState
    data class Data(val snapshot: BookDetailSnapshot) : BookDetailQueryState
    data class Failed(val retryable: Boolean) : BookDetailQueryState
}

/**
 * 移出书架的影响面（画板 S-04a 的确认框）。
 *
 * 设计规则要求确认框说清对象、影响范围与体积，所以这里给的是**组成那句话的数**，
 * 句子由 UI 组装。字段可空表示统计不可得，UI 应当省略该分句而不是显示 0。
 */
data class BookRemovalImpact(
    val bookName: String,
    /** 会一并丢失的阅读进度 0f..1f；null 表示尚未开始阅读。 */
    val progress: Float? = null,
    val bookmarkCount: Int = 0,
    val noteCount: Int = 0,
    val cachedChapterCount: Int = 0,
    /** 已缓存正文占用字节；null 表示未能统计。 */
    val cachedBytes: Long? = null,
    /**
     * 本地书的文件路径；非本地书为 null。
     *
     * 非空时确认框出现「同时删除本地文件」勾选——它是第二意图，默认不选。
     */
    val localFilePath: String? = null,
)
