package io.legado.app.domain.gateway

interface BookshelfDeleteOriginalGateway {
    val current: Boolean
    suspend fun update(deleteOriginal: Boolean)
}
