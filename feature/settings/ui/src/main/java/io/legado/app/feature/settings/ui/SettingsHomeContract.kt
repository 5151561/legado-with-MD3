package io.legado.app.feature.settings.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * 设置主页（重设计画板 C-01）的契约。
 *
 * 设计规则「设置路由重排」把原来九个平级入口改成三组，排序依据是改动频率加影响范围：
 * 越常改越靠上，越容易影响别处的越靠下。每天都要动的入口留在「我的」（画板 P-01），
 * 这里只放一次配置很久不动的项；**同一入口不在两处出现**。
 *
 * 与既有 [SettingsUiState] 并存：后者服务实验版 `SettingsScreen`，本契约服务重设计页面。
 */
enum class SettingsEntryId {
    ReadingAndLayout,
    ReadAloud,
    ShelfAndCover,
    BackupAndSync,
    WebService,
    AiAndTranslation,
    General,
}

@Immutable
data class SettingsEntryUi(
    val id: SettingsEntryId,
    val title: String,
    /** 当前值摘要，一行内显示，超出省略。 */
    val summary: String,
)

@Immutable
data class SettingsGroupUi(
    val label: String,
    /** 分组存在理由的短提示，如「改得最勤，放最上」。 */
    val hint: String,
    val entries: ImmutableList<SettingsEntryUi>,
)

@Stable
data class SettingsHomeUiState(
    val loading: Boolean = false,
    val groups: ImmutableList<SettingsGroupUi> = persistentListOf(),
)

sealed interface SettingsHomeIntent {
    data class OpenEntry(val id: SettingsEntryId) : SettingsHomeIntent
    data object OpenSearch : SettingsHomeIntent
    data object Back : SettingsHomeIntent
}
