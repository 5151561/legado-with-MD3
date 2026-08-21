package io.legado.app.domain.gateway

import io.legado.app.domain.model.BookGroupOrderAssignment

interface BookGroupOrderGateway {
    suspend fun getGroupOrders(groupIds: Set<Long>): List<BookGroupOrderAssignment>
    suspend fun updateGroupOrders(assignments: List<BookGroupOrderAssignment>)
}
