package io.legado.app.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.legado.app.feature.settings.api.AppThemeMode
import io.legado.app.feature.settings.api.ProfileCommands
import io.legado.app.feature.settings.api.ProfileQuery
import io.legado.app.feature.settings.api.ProfileSnapshot
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 「我的」（画板 P-01）。
 *
 * 本页只有两个就地写：主题模式与 Web 服务开关。其余七项一律是「去某处」，
 * 由 [ProfileEffect] 交给宿主导航——页面自己不认识任何路由。
 */
class ProfileViewModel(
    private val query: ProfileQuery,
    private val commands: ProfileCommands,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<ProfileEffect>(extraBufferCapacity = 16)
    val effects = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            query.observeProfile().collect { snapshot -> _uiState.value = snapshot.toUiState() }
        }
    }

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.SelectThemeMode -> viewModelScope.launch {
                commands.setThemeMode(intent.mode.toApi())
            }

            is ProfileIntent.SetToggle -> viewModelScope.launch {
                // 目前只有 Web 服务这一个行内开关，见 ProfileTrailing.Toggle 的说明。
                if (intent.id == ProfileEntryId.WebService) {
                    commands.setWebServiceEnabled(intent.checked)
                }
            }

            is ProfileIntent.OpenEntry -> _effects.tryEmit(ProfileEffect.OpenEntry(intent.id))
        }
    }
}

/** 去处由宿主决定。页面只说「用户点了哪一项」。 */
sealed interface ProfileEffect {
    data class OpenEntry(val id: ProfileEntryId) : ProfileEffect
}

private fun ProfileSnapshot.toUiState() = ProfileUiState(
    themeMode = themeMode.toUi(),
    themeEntry = ProfileEntryUi(
        id = ProfileEntryId.Theme,
        title = "主题",
        summary = SettingsFormat.join(
            accentName?.let { "强调色 $it" },
            "定时切换 ${SettingsFormat.onOff(scheduledThemeEnabled)}",
            "界面字号 ${SettingsFormat.fontScale(uiFontScalePercent)}",
        ),
    ),
    groups = persistentListOf(
        ProfileGroupUi(
            label = "内容",
            entries = persistentListOf(
                ProfileEntryUi(
                    id = ProfileEntryId.BookmarksAndNotes,
                    title = "书签与笔记",
                    summary = SettingsFormat.join(
                        "$bookmarkCount 条",
                        "$bookmarkBookCount 本书".takeIf { bookmarkBookCount > 0 },
                    ),
                ),
                ProfileEntryUi(
                    id = ProfileEntryId.ReadingRecords,
                    title = "阅读记录与统计",
                    summary = "本周 ${SettingsFormat.duration(weeklyReadMillis)}",
                ),
                ProfileEntryUi(
                    id = ProfileEntryId.AiChat,
                    title = "AI 对话",
                    summary = "$aiConversationCount 个会话",
                ),
            ),
        ),
        ProfileGroupUi(
            label = "源与服务",
            entries = persistentListOf(
                ProfileEntryUi(
                    id = ProfileEntryId.SourcesAndRules,
                    title = "源与规则",
                    summary = SettingsFormat.join(
                        "书源 $bookSourceCount",
                        "订阅源 $rssSourceCount",
                        "规则 $ruleCount",
                    ),
                ),
                ProfileEntryUi(
                    id = ProfileEntryId.WebService,
                    title = "Web 服务",
                    // 运行中用强调色：这是一个正在生效的运行态，不是静态说明。
                    summary = if (webService.running) {
                        SettingsFormat.join("运行中", webService.address)
                    } else {
                        "未开启"
                    },
                    summaryAccent = webService.running,
                    trailing = ProfileTrailing.Toggle(checked = webService.running),
                ),
            ),
        ),
        ProfileGroupUi(
            label = "系统",
            entries = persistentListOf(
                ProfileEntryUi(id = ProfileEntryId.Settings, title = "设置"),
                ProfileEntryUi(
                    id = ProfileEntryId.About,
                    title = "关于",
                    summary = SettingsFormat.join(
                        appVersionName.takeIf { it.isNotBlank() }?.let { "v$it" },
                        "退出应用在此",
                    ),
                    trailing = if (updateAvailable) {
                        ProfileTrailing.Badge("新版本")
                    } else {
                        ProfileTrailing.Chevron
                    },
                ),
            ),
        ),
    ),
)

private fun AppThemeMode.toUi() = when (this) {
    AppThemeMode.Light -> ProfileThemeMode.Light
    AppThemeMode.Dark -> ProfileThemeMode.Dark
    AppThemeMode.System -> ProfileThemeMode.System
}

private fun ProfileThemeMode.toApi() = when (this) {
    ProfileThemeMode.Light -> AppThemeMode.Light
    ProfileThemeMode.Dark -> AppThemeMode.Dark
    ProfileThemeMode.System -> AppThemeMode.System
}
