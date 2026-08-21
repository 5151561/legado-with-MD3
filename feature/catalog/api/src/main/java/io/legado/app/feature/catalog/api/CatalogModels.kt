package io.legado.app.feature.catalog.api

data class CatalogRequest(
    val query: String = "",
    val group: String = "",
)

data class CatalogSourceSummary(
    val id: String,
    val name: String,
    val group: String?,
    val hasLogin: Boolean,
    val exploreEnabled: Boolean,
    val responseTimeMillis: Long,
)

data class CatalogSnapshot(
    val groups: List<String>,
    val sources: List<CatalogSourceSummary>,
)

sealed interface CatalogQueryState {
    data object Loading : CatalogQueryState
    data class Data(val snapshot: CatalogSnapshot) : CatalogQueryState
    data class Failed(val retryable: Boolean) : CatalogQueryState
}

sealed interface CatalogCommandResult {
    data object Success : CatalogCommandResult
    data class Failure(val message: String?) : CatalogCommandResult
}
