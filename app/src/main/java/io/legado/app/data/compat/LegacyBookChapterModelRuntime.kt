package io.legado.app.data.compat

import io.legado.app.constant.AppLog
import io.legado.app.constant.AppPattern
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.data.runtime.BookChapterModelRuntime
import io.legado.app.exception.RegexTimeoutException
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.utils.ChineseUtils
import io.legado.app.utils.NetworkUtils
import io.legado.app.utils.replace
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.CancellationException
import splitties.init.appCtx

object LegacyBookChapterModelRuntime : BookChapterModelRuntime {
    @Volatile
    var replaceRuleTimeoutHandler: ((ReplaceRule) -> Unit)? = null

    override fun displayTitle(
        chapter: BookChapter,
        replaceRules: List<ReplaceRule>?,
        useReplace: Boolean,
        chineseConvert: Boolean,
        chineseConverterType: Int,
    ): String {
        var displayTitle = chapter.title.replace(AppPattern.rnRegex, "")
        if (chineseConvert) {
            when (chineseConverterType) {
                1 -> displayTitle = ChineseUtils.t2s(displayTitle)
                2 -> displayTitle = ChineseUtils.s2t(displayTitle)
            }
        }
        if (useReplace && replaceRules != null) run replacement@{
            replaceRules.forEach { item ->
                if (item.pattern.isEmpty()) return@forEach
                try {
                    val replaced = if (item.isRegex) {
                        displayTitle.replace(
                            item.regex,
                            item.replacement,
                            item.getValidTimeoutMillisecond(),
                        )
                    } else {
                        displayTitle.replace(item.pattern, item.replacement)
                    }
                    if (replaced.isNotBlank()) displayTitle = replaced
                } catch (_: RegexTimeoutException) {
                    item.isEnabled = false
                    replaceRuleTimeoutHandler?.invoke(item)
                } catch (_: CancellationException) {
                    return@replacement
                } catch (error: Exception) {
                    AppLog.put("${item.name}替换出错\n替换内容\n$displayTitle", error)
                    appCtx.toastOnUi("${item.name}替换出错")
                }
            }
        }
        return displayTitle
    }

    override fun absoluteUrl(chapter: BookChapter): String {
        if (chapter.url.startsWith(chapter.title) && chapter.isVolume) return chapter.baseUrl
        val match = AnalyzeUrl.paramPattern.find(chapter.url)
        val before = if (match == null) chapter.url else chapter.url.substring(0, match.range.first)
        val absolute = NetworkUtils.getAbsoluteURL(chapter.baseUrl, before)
        return if (match == null) absolute else "$absolute," + chapter.url.substring(match.range.last + 1)
    }
}
