package io.legado.app.ui.book.info

import androidx.compose.runtime.Stable

/**
 * 命中的书架标签。原本定义在已删除的 `BookInfoContract`，
 * 因为 `HighlightTagRow` 与 `Book.highlightedTags()` 都在用，随旧详情页删除时保留下来。
 */
@Stable
data class HighlightedTag(
    val matchedLabels: List<String>,
    val title: String?,
)

/** 阅读器回传「书已删除」的结果码。`ReadMangaActivity` 仍在用。 */
const val READER_RESULT_DELETED = 100
