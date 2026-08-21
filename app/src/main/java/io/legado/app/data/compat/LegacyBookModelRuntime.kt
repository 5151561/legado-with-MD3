package io.legado.app.data.compat

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.runtime.BookModelRuntime
import io.legado.app.help.book.BookHelp
import io.legado.app.help.book.ContentProcessor
import io.legado.app.help.book.getFolderNameNoCache
import io.legado.app.help.book.simulatedTotalChapterNum
import io.legado.app.help.config.ReadBookConfig

/** Legacy behavior behind the core database model contract; no runtime implementation leaks to core. */
object LegacyBookModelRuntime : BookModelRuntime {
    override fun simulatedTotalChapterNum(book: Book): Int = book.simulatedTotalChapterNum()

    override fun defaultPageAnimation(): Int = ReadBookConfig.pageAnim

    override fun folderName(book: Book): String = book.getFolderNameNoCache()

    override fun migrate(
        source: Book,
        target: Book,
        toc: List<BookChapter>,
        defaultReplaceEnabled: Boolean,
        chineseConverterType: Int,
    ): Book = target.apply {
        durChapterIndex = BookHelp.getDurChapter(
            source.durChapterIndex,
            source.durChapterTitle,
            toc,
            source.totalChapterNum,
        )
        durChapterTitle = toc[durChapterIndex].getDisplayTitle(
            ContentProcessor.get(name, origin).getTitleReplaceRules(),
            source.getUseReplaceRule(defaultReplaceEnabled),
            chineseConverterType = chineseConverterType,
        )
        durChapterPos = source.durChapterPos
        durChapterTime = source.durChapterTime
        group = source.group
        order = source.order
        customCoverUrl = source.customCoverUrl
        customIntro = source.customIntro
        customTag = source.customTag
        canUpdate = source.canUpdate
        if (source.config.fixedType) type = source.type
        readConfig = source.readConfig
        if (wordCount.isNullOrBlank()) wordCount = source.wordCount
    }
}
