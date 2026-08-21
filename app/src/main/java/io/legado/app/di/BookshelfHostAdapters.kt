package io.legado.app.di

import io.legado.app.domain.gateway.BookSourceCallbackGateway
import io.legado.app.domain.gateway.BookshelfDeleteOriginalGateway
import io.legado.app.domain.gateway.BookshelfSettingsGateway
import io.legado.app.domain.gateway.LocalBookGateway
import io.legado.app.domain.model.settings.BookshelfSettings
import io.legado.app.feature.bookshelf.api.BookshelfPreferences
import io.legado.app.feature.bookshelf.impl.BookshelfBookRemovalHost
import io.legado.app.feature.bookshelf.impl.BookshelfPreferencesHost
import io.legado.app.feature.bookshelf.impl.BookshelfSortCodec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * App shell seams required by `:feature:bookshelf:impl`. They only forward to the existing owners
 * of the preference store and of the local-file / book-source side effects; the bookshelf business
 * rules live in the feature implementation.
 */
class AppBookshelfPreferencesHost(
    private val settings: BookshelfSettingsGateway,
    private val deleteOriginal: BookshelfDeleteOriginalGateway,
) : BookshelfPreferencesHost {

    override val current: BookshelfPreferences
        get() = settings.currentSettings.toFeaturePreferences(deleteOriginal.current)

    override val preferences: Flow<BookshelfPreferences> = settings.settings
        .map { it.toFeaturePreferences(deleteOriginal.current) }

    override suspend fun selectGroup(groupId: Long) {
        settings.update { it.copy(saveTabPosition = groupId) }
    }

    override suspend fun setDeleteOriginalDefault(deleteOriginal: Boolean) {
        this.deleteOriginal.update(deleteOriginal)
    }
}

class AppBookshelfBookRemovalHost(
    private val localBookGateway: LocalBookGateway,
    private val bookSourceCallbackGateway: BookSourceCallbackGateway,
) : BookshelfBookRemovalHost {

    override suspend fun onBookRemoved(
        bookUrl: String,
        isLocal: Boolean,
        deleteOriginal: Boolean,
    ) {
        if (isLocal) {
            localBookGateway.deleteBook(bookUrl, deleteOriginal)
        } else {
            bookSourceCallbackGateway.onDeleteFromShelf(bookUrl)
        }
    }
}

private fun BookshelfSettings.toFeaturePreferences(deleteOriginalDefault: Boolean) =
    BookshelfPreferences(
        selectedGroupId = saveTabPosition,
        defaultSort = BookshelfSortCodec.fromStored(bookshelfSort),
        descending = bookshelfSortOrder == 1,
        deleteOriginalDefault = deleteOriginalDefault,
    )
