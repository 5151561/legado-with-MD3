package io.legado.app.data.runtime

import io.legado.app.data.entities.DictRule

fun interface DictRuleRuntime {
    suspend fun search(rule: DictRule, word: String): String
}

object DictRuleRuntimeRegistry {
    @Volatile
    lateinit var runtime: DictRuleRuntime
}
