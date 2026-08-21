package io.legado.app.feature.catalog.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class CatalogSourceUi(
    val id: String,
    val name: String,
    val group: String?,
    val hasLogin: Boolean,
    val responseTimeMillis: Long,
)

@Stable
data class CatalogUiState(
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val query: String = "",
    val selectedGroup: String = "",
    val groups: ImmutableList<String> = persistentListOf(),
    val sources: ImmutableList<CatalogSourceUi> = persistentListOf(),
    val commandInFlight: Boolean = false,
)

sealed interface CatalogIntent {
    data object Retry : CatalogIntent
    data class Search(val value: String) : CatalogIntent
    data class SelectGroup(val value: String) : CatalogIntent
    data class OpenDiscovery(val sourceId: String) : CatalogIntent
    data class SearchSource(val sourceId: String) : CatalogIntent
    data class Login(val sourceId: String) : CatalogIntent
    data class Edit(val sourceId: String) : CatalogIntent
    data class Pin(val sourceId: String) : CatalogIntent
    data class Delete(val sourceId: String) : CatalogIntent
    data object OpenGlobalSearch : CatalogIntent
    data object OpenSourceManage : CatalogIntent
    data object OpenImport : CatalogIntent
}

sealed interface CatalogEffect {
    data class OpenDiscovery(val sourceId: String, val title: String) : CatalogEffect
    data class SearchSource(val sourceId: String, val name: String) : CatalogEffect
    data class Login(val sourceId: String) : CatalogEffect
    data class Edit(val sourceId: String) : CatalogEffect
    data object GlobalSearch : CatalogEffect
    data object SourceManage : CatalogEffect
    data object Import : CatalogEffect
    data class Message(val text: String) : CatalogEffect
}
