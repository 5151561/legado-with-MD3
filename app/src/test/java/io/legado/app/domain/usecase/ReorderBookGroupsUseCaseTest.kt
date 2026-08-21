package io.legado.app.domain.usecase

import io.legado.app.domain.gateway.BookGroupOrderGateway
import io.legado.app.domain.model.BookGroupOrderAssignment
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReorderBookGroupsUseCaseTest {
    @Test
    fun `writes a complete zero based group order`() = runTest {
        val gateway = RecordingGateway(setOf(1, 2, 4))
        ReorderBookGroupsUseCase(gateway).execute(listOf(4, 1, 2))

        assertEquals(
            listOf(
                BookGroupOrderAssignment(4, 0),
                BookGroupOrderAssignment(1, 1),
                BookGroupOrderAssignment(2, 2),
            ),
            gateway.updated,
        )
    }

    @Test
    fun `missing group fails without a partial write`() = runTest {
        val gateway = RecordingGateway(setOf(1))
        val failure = runCatching {
            ReorderBookGroupsUseCase(gateway).execute(listOf(1, 2))
        }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
        assertEquals(emptyList<BookGroupOrderAssignment>(), gateway.updated)
    }

    private class RecordingGateway(private val ids: Set<Long>) : BookGroupOrderGateway {
        var updated: List<BookGroupOrderAssignment> = emptyList()

        override suspend fun getGroupOrders(groupIds: Set<Long>) =
            groupIds.filter { it in ids }.map { BookGroupOrderAssignment(it, 0) }

        override suspend fun updateGroupOrders(assignments: List<BookGroupOrderAssignment>) {
            updated = assignments
        }
    }
}
