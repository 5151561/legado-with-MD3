package io.legado.app.feature.rss.api

import kotlinx.coroutines.flow.Flow

data class RssRequest(val query: String = "", val group: String = "")

data class RssSourceSummary(
    val id: String,
    val name: String,
    val icon: String?,
    val group: String?,
    val hasLogin: Boolean,
)

data class RssSnapshot(val groups: List<String>, val sources: List<RssSourceSummary>)

sealed interface RssQueryState {
    data object Loading : RssQueryState
    data class Data(val snapshot: RssSnapshot) : RssQueryState
    data class Failed(val retryable: Boolean) : RssQueryState
}

sealed interface RssOpenTarget {
    data class Sort(val sourceId: String) : RssOpenTarget
    data class Read(
        val title: String?,
        val origin: String,
        val link: String? = null,
        val openUrl: String? = null,
        val startPage: Boolean = false,
    ) : RssOpenTarget
    data class External(val url: String) : RssOpenTarget
}

sealed interface RssCommandResult {
    data object Success : RssCommandResult
    data class Failure(val message: String?) : RssCommandResult
}

fun interface RssQuery {
    fun observeSources(request: RssRequest): Flow<RssQueryState>
}

interface RssCommands {
    suspend fun resolveOpenTarget(sourceId: String): Result<RssOpenTarget>
    suspend fun pinSource(sourceId: String): RssCommandResult
    suspend fun disableSource(sourceId: String): RssCommandResult
    suspend fun deleteSource(sourceId: String): RssCommandResult
}
