package io.legado.app.feature.rss.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class RssSourceUi(
    val id: String,
    val name: String,
    val icon: String?,
    val group: String?,
    val hasLogin: Boolean,
)

@Stable
data class RssUiState(
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val query: String = "",
    val selectedGroup: String = "",
    val groups: ImmutableList<String> = persistentListOf(),
    val sources: ImmutableList<RssSourceUi> = persistentListOf(),
    val commandInFlight: Boolean = false,
)

sealed interface RssIntent {
    data object Retry : RssIntent
    data class Search(val value: String) : RssIntent
    data class SelectGroup(val value: String) : RssIntent
    data class Open(val sourceId: String) : RssIntent
    data class Login(val sourceId: String) : RssIntent
    data class Edit(val sourceId: String) : RssIntent
    data class Pin(val sourceId: String) : RssIntent
    data class Disable(val sourceId: String) : RssIntent
    data class Delete(val sourceId: String) : RssIntent
    data object Favorites : RssIntent
    data object Manage : RssIntent
    data object RuleSubscriptions : RssIntent
}

sealed interface RssEffect {
    data class Open(val target: io.legado.app.feature.rss.api.RssOpenTarget) : RssEffect
    data class Login(val sourceId: String) : RssEffect
    data class Edit(val sourceId: String) : RssEffect
    data object Favorites : RssEffect
    data object Manage : RssEffect
    data object RuleSubscriptions : RssEffect
    data class Message(val text: String) : RssEffect
}
