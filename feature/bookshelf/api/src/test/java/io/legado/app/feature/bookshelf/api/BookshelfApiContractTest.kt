package io.legado.app.feature.bookshelf.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookshelfApiContractTest {

    @Test
    fun `empty data is distinct from a query failure`() {
        val empty = BookshelfQueryState.Data(
            BookshelfSnapshot(BookshelfGroup.AllId, emptyList(), emptyList())
        )
        val failure = BookshelfQueryState.Failure(BookshelfError.Retryable("offline"))

        assertEquals(BookshelfQueryState.Data::class.java, empty.javaClass)
        assertEquals(BookshelfQueryState.Failure::class.java, failure.javaClass)
        assertFalse(empty == failure)
    }

    @Test
    fun `reading progress is a read only projection`() {
        val book = BookshelfBookSummary(
            id = "book",
            name = "Book",
            author = "Author",
            origin = "source",
            originName = "Source",
            coverUrl = null,
            currentChapterTitle = null,
            latestChapterTitle = null,
            currentChapterIndex = 4,
            totalChapterCount = 10,
            unreadChapterCount = 5,
            readingProgress = 0.5f,
            lastReadAt = 0,
            latestChapterAt = 0,
            groupMask = 0,
            order = 0,
            isLocal = false,
            isAudio = false,
            isImage = false,
        )

        assertEquals(0.5f, book.readingProgress)
        assertTrue(BookshelfCommands::class.java.methods.none { it.name.contains("progress", true) })
    }
}
