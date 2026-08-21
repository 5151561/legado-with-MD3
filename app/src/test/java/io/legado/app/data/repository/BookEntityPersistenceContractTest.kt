package io.legado.app.data.repository

import io.legado.app.data.dao.BookChapterDao
import io.legado.app.data.dao.BookDao
import io.legado.app.data.entities.Book
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.lang.reflect.Proxy

class BookEntityPersistenceContractTest {

    @Test
    fun `new and existing books choose exactly one write path`() {
        val calls = mutableListOf<String>()
        var exists = false
        val persistence = persistence(calls) { exists }
        val book = Book(bookUrl = "book://one")

        persistence.save(book) { calls += "rules" }
        exists = true
        persistence.save(book) { calls += "rules" }

        assertEquals(listOf("rules", "has", "insert", "rules", "has", "update"), calls)
    }

    @Test
    fun `delete preserves chapter then book ordering`() {
        val calls = mutableListOf<String>()
        val persistence = persistence(calls) { true }

        persistence.delete(Book(bookUrl = "book://one"))

        assertEquals(listOf("deleteChapters", "deleteBook"), calls)
    }

    @Test
    fun `chapter deletion failure stops book deletion`() {
        val calls = mutableListOf<String>()
        val bookDao = proxy<BookDao> { method, _ ->
            if (method == "delete") calls += "deleteBook"
            defaultValue(method)
        }
        val chapterDao = proxy<BookChapterDao> { method, _ ->
            if (method == "delByBook") {
                calls += "deleteChapters"
                error("chapter delete failed")
            }
            defaultValue(method)
        }

        assertThrows(IllegalStateException::class.java) {
            BookEntityPersistence(bookDao, chapterDao).delete(Book(bookUrl = "book://one"))
        }
        assertEquals(listOf("deleteChapters"), calls)
    }

    private fun persistence(
        calls: MutableList<String>,
        exists: () -> Boolean,
    ): BookEntityPersistence {
        val bookDao = proxy<BookDao> { method, arguments ->
            when {
                method == "has" && arguments?.size == 1 -> {
                    calls += "has"
                    exists()
                }
                method == "insert" -> calls += "insert"
                method == "update" -> calls += "update"
                method == "delete" -> calls += "deleteBook"
                else -> defaultValue(method)
            }
        }
        val chapterDao = proxy<BookChapterDao> { method, _ ->
            if (method == "delByBook") calls += "deleteChapters"
            defaultValue(method)
        }
        return BookEntityPersistence(bookDao, chapterDao)
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T> proxy(
        crossinline handler: (String, Array<out Any?>?) -> Any?,
    ): T = Proxy.newProxyInstance(
        T::class.java.classLoader,
        arrayOf(T::class.java),
    ) { _, method, arguments -> handler(method.name, arguments) } as T

    private fun defaultValue(method: String): Any? = when (method) {
        "toString" -> "FakeDao"
        "hashCode" -> 0
        "equals" -> false
        else -> null
    }
}
