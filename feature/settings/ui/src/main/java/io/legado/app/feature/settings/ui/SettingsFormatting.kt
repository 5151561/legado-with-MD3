package io.legado.app.feature.settings.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 摘要文案的组装。
 *
 * api 只给数，句子在这一层拼——本地化字符串不该进业务契约。手法与
 * `catalog:ui` 的 `CatalogFormat` 一致：**取不到的数就不写那一句**，
 * 不用 0 或「未知」占位（设计规则「状态优先于成功态」）。
 */
internal object SettingsFormat {

    /** 用「 · 」连接非空片段。全空时返回空串。 */
    fun join(vararg parts: String?): String =
        parts.filter { !it.isNullOrBlank() }.joinToString(" · ")

    /** 「4 小时 12 分」。不足一分钟按「不足 1 分」，不显示秒。 */
    fun duration(millis: Long): String {
        val totalMinutes = millis / 60_000L
        if (totalMinutes <= 0L) return "不足 1 分"
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours <= 0L -> "$minutes 分"
            minutes <= 0L -> "$hours 小时"
            else -> "$hours 小时 $minutes 分"
        }
    }

    /** 「22:00」。 */
    fun timeOfDay(minuteOfDay: Int): String {
        val hour = minuteOfDay / 60
        val minute = minuteOfDay % 60
        return "%02d:%02d".format(hour, minute)
    }

    /** 界面字号：100 是稿面上的「标准」，其余直接写百分比。 */
    fun fontScale(percent: Int): String = if (percent == 100) "标准" else "$percent%"

    fun onOff(enabled: Boolean): String = if (enabled) "开" else "关"

    /**
     * 备份时刻。今天写「今天 08:12」，其余写「08-10 08:12」——
     * 年份不写：备份摘要看的是「多久没备份」，跨年的情况由「已 N 天未备份」那一句表达。
     */
    fun dateTime(millis: Long): String {
        val moment = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        val today = LocalDate.now(ZoneId.systemDefault())
        val time = "%02d:%02d".format(moment.hour, moment.minute)
        return if (moment.toLocalDate() == today) {
            "今天 $time"
        } else {
            "%02d-%02d %s".format(moment.monthValue, moment.dayOfMonth, time)
        }
    }
}
