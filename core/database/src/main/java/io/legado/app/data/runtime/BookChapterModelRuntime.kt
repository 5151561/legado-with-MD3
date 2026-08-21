package io.legado.app.data.runtime

import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.ReplaceRule

interface BookChapterModelRuntime {
    fun displayTitle(
        chapter: BookChapter,
        replaceRules: List<ReplaceRule>?,
        useReplace: Boolean,
        chineseConvert: Boolean,
        chineseConverterType: Int,
    ): String

    fun absoluteUrl(chapter: BookChapter): String
}

object BookChapterModelRuntimeRegistry {
    @Volatile
    lateinit var runtime: BookChapterModelRuntime
}
