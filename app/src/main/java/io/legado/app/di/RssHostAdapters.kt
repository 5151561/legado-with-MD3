package io.legado.app.di

import com.script.rhino.runScriptWithContext
import io.legado.app.data.compat.evalJS
import io.legado.app.data.entities.RssSource
import io.legado.app.data.repository.RssRepository
import io.legado.app.feature.rss.impl.RssSourceRemovalHost
import io.legado.app.feature.rss.impl.RssSourceScriptHost

/**
 * App shell seams required by `:feature:rss:impl`. They only forward to the existing rule engine
 * and to the single owner of source deletion; the RSS business rules live in the feature
 * implementation.
 */
class AppRssSourceScriptHost : RssSourceScriptHost {
    override suspend fun evaluateSourceScript(source: RssSource, script: String): String? =
        runScriptWithContext { source.evalJS(script)?.toString() }
}

class AppRssSourceRemovalHost(
    private val repository: RssRepository,
) : RssSourceRemovalHost {
    override suspend fun deleteSource(sourceId: String) = repository.deleteByIds(setOf(sourceId))
}
