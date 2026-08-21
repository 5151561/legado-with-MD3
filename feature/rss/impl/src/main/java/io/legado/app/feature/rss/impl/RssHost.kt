package io.legado.app.feature.rss.impl

import io.legado.app.data.entities.RssSource

/**
 * App shell seams for the RSS implementation: the rule engine that evaluates a source's `sortUrl`
 * script, and the source-deletion owner that also clears the runtime source-variable cache.
 */
interface RssSourceScriptHost {
    suspend fun evaluateSourceScript(source: RssSource, script: String): String?
}

interface RssSourceRemovalHost {
    suspend fun deleteSource(sourceId: String)
}
