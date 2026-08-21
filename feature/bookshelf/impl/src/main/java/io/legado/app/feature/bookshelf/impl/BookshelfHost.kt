package io.legado.app.feature.bookshelf.impl

import io.legado.app.feature.bookshelf.api.BookshelfPreferences
import kotlinx.coroutines.flow.Flow

/**
 * Preference seam owned by the app shell. The bookshelf implementation only needs the three
 * bookshelf-scoped values below, so it does not depend on the app-wide settings model.
 */
interface BookshelfPreferencesHost {
    val current: BookshelfPreferences
    val preferences: Flow<BookshelfPreferences>
    suspend fun selectGroup(groupId: Long)
    suspend fun setDeleteOriginalDefault(deleteOriginal: Boolean)
}

/**
 * File-system and book-source side effects of removing a book from the shelf. They stay with the
 * app shell because they need local storage and the rule engine, not the bookshelf SSOT.
 */
interface BookshelfBookRemovalHost {
    suspend fun onBookRemoved(bookUrl: String, isLocal: Boolean, deleteOriginal: Boolean)
}
