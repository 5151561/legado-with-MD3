package io.legado.app.data.entities

/**
 * 「我的」（画板 P-01）里几条摘要各自需要的计数，一次查询取齐。
 *
 * 不是表，是投影。理由同 [SourceCatalogCounts]：这一页要的是同一时刻的横截面，
 * 分散成四条 Flow 会让四行摘要各自抖动。
 */
data class ProfileCounts(
    val bookmarkTotal: Int,
    /** 有书签的书本数，按书名 + 作者去重。 */
    val bookmarkBookTotal: Int,
    /** 划线笔记数。与书签同属「书签与笔记」那一行。 */
    val markingTotal: Int,
    /** 统计窗口内的阅读时长，单位毫秒。窗口由调用方给的起始日期决定。 */
    val windowReadMillis: Long,
    val aiConversationTotal: Int,
)
