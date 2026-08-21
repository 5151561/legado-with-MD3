package io.legado.app.data.repository

import io.legado.app.data.dao.BookGroupDao
import io.legado.app.data.entities.BookGroup
import io.legado.app.domain.gateway.BookGroupOrderGateway
import io.legado.app.domain.model.BookGroupOrderAssignment
import kotlinx.coroutines.flow.Flow

class BookGroupRepository(private val bookGroupDao: BookGroupDao) : BookGroupOrderGateway {

    fun flowAll(): Flow<List<BookGroup>> {
        return bookGroupDao.flowAll()
    }

    fun flowSelect(): Flow<List<BookGroup>> {
        return bookGroupDao.flowSelect()
    }

    fun flowShow(): Flow<List<BookGroup>> {
        return bookGroupDao.flowShow()
    }

    suspend fun update(vararg bookGroup: BookGroup) {
        bookGroupDao.update(*bookGroup)
    }

    suspend fun insert(vararg bookGroup: BookGroup) {
        bookGroupDao.insert(*bookGroup)
    }

    suspend fun delete(vararg bookGroup: BookGroup) {
        bookGroupDao.delete(*bookGroup)
    }

    suspend fun getUnusedId(): Long {
        return bookGroupDao.getUnusedId()
    }

    fun getMaxOrder(): Int {
        return bookGroupDao.maxOrder
    }

    suspend fun getByID(id: Long): BookGroup? {
        return bookGroupDao.getByID(id)
    }

    suspend fun getIdsSum(): Long {
        return bookGroupDao.idsSum
    }

    suspend fun getGroupNames(id: Long): List<String> {
        return bookGroupDao.getGroupNames(id)
    }

    suspend fun clearCover(groupId: Long) {
        bookGroupDao.clearCover(groupId)
    }

    override suspend fun getGroupOrders(groupIds: Set<Long>): List<BookGroupOrderAssignment> =
        groupIds.mapNotNull { id ->
            bookGroupDao.getByID(id)?.let { BookGroupOrderAssignment(it.groupId, it.order) }
        }

    override suspend fun updateGroupOrders(assignments: List<BookGroupOrderAssignment>) {
        val byId = assignments.associateBy { it.groupId }
        val groups = byId.keys.mapNotNull { id ->
            bookGroupDao.getByID(id)?.let { group -> group.copy(order = byId.getValue(id).order) }
        }
        if (groups.isNotEmpty()) bookGroupDao.update(*groups.toTypedArray())
    }
}
