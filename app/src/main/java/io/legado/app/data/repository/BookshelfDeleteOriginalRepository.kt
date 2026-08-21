package io.legado.app.data.repository

import io.legado.app.domain.gateway.BookshelfDeleteOriginalGateway
import io.legado.app.help.config.LocalConfig

class BookshelfDeleteOriginalRepository : BookshelfDeleteOriginalGateway {
    override val current: Boolean get() = LocalConfig.deleteBookOriginal

    override suspend fun update(deleteOriginal: Boolean) {
        LocalConfig.deleteBookOriginal = deleteOriginal
    }
}
