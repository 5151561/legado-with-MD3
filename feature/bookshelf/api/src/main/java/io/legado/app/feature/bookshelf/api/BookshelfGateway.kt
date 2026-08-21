package io.legado.app.feature.bookshelf.api

import kotlinx.coroutines.flow.Flow

interface BookshelfQuery {
    fun observeBookshelf(request: BookshelfQueryRequest): Flow<BookshelfQueryState>
}

interface BookshelfPreferencesGateway {
    val current: BookshelfPreferences
    val preferences: Flow<BookshelfPreferences>
    suspend fun selectGroup(groupId: Long)
}

interface BookshelfCommands {
    suspend fun moveBooks(bookIds: Set<String>, groupId: Long): BookshelfCommandResult

    suspend fun reorderBooks(
        groupId: Long,
        orderedBookIds: List<String>,
        descending: Boolean,
    ): BookshelfCommandResult

    suspend fun deleteBooks(
        bookIds: Set<String>,
        deleteOriginal: Boolean,
    ): BookshelfCommandResult
}

interface BookshelfGroupCommands {
    suspend fun createGroup(draft: BookshelfGroupDraft): BookshelfCommandResult
    suspend fun updateGroup(group: BookshelfGroup): BookshelfCommandResult
    suspend fun deleteGroup(groupId: Long): BookshelfCommandResult
    suspend fun reorderGroups(orderedGroupIds: List<Long>): BookshelfCommandResult
}
