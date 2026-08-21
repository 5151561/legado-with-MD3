package io.legado.app.data.compat

import io.legado.app.data.entities.Book

/**
 * Synchronous compatibility path for the legacy LocalBook parser, whose API is not suspendable.
 * New UI/feature code must use BookRepository. This bridge is included in the declining baseline.
 */
object LegacyBookPersistence {
    @Volatile
    var saveHandler: ((Book) -> Unit)? = null

    fun save(book: Book) {
        checkNotNull(saveHandler) { "LegacyBookPersistence has not been assembled" }(book)
    }
}
