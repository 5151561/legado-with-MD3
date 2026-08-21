package io.legado.app.data.runtime

/**
 * Explicit bridge for oversized rule variables that are stored outside Room rows.
 * The database model depends only on this contract; the app installs the legacy storage backend.
 */
interface EntityVariableStore {
    fun putBook(bookUrl: String, key: String, value: String?)
    fun getBook(bookUrl: String, key: String): String?
    fun putChapter(bookUrl: String, chapterUrl: String, key: String, value: String?)
    fun getChapter(bookUrl: String, chapterUrl: String, key: String): String?
    fun putRss(origin: String, link: String, key: String, value: String?)
    fun getRss(origin: String, link: String, key: String): String?
}

object EntityVariableRuntime {
    @Volatile
    var store: EntityVariableStore = object : EntityVariableStore {
        override fun putBook(bookUrl: String, key: String, value: String?) = Unit
        override fun getBook(bookUrl: String, key: String): String? = null
        override fun putChapter(bookUrl: String, chapterUrl: String, key: String, value: String?) = Unit
        override fun getChapter(bookUrl: String, chapterUrl: String, key: String): String? = null
        override fun putRss(origin: String, link: String, key: String, value: String?) = Unit
        override fun getRss(origin: String, link: String, key: String): String? = null
    }
}
