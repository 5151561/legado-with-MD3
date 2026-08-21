package io.legado.app.feature.bookshelf.impl

import io.legado.app.data.model.BookshelfBookRecord
import io.legado.app.feature.bookshelf.api.BookshelfBookSummary
import io.legado.app.feature.bookshelf.api.BookshelfCommandResult
import io.legado.app.feature.bookshelf.api.BookshelfError
import io.legado.app.feature.bookshelf.api.BookshelfSort
import io.legado.app.utils.cnCompare
import java.io.IOException
import kotlin.math.max

internal fun BookshelfBookRecord.toFeatureSummary(): BookshelfBookSummary {
    val lastChapterIndex = (totalChapterNum - 1).coerceAtLeast(0)
    val progress = if (lastChapterIndex == 0) 0f else {
        (durChapterIndex.coerceIn(0, lastChapterIndex).toFloat() / lastChapterIndex).coerceIn(0f, 1f)
    }
    return BookshelfBookSummary(
        id = bookUrl,
        name = name,
        author = author,
        origin = origin,
        originName = originName,
        coverUrl = getDisplayCover(),
        currentChapterTitle = durChapterTitle,
        latestChapterTitle = latestChapterTitle,
        currentChapterIndex = durChapterIndex,
        totalChapterCount = totalChapterNum,
        unreadChapterCount = getUnreadChapterNum(),
        readingProgress = progress,
        lastReadAt = durChapterTime,
        latestChapterAt = latestChapterTime,
        groupMask = group,
        order = order,
        isLocal = isLocal,
        isAudio = isAudio,
        isImage = isImage,
    )
}

internal fun BookshelfBookRecord.matches(query: String): Boolean =
    name.contains(query, ignoreCase = true) ||
        author.contains(query, ignoreCase = true) ||
        originName.contains(query, ignoreCase = true) ||
        customTag.orEmpty().contains(query, ignoreCase = true) ||
        kind.orEmpty().contains(query, ignoreCase = true)

/**
 * Bookshelf ordering. The legacy BookshelfRepository took an optional per-group sort override, but
 * the bookshelf API has always passed `null` for it, so the dead override is not reproduced here.
 */
internal fun List<BookshelfBookRecord>.sortedForShelf(
    sort: BookshelfSort,
    descending: Boolean,
): List<BookshelfBookRecord> = when (sort) {
    BookshelfSort.LatestChapter ->
        if (descending) sortedByDescending { it.latestChapterTime }
        else sortedBy { it.latestChapterTime }

    BookshelfSort.BookName ->
        if (descending) sortedWith { o1, o2 -> o2.name.cnCompare(o1.name) }
        else sortedWith { o1, o2 -> o1.name.cnCompare(o2.name) }

    BookshelfSort.Manual ->
        if (descending) sortedByDescending { it.order } else sortedBy { it.order }

    BookshelfSort.LastActivity ->
        if (descending) sortedByDescending { max(it.latestChapterTime, it.durChapterTime) }
        else sortedBy { max(it.latestChapterTime, it.durChapterTime) }

    BookshelfSort.Author ->
        if (descending) sortedWith { o1, o2 -> o2.author.cnCompare(o1.author) }
        else sortedWith { o1, o2 -> o1.author.cnCompare(o2.author) }

    BookshelfSort.RecentReading ->
        if (descending) sortedByDescending { it.durChapterTime } else sortedBy { it.durChapterTime }
}

/**
 * The persisted bookshelf sort encoding. It stays in the implementation, and the app shell uses it
 * to translate the stored preference value instead of keeping a second copy of the mapping.
 */
object BookshelfSortCodec {
    fun fromStored(value: Int): BookshelfSort = when (value) {
        1 -> BookshelfSort.LatestChapter
        2 -> BookshelfSort.BookName
        3 -> BookshelfSort.Manual
        4 -> BookshelfSort.LastActivity
        5 -> BookshelfSort.Author
        else -> BookshelfSort.RecentReading
    }

    fun toStored(sort: BookshelfSort): Int = when (sort) {
        BookshelfSort.RecentReading -> 0
        BookshelfSort.LatestChapter -> 1
        BookshelfSort.BookName -> 2
        BookshelfSort.Manual -> 3
        BookshelfSort.LastActivity -> 4
        BookshelfSort.Author -> 5
    }
}

internal fun Throwable.toFeatureError(): BookshelfError = when (this) {
    is SecurityException -> BookshelfError.PermissionDenied(message)
    is IllegalArgumentException -> BookshelfError.InvalidRequest(message)
    is IOException -> BookshelfError.Retryable(message)
    else -> BookshelfError.Unexpected(message)
}

internal fun classifyDeleteFailure(
    requested: Set<String>,
    remaining: Set<String>,
    error: BookshelfError,
): BookshelfCommandResult {
    val changed = requested - remaining
    return when {
        changed.isEmpty() -> BookshelfCommandResult.Failure(error)
        remaining.isEmpty() -> BookshelfCommandResult.Success(changed)
        else -> BookshelfCommandResult.Partial(
            changedBookIds = changed,
            failed = remaining.associateWith { error },
        )
    }
}
