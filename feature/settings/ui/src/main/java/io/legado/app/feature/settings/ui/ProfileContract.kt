package io.legado.app.feature.settings.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * 「我的」（重设计画板 P-01 v2）的契约。
 *
 * 设计规则「设置路由重排」在这里的一半：**每天要动的放「我的」**，一次配置很久不动的
 * 放设置主页（画板 C-01，见 [SettingsHomeUiState]），同一入口不在两处出现。
 *
 * 旧「我的」14 项收敛成四组八项（解 N1）：
 *
 * | 去向 | 项 |
 * |---|---|
 * | 留在本页 | 主题、书签与笔记、阅读记录与统计、AI 对话、源与规则、Web 服务、设置、关于 |
 * | 合并进源与规则枢纽（画板 D-00） | 原先五个平级的规则页 |
 * | 下沉进设置 | 缓存管理、文件管理 |
 * | 移入关于（画板 P-06） | 退出应用 |
 *
 * 主题模式收敛为日光 / 夜墨 / 跟随三选一，个性化由强调色种子表达（画板 N-04）。
 */
enum class ProfileThemeMode { Light, Dark, System }

enum class ProfileEntryId {
    Theme,
    BookmarksAndNotes,
    ReadingRecords,
    AiChat,
    SourcesAndRules,
    WebService,
    Settings,
    About,
}

/** 行尾控件。三种就够——本页没有第四种尾随形态。 */
@Immutable
sealed interface ProfileTrailing {
    /** 进入下一页。 */
    data object Chevron : ProfileTrailing

    /** 就地开关，目前只有 Web 服务用。 */
    data class Toggle(val checked: Boolean) : ProfileTrailing

    /** 提示角标，如「新版本」。 */
    data class Badge(val label: String) : ProfileTrailing
}

@Immutable
data class ProfileEntryUi(
    val id: ProfileEntryId,
    val title: String,
    /** 当前值摘要；null 表示这一项没有可摘要的状态（如「设置」）。 */
    val summary: String? = null,
    /** 摘要用强调色——表示这是一个正在生效的运行态，而不是静态说明。 */
    val summaryAccent: Boolean = false,
    val trailing: ProfileTrailing = ProfileTrailing.Chevron,
)

@Immutable
data class ProfileGroupUi(
    val label: String,
    val entries: ImmutableList<ProfileEntryUi>,
)

@Stable
data class ProfileUiState(
    val themeMode: ProfileThemeMode = ProfileThemeMode.System,
    /** 主题行。它在分组标题之上，与三选一的模式条同属「外观」，因此单列。 */
    val themeEntry: ProfileEntryUi = ProfileEntryUi(ProfileEntryId.Theme, "主题"),
    val groups: ImmutableList<ProfileGroupUi> = persistentListOf(),
)

sealed interface ProfileIntent {
    data class SelectThemeMode(val mode: ProfileThemeMode) : ProfileIntent
    data class OpenEntry(val id: ProfileEntryId) : ProfileIntent
    /** 行内开关。目前只有 [ProfileEntryId.WebService] 会发出。 */
    data class SetToggle(val id: ProfileEntryId, val checked: Boolean) : ProfileIntent
}

/** 画板 P-01 v2 的原始数据，用于预览与对稿。 */
val ProfilePreviewState = ProfileUiState(
    themeMode = ProfileThemeMode.Light,
    themeEntry = ProfileEntryUi(
        ProfileEntryId.Theme,
        "主题",
        "强调色 石墨青 · 定时切换 关 · 界面字号 标准",
    ),
    groups = persistentListOf(
        ProfileGroupUi(
            label = "内容",
            entries = persistentListOf(
                ProfileEntryUi(ProfileEntryId.BookmarksAndNotes, "书签与笔记", "128 条 · 9 本书"),
                ProfileEntryUi(ProfileEntryId.ReadingRecords, "阅读记录与统计", "本周 4 小时 12 分"),
                ProfileEntryUi(ProfileEntryId.AiChat, "AI 对话", "3 个会话"),
            ),
        ),
        ProfileGroupUi(
            label = "源与服务",
            entries = persistentListOf(
                ProfileEntryUi(ProfileEntryId.SourcesAndRules, "源与规则", "书源 312 · 订阅源 8 · 规则 46"),
                ProfileEntryUi(
                    ProfileEntryId.WebService,
                    "Web 服务",
                    "运行中 · 192.168.1.7:1122",
                    summaryAccent = true,
                    trailing = ProfileTrailing.Toggle(checked = true),
                ),
            ),
        ),
        ProfileGroupUi(
            label = "系统",
            entries = persistentListOf(
                ProfileEntryUi(ProfileEntryId.Settings, "设置"),
                ProfileEntryUi(
                    ProfileEntryId.About,
                    "关于",
                    "v3.26 · 退出应用在此",
                    trailing = ProfileTrailing.Badge("新版本"),
                ),
            ),
        ),
    ),
)
