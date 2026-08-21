package io.legado.app.ui.main.bookshelf

import androidx.compose.runtime.Stable
import io.legado.app.data.entities.Book
import io.legado.app.data.model.BookshelfBookRecord
import io.legado.app.utils.splitNotBlank
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
typealias BookShelfItem = BookshelfBookRecord

fun BookshelfBookRecord.toUiItem(): BookUiItem {
    val tagList = mutableListOf<String>()
    customTag?.splitNotBlank(",", "\n")?.filter { it.isNotBlank() }?.let(tagList::addAll)
    kind?.splitNotBlank(",", "\n")?.filter { it.isNotBlank() }?.let {
        tagList.addAll(it.filterNot(tagList::contains))
    }
    val displayWordCount = wordCount
    if (!displayWordCount.isNullOrBlank() && !tagList.contains(displayWordCount)) {
        tagList.add(displayWordCount)
    }
    return BookUiItem(book = this, displayTags = tagList.toImmutableList())
}

/**
 * 理想实现：专为 UI 设计的状态类
 */
@Stable
data class BookUiItem(
    val book: BookShelfItem,
    val displayTags: ImmutableList<String>
) {
    fun matches(key: String): Boolean {
        return book.name.contains(key, true) ||
                book.author.contains(key, true) ||
                book.originName.contains(key, true) ||
                displayTags.any { it.contains(key, true) }
    }
}

fun BookShelfItem.toLightBook() = Book(
    bookUrl = bookUrl,
    origin = origin,
    originName = originName,
    name = name,
    author = author,
    coverUrl = coverUrl,
    customCoverUrl = customCoverUrl,
    latestChapterTitle = latestChapterTitle,
    latestChapterTime = latestChapterTime,
    lastCheckCount = lastCheckCount,
    totalChapterNum = totalChapterNum,
    durChapterTitle = durChapterTitle,
    durChapterIndex = durChapterIndex,
    durChapterPos = durChapterPos,
    durChapterTime = durChapterTime,
    type = type,
    group = group,
    order = order,
    canUpdate = canUpdate,
    wordCount = wordCount,
    kind = kind,
    customTag = customTag
)
