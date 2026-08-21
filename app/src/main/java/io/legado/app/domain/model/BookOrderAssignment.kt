package io.legado.app.domain.model

data class BookOrderAssignment(
    val bookUrl: String,
    val order: Int,
)

data class BookGroupOrderAssignment(
    val groupId: Long,
    val order: Int,
)
