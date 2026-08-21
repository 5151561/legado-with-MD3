package io.legado.app.feature.catalog.impl

import io.legado.app.data.entities.BookSourcePart
import io.legado.app.feature.catalog.api.CatalogCommandResult
import io.legado.app.feature.catalog.api.CatalogQueryState
import io.legado.app.feature.catalog.api.CatalogRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The catalog API contract, executed against the formal implementation. These cases previously ran
 * against the deleted `LegacyCatalogAdapter`; the expectations are unchanged.
 */
class CatalogImplContractTest {

    private val store = FakeCatalogSourceStore()
    private val removalHost = RecordingCatalogRemovalHost()
    private val catalog = DefaultCatalogRepository(store, removalHost)

    @Test
    fun `query emits loading before the first SSOT snapshot`() = runTest {
        store.sources = listOf(sourcePart("a"))

        val states = catalog.observeCatalog(CatalogRequest()).take(2).toList()

        assertEquals(CatalogQueryState.Loading, states.first())
        assertEquals(
            listOf("a"),
            (states[1] as CatalogQueryState.Data).snapshot.sources.map { it.id },
        )
    }

    @Test
    fun `an empty catalog is data, not a failure`() = runTest {
        val state = catalog.observeCatalog(CatalogRequest()).take(2).toList()[1]

        assertTrue(state is CatalogQueryState.Data)
        assertEquals(emptyList<String>(), (state as CatalogQueryState.Data).snapshot.sources)
    }

    @Test
    fun `a query error is reported as retryable`() = runTest {
        store.groupsFlow = flow { error("db closed") }

        val states = catalog.observeCatalog(CatalogRequest()).take(2).toList()

        assertEquals(CatalogQueryState.Failed(retryable = true), states[1])
    }

    @Test
    fun `explore is only enabled when the source both allows and declares it`() = runTest {
        store.sources = listOf(
            sourcePart("a", enabledExplore = true, hasExploreUrl = true),
            sourcePart("b", enabledExplore = true, hasExploreUrl = false),
            sourcePart("c", enabledExplore = false, hasExploreUrl = true),
        )

        val snapshot = (catalog.observeCatalog(CatalogRequest()).take(2).toList()[1]
            as CatalogQueryState.Data).snapshot

        assertEquals(
            mapOf("a" to true, "b" to false, "c" to false),
            snapshot.sources.associate { it.id to it.exploreEnabled },
        )
    }

    @Test
    fun `the group prefix keeps its existing search syntax`() = runTest {
        store.sources = listOf(sourcePart("a", group = "漫画"), sourcePart("b", group = "小说"))

        val snapshot = (catalog.observeCatalog(CatalogRequest(query = "group:漫画"))
            .take(2).toList()[1] as CatalogQueryState.Data).snapshot

        assertEquals(listOf("a"), snapshot.sources.map { it.id })
        assertEquals(listOf("group:漫画"), store.groupQueries)
    }

    @Test
    fun `pinning moves the source above the current minimum order`() = runTest {
        store.sources = listOf(sourcePart("a", order = 5), sourcePart("b", order = 2))

        assertEquals(CatalogCommandResult.Success, catalog.pinSource("a"))
        assertEquals(1, store.sources.first { it.bookSourceUrl == "a" }.customOrder)
    }

    @Test
    fun `deletion is delegated to the single removal owner`() = runTest {
        store.sources = listOf(sourcePart("a"))

        assertEquals(CatalogCommandResult.Success, catalog.deleteSource("a"))
        assertEquals(listOf("a"), removalHost.deleted)
    }

    @Test
    fun `commands on an unknown source fail without touching the SSOT`() = runTest {
        assertTrue(catalog.pinSource("ghost") is CatalogCommandResult.Failure)
        assertTrue(catalog.deleteSource("ghost") is CatalogCommandResult.Failure)
        assertEquals(emptyList<String>(), removalHost.deleted)
    }

    @Test
    fun `a failing write is reported as a command failure`() = runTest {
        store.sources = listOf(sourcePart("a"))
        store.failOnPin = true

        assertTrue(catalog.pinSource("a") is CatalogCommandResult.Failure)
    }
}

/**
 * `BookSourcePart` is a plain data class, but the fake still tracks a revision counter so it
 * matches Room's table-level invalidation rather than list equality.
 */
internal class FakeCatalogSourceStore : CatalogSourceStore {

    var sources: List<BookSourcePart> = emptyList()
        set(value) {
            field = value
            revision.value += 1
        }

    private val revision = MutableStateFlow(0)
    val groupQueries = mutableListOf<String>()
    var groupsFlow: Flow<List<String>> = MutableStateFlow(listOf("小说"))
    var failOnPin = false

    override fun observeExploreGroups(): Flow<List<String>> = groupsFlow

    override fun observeExploreSources(query: String, group: String): Flow<List<BookSourcePart>> {
        val selectedGroup = when {
            query.startsWith("group:") -> query.substringAfter("group:").also { groupQueries += query }
            query.isNotBlank() -> ""
            else -> group
        }
        return revision.map {
            sources.filter { source ->
                when {
                    selectedGroup.isNotEmpty() -> source.bookSourceGroup.orEmpty().contains(selectedGroup)
                    query.isNotBlank() -> source.bookSourceName.contains(query, ignoreCase = true)
                    else -> true
                }
            }
        }
    }

    override suspend fun getSource(sourceId: String): BookSourcePart? =
        sources.firstOrNull { it.bookSourceUrl == sourceId }

    override suspend fun pinSource(source: BookSourcePart) {
        if (failOnPin) error("boom")
        val minOrder = sources.minOfOrNull { it.customOrder } ?: 0
        sources = sources.map {
            if (it.bookSourceUrl == source.bookSourceUrl) it.copy(customOrder = minOrder - 1) else it
        }
    }
}

internal class RecordingCatalogRemovalHost : CatalogSourceRemovalHost {
    val deleted = mutableListOf<String>()
    override suspend fun deleteSource(sourceId: String) {
        deleted += sourceId
    }
}

internal fun sourcePart(
    id: String,
    name: String = id,
    group: String? = "小说",
    order: Int = 0,
    enabledExplore: Boolean = true,
    hasExploreUrl: Boolean = true,
    hasLoginUrl: Boolean = false,
    respondTime: Long = 180000L,
) = BookSourcePart(
    bookSourceUrl = id,
    bookSourceName = name,
    bookSourceGroup = group,
    customOrder = order,
    enabledExplore = enabledExplore,
    hasExploreUrl = hasExploreUrl,
    hasLoginUrl = hasLoginUrl,
    respondTime = respondTime,
)
