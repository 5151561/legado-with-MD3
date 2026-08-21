package io.legado.app.data.compat

import io.legado.app.data.entities.DictRule
import io.legado.app.data.runtime.DictRuleRuntime
import io.legado.app.model.analyzeRule.AnalyzeRule
import io.legado.app.model.analyzeRule.AnalyzeRule.Companion.setCoroutineContext
import io.legado.app.model.analyzeRule.AnalyzeUrl
import kotlin.coroutines.coroutineContext

object LegacyDictRuleRuntime : DictRuleRuntime {
    override suspend fun search(rule: DictRule, word: String): String {
        val analyzeUrl = AnalyzeUrl(rule.urlRule, key = word, coroutineContext = coroutineContext)
        val body = analyzeUrl.getStrResponseAwait().body
        if (rule.showRule.isBlank()) return body!!
        return AnalyzeRule()
            .setCoroutineContext(coroutineContext)
            .getString(rule.showRule, mContent = body)
    }
}
