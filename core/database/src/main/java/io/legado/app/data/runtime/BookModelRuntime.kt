package io.legado.app.data.runtime

import io.legado.app.constant.AppPattern
import io.legado.app.constant.PageAnim
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import java.security.MessageDigest

/** App-owned behavior that must not make the Room entity depend on reader/rule implementations. */
interface BookModelRuntime {
    fun simulatedTotalChapterNum(book: Book): Int
    fun defaultPageAnimation(): Int
    fun folderName(book: Book): String
    fun migrate(
        source: Book,
        target: Book,
        toc: List<BookChapter>,
        defaultReplaceEnabled: Boolean,
        chineseConverterType: Int,
    ): Book
}

object BookModelRuntimeRegistry {
    @Volatile
    var runtime: BookModelRuntime = object : BookModelRuntime {
        override fun simulatedTotalChapterNum(book: Book): Int = book.totalChapterNum

        override fun defaultPageAnimation(): Int = PageAnim.coverPageAnim

        override fun folderName(book: Book): String {
            val safeName = book.name.replace(AppPattern.fileNameRegex, "").take(9)
            val md5 = MessageDigest.getInstance("MD5")
                .digest(book.bookUrl.toByteArray())
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
                .substring(8, 24)
            return safeName + md5
        }

        override fun migrate(
            source: Book,
            target: Book,
            toc: List<BookChapter>,
            defaultReplaceEnabled: Boolean,
            chineseConverterType: Int,
        ): Book = error("Book migration runtime is not installed")
    }
}
