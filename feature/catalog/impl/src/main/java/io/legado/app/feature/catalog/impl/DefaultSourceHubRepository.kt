package io.legado.app.feature.catalog.impl

import io.legado.app.data.entities.SourceCatalogCounts
import io.legado.app.feature.catalog.api.SourceCatalogCount
import io.legado.app.feature.catalog.api.SourceCatalogKind
import io.legado.app.feature.catalog.api.SourceHubQuery
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * 画板 D-00 的计数投影。Room 是唯一真相源，一次子查询取齐九类，
 * 任一张表变更都会重新发射。
 */
internal class DefaultSourceHubRepository(
    private val store: SourceCatalogStore,
    private val preferencesHost: CatalogReadAloudPreferencesHost,
) : SourceHubQuery {

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun observeSourceCatalog() = preferencesHost.observeDefaultHttpTtsId()
        .flatMapLatest { ttsId ->
            store.observeHttpTtsName(ttsId).flatMapLatest { ttsName ->
                store.observeCounts().map { it.toCatalogCounts(ttsName) }
            }
        }
}

private fun SourceCatalogCounts.toCatalogCounts(
    defaultHttpTtsName: String?,
): List<SourceCatalogCount> = listOf(
    SourceCatalogCount(
        kind = SourceCatalogKind.BookSource,
        total = bookSourceTotal,
        enabled = bookSourceEnabled,
        // 失效标记由校验流程写进 bookSourceGroup 并落库，重启后仍然成立。
        // 0 的含义是「没有书源被标记失效」，不是「全部校验通过」。见盘点 §6.1。
        unhealthy = bookSourceUnhealthy,
    ),
    SourceCatalogCount(
        kind = SourceCatalogKind.RssSource,
        total = rssSourceTotal,
        enabled = rssSourceEnabled,
    ),
    SourceCatalogCount(
        kind = SourceCatalogKind.HttpTts,
        total = httpTtsTotal,
        defaultName = defaultHttpTtsName,
    ),
    SourceCatalogCount(
        kind = SourceCatalogKind.ReplaceRule,
        total = replaceRuleTotal,
        enabled = replaceRuleEnabled,
    ),
    SourceCatalogCount(
        kind = SourceCatalogKind.TxtTocRule,
        total = txtTocRuleTotal,
        builtIn = txtTocRuleBuiltIn,
    ),
    SourceCatalogCount(
        kind = SourceCatalogKind.DictRule,
        total = dictRuleTotal,
        enabled = dictRuleEnabled,
    ),
    SourceCatalogCount(
        kind = SourceCatalogKind.ContentHighlightRule,
        total = contentHighlightTotal,
        enabled = contentHighlightEnabled,
    ),
    SourceCatalogCount(
        kind = SourceCatalogKind.TagHighlightRule,
        total = tagHighlightTotal,
        enabled = tagHighlightEnabled,
    ),
    SourceCatalogCount(
        kind = SourceCatalogKind.RuleSubscription,
        total = ruleSubscriptionTotal,
    ),
)
