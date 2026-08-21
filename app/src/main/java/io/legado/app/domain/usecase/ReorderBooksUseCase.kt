package io.legado.app.domain.usecase

import io.legado.app.domain.model.BookOrderAssignment
import io.legado.app.domain.repository.BookDomainRepository

/** Keeps the legacy order-column semantics out of the temporary feature adapter. */
class ReorderBooksUseCase(
    private val bookRepository: BookDomainRepository,
) {
    suspend fun execute(orderedBookIds: List<String>, descending: Boolean): Set<String> {
        if (orderedBookIds.isEmpty()) return emptySet()
        require(orderedBookIds.distinct().size == orderedBookIds.size) {
            "orderedBookIds contains duplicates"
        }

        val existing = bookRepository.getBookOrders(orderedBookIds.toSet()).associateBy { it.bookUrl }
        require(existing.size == orderedBookIds.size) { "One or more books were not found" }
        val maxOrder = orderedBookIds.size
        val assignments = orderedBookIds.mapIndexed { index, bookId ->
            BookOrderAssignment(
                bookUrl = bookId,
                order = if (descending) maxOrder - index else index + 1,
            )
        }
        bookRepository.updateBookOrders(assignments)
        return assignments.mapTo(linkedSetOf()) { it.bookUrl }
    }
}
