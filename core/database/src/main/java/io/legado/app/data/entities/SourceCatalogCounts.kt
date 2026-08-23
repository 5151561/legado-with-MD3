package io.legado.app.data.entities

/**
 * 源与规则枢纽（画板 D-00）的九类对象计数，一次查询取齐。
 *
 * 不是表，是投影。分散到九个 DAO 各查一次会得到九个独立的 Flow，
 * 枢纽那一页要的却是同一时刻的横截面。
 */
data class SourceCatalogCounts(
    val bookSourceTotal: Int,
    val bookSourceEnabled: Int,
    /**
     * 被标记失效的书源数。
     *
     * 判定沿用 `BookSource.getInvalidGroupNames()`：分组名里带「失效」或等于「校验超时」。
     * 这些标记由校验流程写进 `bookSourceGroup` 并落库，因此重启后仍然成立。
     * 注意 0 的含义是「没有书源被标记失效」，不是「全部校验通过」——没校验过的书源不带标记。
     */
    val bookSourceUnhealthy: Int,
    val rssSourceTotal: Int,
    val rssSourceEnabled: Int,
    val httpTtsTotal: Int,
    val replaceRuleTotal: Int,
    val replaceRuleEnabled: Int,
    val txtTocRuleTotal: Int,
    /** 内置规则以负数 id 标记，见 `TxtTocRuleDao.deleteDefault`。 */
    val txtTocRuleBuiltIn: Int,
    val dictRuleTotal: Int,
    val dictRuleEnabled: Int,
    val contentHighlightTotal: Int,
    val contentHighlightEnabled: Int,
    val tagHighlightTotal: Int,
    val tagHighlightEnabled: Int,
    val ruleSubscriptionTotal: Int,
)
