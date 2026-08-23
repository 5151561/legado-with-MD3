package io.legado.app.feature.home.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * 首页（重设计画板 M-01 v2）的契约。
 *
 * 这一版解决两条结构性病灶：
 *
 * - **P3**：首页不再持有第二套备份操作面。备份区块降级为「状态 + 一个去处」，
 *   没有备份 / 恢复 / 配置按钮——操作面唯一在画板 K-01。因此这里只有
 *   [BackupReminderUi] 与一条 [HomeIntent.OpenBackup]，契约里不存在「立即备份」这类命令。
 * - **N8**：首页不持有平行的内容浏览系统。精选区块的每一本书与「更多」都通往
 *   发现分类页（画板 S-03），所以 [FeaturedBookUi] 不带书籍详情所需的字段，
 *   点击一律走 [HomeIntent.OpenFeatured]。
 *
 * 区块的显示与顺序由画板 M-01a 的「首页区块设置」决定，见 [HomeSectionsUiState]。
 * 本状态只表达「这一次要渲染什么」，不表达用户的偏好本身。
 */

/** 继续阅读卡。首页恒在第一位，不可隐藏（画板 M-01a）。 */
@Immutable
data class ContinueReadingUi(
    val bookId: String,
    val bookName: String,
    val chapterTitle: String,
    /** 0f..1f。 */
    val progress: Float,
    val progressLabel: String,
)

/** 阅读目标与统计。柱状图是最近七天的相对时长，末位为今天。 */
@Immutable
data class ReadingGoalUi(
    val todayLabel: String,
    /** 七个 0f..1f 的相对值，最后一个是今天，用强调色绘制。 */
    val weekBars: ImmutableList<Float>,
    val summary: String,
)

/**
 * 备份状态提醒。
 *
 * 只在超期或失败时出现（画板 M-01a 的开关说明），因此它在 [HomeDashboardUiState]
 * 里是可空的——不出现即为一切正常，不需要一个「已备份」的成功态卡片。
 */
@Immutable
data class BackupReminderUi(
    val title: String,
    val detail: String,
    /** 唯一去处的按钮文案，动作固定为 [HomeIntent.OpenBackup]。 */
    val actionLabel: String,
)

/** 精选发现模块里的一本书。[loading] 为占位格，尚未取到数据。 */
@Immutable
data class FeaturedBookUi(
    val id: String,
    val title: String,
    val loading: Boolean = false,
)

@Immutable
data class FeaturedSectionUi(
    /** 形如「精选 · 墨韵书屋」，来源名由模块管理决定（画板 M-02a）。 */
    val title: String,
    val books: ImmutableList<FeaturedBookUi>,
    /** 说明这一区块的去处，写在区块末尾。 */
    val footnote: String,
)

@Stable
data class HomeDashboardUiState(
    val loading: Boolean = false,
    /** 顶栏标题，稿面为「今天」。 */
    val title: String = "今天",
    val continueReading: ContinueReadingUi? = null,
    val readingGoal: ReadingGoalUi? = null,
    /** null 表示备份正常，不显示提醒（见 [BackupReminderUi]）。 */
    val backupReminder: BackupReminderUi? = null,
    val featured: FeaturedSectionUi? = null,
)

sealed interface HomeIntent {
    /** 继续读上次那本。 */
    data object ContinueReading : HomeIntent
    /** 深链阅读统计（画板 T-01）。 */
    data object OpenReadingStats : HomeIntent
    /** 备份区块的唯一去处（画板 K-01）。 */
    data object OpenBackup : HomeIntent
    /** 精选区块的任一本书或「更多」，一律进发现分类页（画板 S-03）。 */
    data class OpenFeatured(val bookId: String?) : HomeIntent
    /** 顶栏唯一操作：首页区块设置（画板 M-01a）。 */
    data object OpenSectionSettings : HomeIntent
}

/** 画板 M-01 v2 的原始数据，用于预览与对稿。 */
val HomeDashboardPreviewState = HomeDashboardUiState(
    continueReading = ContinueReadingUi(
        bookId = "xuelochangan",
        bookName = "雪落长安",
        chapterTitle = "第四十七章 城南旧雪",
        progress = 0.62f,
        progressLabel = "62%",
    ),
    readingGoal = ReadingGoalUi(
        todayLabel = "今日 42 / 60 分钟",
        weekBars = persistentListOf(0.38f, 0.64f, 0.22f, 0.80f, 0.52f, 0.46f, 0.70f),
        summary = "连续 6 天达标 · 本周 4 小时 12 分",
    ),
    backupReminder = BackupReminderUi(
        title = "已 12 天未备份",
        detail = "上次 08-10 · WebDAV",
        actionLabel = "去备份",
    ),
    featured = FeaturedSectionUi(
        title = "精选 · 墨韵书屋",
        books = persistentListOf(
            FeaturedBookUi("1", "海边的旧书店"),
            FeaturedBookUi("2", "北山纪事"),
            FeaturedBookUi("3", "夜航船"),
            FeaturedBookUi("loading", "加载中", loading = true),
        ),
        footnote = "点击任一本书或「更多」都进发现分类页 S-03——首页不持有平行浏览系统（解 N8）",
    ),
)
