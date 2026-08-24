package io.legado.app.ui.widget.components.card

/**
 * 封面共享元素的键。
 *
 * 原先住在旧外壳的 `ui/main/` 下，但它与外壳无关——任何两个显示同一本书封面的页面
 * 都要用同一个键。随旧外壳删除时迁到这里。
 */
fun bookCoverSharedElementKey(bookUrl: String, sourceId: String? = null): String {
    val source = sourceId?.takeIf { it.isNotBlank() } ?: return "book-cover:$bookUrl"
    return "book-cover:$source:$bookUrl"
}
