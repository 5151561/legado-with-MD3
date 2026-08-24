package io.legado.app.feature.settings.api

/**
 * 「我的」（重设计画板 P-01）与设置主页（画板 C-01）的数据模型。
 *
 * 两块画板读的是同一批设置与计数，差别只在各自折叠多少信息，因此共用本模型。
 *
 * **摘要文案不进 api**：这里只给组成摘要的数与枚举，「书源 312 · 订阅源 8 · 规则 46」
 * 这类句子由 UI 拼。可空一律表示「该口径不适用或无信号」，不表示 0。
 */

/** 应用主题模式。画板 N-04 把主题收敛为三选一，个性化交给强调色种子。 */
enum class AppThemeMode { Light, Dark, System }

/**
 * Web 服务状态。
 *
 * [address] 只在 [running] 为真时有值——没跑起来时地址不是空串而是「不适用」。
 */
data class WebServiceStatus(
    val running: Boolean,
    val address: String? = null,
    val port: Int = 0,
)

/**
 * 「我的」的一次投影。
 *
 * @param accentName 强调色种子的名字，跟随系统动态取色时为 null。
 * @param uiFontScalePercent 界面字号，100 为标准。
 * @param weeklyReadMillis 本周（近七天）阅读时长。
 * @param ruleCount 替换 / txt 目录 / 字典 / 正文高亮 / 标签高亮五类规则之和，
 *   与源与规则枢纽（画板 D-00）里「规则」那一档同口径。
 */
data class ProfileSnapshot(
    val themeMode: AppThemeMode = AppThemeMode.System,
    val accentName: String? = null,
    val scheduledThemeEnabled: Boolean = false,
    val uiFontScalePercent: Int = 100,
    val bookmarkCount: Int = 0,
    val bookmarkBookCount: Int = 0,
    val weeklyReadMillis: Long = 0L,
    val aiConversationCount: Int = 0,
    val bookSourceCount: Int = 0,
    val rssSourceCount: Int = 0,
    val ruleCount: Int = 0,
    val webService: WebServiceStatus = WebServiceStatus(running = false),
    val appVersionName: String = "",
    val updateAvailable: Boolean = false,
)

/** 命令结果。失败带一句可直接展示的原因；成功不带任何文案。 */
sealed interface SettingsCommandResult {
    data object Success : SettingsCommandResult
    data class Failure(val message: String? = null) : SettingsCommandResult
}
