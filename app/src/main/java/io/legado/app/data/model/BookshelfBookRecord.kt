package io.legado.app.data.model

import io.legado.app.constant.BookType
import kotlin.math.max

/** Room query projection shared by legacy app code and the Phase 2 compatibility adapter. */
data class BookshelfBookRecord(
    val bookUrl: String,
    val name: String,
    val author: String,
    val origin: String,
    val originName: String,
    val coverUrl: String?,
    val customCoverUrl: String?,
    val durChapterTitle: String?,
    val durChapterTime: Long,
    val durChapterPos: Int,
    val latestChapterTitle: String?,
    val latestChapterTime: Long,
    val lastCheckCount: Int,
    val totalChapterNum: Int,
    val durChapterIndex: Int,
    val type: Int,
    val group: Long,
    val order: Int,
    val canUpdate: Boolean = true,
    val intro: String? = null,
    val kind: String? = null,
    val customTag: String? = null,
    val wordCount: String? = null,
) {
    fun getDisplayCover() = if (customCoverUrl.isNullOrEmpty()) coverUrl else customCoverUrl

    val isLocal: Boolean get() = (type and BookType.local) > 0
    val isAudio: Boolean get() = (type and BookType.audio) > 0
    val isImage: Boolean get() = (type and BookType.image) > 0
    val isNotShelf: Boolean get() = (type and BookType.notShelf) > 0
    val isNew: Boolean get() = lastCheckCount > 0

    fun getUnreadChapterNum() = max(totalChapterNum - durChapterIndex - 1, 0)
}
