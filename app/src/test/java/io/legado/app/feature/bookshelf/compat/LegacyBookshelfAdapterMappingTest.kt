package io.legado.app.feature.bookshelf.compat

import io.legado.app.data.model.BookshelfBookRecord
import io.legado.app.feature.bookshelf.api.BookshelfCommandResult
import io.legado.app.feature.bookshelf.api.BookshelfError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyBookshelfAdapterMappingTest {
    @Test
    fun `room projection maps reader progress without exposing the record`() {
        val summary = record(currentIndex = 4, chapterCount = 10).toFeatureSummary()

        assertEquals("book", summary.id)
        assertEquals(5, summary.unreadChapterCount)
        assertEquals(4f / 9f, summary.readingProgress)
    }

    @Test
    fun `delete exception after some SSOT removals becomes partial`() {
        val result = classifyDeleteFailure(
            requested = setOf("deleted", "remaining"),
            remaining = setOf("remaining"),
            error = BookshelfError.Retryable("locked"),
        )

        assertTrue(result is BookshelfCommandResult.Partial)
        result as BookshelfCommandResult.Partial
        assertEquals(setOf("deleted"), result.changedBookIds)
        assertEquals(setOf("remaining"), result.failed.keys)
    }

    @Test
    fun `delete exception before SSOT removal is a failure`() {
        val result = classifyDeleteFailure(
            requested = setOf("book"),
            remaining = setOf("book"),
            error = BookshelfError.PermissionDenied("denied"),
        )

        assertTrue(result is BookshelfCommandResult.Failure)
    }

    private fun record(currentIndex: Int, chapterCount: Int) = BookshelfBookRecord(
        bookUrl = "book",
        name = "Book",
        author = "Author",
        origin = "source",
        originName = "Source",
        coverUrl = null,
        customCoverUrl = null,
        durChapterTitle = "Chapter",
        durChapterTime = 1,
        durChapterPos = 0,
        latestChapterTitle = "Latest",
        latestChapterTime = 2,
        lastCheckCount = 0,
        totalChapterNum = chapterCount,
        durChapterIndex = currentIndex,
        type = 0,
        group = 0,
        order = 0,
    )
}
