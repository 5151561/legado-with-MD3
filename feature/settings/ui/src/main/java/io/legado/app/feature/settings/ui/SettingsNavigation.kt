package io.legado.app.feature.settings.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.legado.app.core.navigation.TopLevelRoute
import kotlinx.serialization.Serializable

/**
 * settings 的导航目的地与它们的装配。理由见 `catalogEntries`：
 * 目的地清单归 feature 自己，外壳只负责把它们装进同一个 `entryProvider`。
 */

/** 「我的」（画板 P-01）。一级导航目的地。 */
@Serializable
data object ProfileRoute : TopLevelRoute {
    override val id: String get() = "profile"
}

/**
 * @param onNotRebuilt 见 `catalogEntries` 的同名参数。
 */
fun EntryProviderScope<NavKey>.settingsEntries(
    onNotRebuilt: (String) -> Unit,
    onOpenSourceHub: () -> Unit,
) {
    entry<ProfileRoute> {
        ProfileRouteScreen(
            onOpenEntry = { id ->
                when (id) {
                    ProfileEntryId.Settings -> onNotRebuilt("设置")
                    ProfileEntryId.SourcesAndRules -> onOpenSourceHub()
                    ProfileEntryId.Theme -> onNotRebuilt("主题")
                    ProfileEntryId.BookmarksAndNotes -> onNotRebuilt("书签与笔记")
                    ProfileEntryId.ReadingRecords -> onNotRebuilt("阅读记录与统计")
                    ProfileEntryId.AiChat -> onNotRebuilt("AI 对话")
                    ProfileEntryId.About -> onNotRebuilt("关于")
                    // Web 服务在页面上就地开关，不会走到这里。
                    ProfileEntryId.WebService -> Unit
                }
            },
        )
    }
}
