package io.legado.app.domain.usecase

import io.legado.app.domain.gateway.BookGroupOrderGateway
import io.legado.app.domain.model.BookGroupOrderAssignment

class ReorderBookGroupsUseCase(
    private val bookGroupRepository: BookGroupOrderGateway,
) {
    suspend fun execute(orderedGroupIds: List<Long>): Set<Long> {
        if (orderedGroupIds.isEmpty()) return emptySet()
        require(orderedGroupIds.distinct().size == orderedGroupIds.size) {
            "orderedGroupIds contains duplicates"
        }

        val existing = bookGroupRepository.getGroupOrders(orderedGroupIds.toSet())
        require(existing.size == orderedGroupIds.size) { "One or more book groups were not found" }
        val assignments = orderedGroupIds.mapIndexed { index, groupId ->
            BookGroupOrderAssignment(groupId = groupId, order = index)
        }
        bookGroupRepository.updateGroupOrders(assignments)
        return assignments.mapTo(linkedSetOf()) { it.groupId }
    }
}
