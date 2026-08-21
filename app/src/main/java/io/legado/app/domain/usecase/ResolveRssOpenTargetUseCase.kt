package io.legado.app.domain.usecase

import com.script.rhino.runScriptWithContext
import io.legado.app.data.entities.RssSource

sealed interface LegacyRssOpenTarget {
    data class Sort(val sourceId: String) : LegacyRssOpenTarget
    data class Read(
        val title: String?,
        val origin: String,
        val startPage: Boolean,
    ) : LegacyRssOpenTarget
    data class External(val url: String) : LegacyRssOpenTarget
}

/** Keeps legacy JS and single-URL semantics outside the feature compatibility adapter. */
class ResolveRssOpenTargetUseCase {
    suspend operator fun invoke(source: RssSource): LegacyRssOpenTarget {
        if (!source.singleUrl) {
            return if (source.startHtml.isNullOrBlank()) {
                LegacyRssOpenTarget.Sort(source.sourceUrl)
            } else {
                LegacyRssOpenTarget.Read(source.sourceName, source.sourceUrl, startPage = true)
            }
        }

        val resolved = resolveSingleUrl(source)
        return if (resolved.startsWith("http", ignoreCase = true)) {
            LegacyRssOpenTarget.Read(source.sourceName, resolved, startPage = false)
        } else {
            LegacyRssOpenTarget.External(resolved)
        }
    }

    private suspend fun resolveSingleUrl(source: RssSource): String {
        var sortUrl = source.sortUrl
        if (!sortUrl.isNullOrBlank()) {
            if (sortUrl.startsWith("<js>") || sortUrl.startsWith("@js:")) {
                val script = if (sortUrl.startsWith("@")) {
                    sortUrl.substring(4)
                } else {
                    sortUrl.substring(4, sortUrl.lastIndexOf("<"))
                }
                val result = runScriptWithContext { source.evalJS(script)?.toString() }
                if (!result.isNullOrBlank()) sortUrl = result
            }
            return if (sortUrl.contains("::")) sortUrl.substringAfter("::") else sortUrl
        }
        return source.sourceUrl
    }
}
