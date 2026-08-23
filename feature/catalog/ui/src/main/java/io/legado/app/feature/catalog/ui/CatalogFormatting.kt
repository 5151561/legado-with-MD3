package io.legado.app.feature.catalog.ui

import java.util.Locale

/**
 * 摘要文案的组装。
 *
 * api 只给数，句子在这一层拼——本地化字符串不该进业务契约。
 * 所有函数都遵守同一条规则：**取不到的数就不写那一句**，不用 0 或「未知」占位
 * （设计规则「状态优先于成功态」）。
 */
internal object CatalogFormat {

    /** 「12 KB」「18.2 MB」。小于 1 KB 一律按 KB 显示，不出现「512 B」这种精度。 */
    fun bytes(value: Long): String = when {
        value >= 1024L * 1024L -> String.format(Locale.getDefault(), "%.1f MB", value / 1024.0 / 1024.0)
        else -> "${(value / 1024.0).coerceAtLeast(1.0).toInt()} KB"
    }

    /** 「62%」。 */
    fun percent(progress: Float): String =
        "${(progress * 100).toInt().coerceIn(0, 100)}%"

    /** 用「 · 」连接非空片段。全空时返回空串。用于摘要这类并列信息。 */
    fun join(vararg parts: String?): String =
        parts.filter { !it.isNullOrBlank() }.joinToString(" · ")

    /**
     * 中文顿号列举：「A、B与C」。用于句子里的并列成分,而不是摘要。
     *
     * 用 [join] 拼句子会得到「A · B 会一并删除」这种中英混排的空格,读起来不像中文。
     */
    fun enumerate(vararg parts: String?): String {
        val items = parts.filterNot { it.isNullOrBlank() }.map { it!! }
        return when (items.size) {
            0 -> ""
            1 -> items.single()
            else -> items.dropLast(1).joinToString("、") + "与" + items.last()
        }
    }
}
