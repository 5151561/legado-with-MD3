package io.legado.app.feature.catalog.api

/**
 * 源与规则枢纽（画板 D-00）收编的九类对象。
 *
 * 枚举名以「这张表管什么」为准，不沿用旧页面的标题：旧 App 里「订阅源」与「规则订阅」
 * 名字相近、去处不明，是 D-00 要解决的问题之一（见 `catalog-behavior-inventory.md` §6.3）。
 */
enum class SourceCatalogKind {
    BookSource,
    RssSource,
    HttpTts,
    ReplaceRule,
    TxtTocRule,
    DictRule,

    /** 作用于正文文本的高亮规则。 */
    ContentHighlightRule,

    /** 作用于书架标签的高亮规则。 */
    TagHighlightRule,

    /** 规则订阅（`ruleSubs`），旧页面 `ui/rss/subscription/`。 */
    RuleSubscription,
}

/**
 * 一类对象的计数投影。
 *
 * 摘要文案由 UI 组装——api 不表达「312 个 · 启用 208 · 3 个失效」这种句子，
 * 只给出组成它的数。可空字段一律表示**该口径不适用或当前无信号**，与 0 不同义。
 */
data class SourceCatalogCount(
    val kind: SourceCatalogKind,
    val total: Int,
    /** 启用条数；null 表示该类没有启用 / 停用的概念。 */
    val enabled: Int? = null,
    /**
     * 失效条数；null 表示当前没有健康信号。
     *
     * 书源的失效判定来自校验会话的内存态，不落库，重启即失。
     * 在 `catalog-behavior-inventory.md` §6.1 做出选择前，这里恒为 null 是合法的。
     */
    val unhealthy: Int? = null,
    /** 内置条数（TXT 目录规则）；其余为 null。 */
    val builtIn: Int? = null,
    /** 默认项名称（HTTP TTS 的默认引擎）；其余为 null。见盘点 §6.2。 */
    val defaultName: String? = null,
)
