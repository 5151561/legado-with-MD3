package io.legado.app.feature.rss.impl

import io.legado.app.feature.rss.api.RssCommandResult
import io.legado.app.feature.rss.api.RssOpenTarget
import io.legado.app.feature.rss.api.RssQueryState
import io.legado.app.feature.rss.api.RssRequest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The RSS API contract, executed against the formal implementation. These cases previously ran
 * against the deleted `LegacyRssAdapter`; the expectations are unchanged.
 */
class RssImplContractTest {

    private val store = FakeRssSourceStore()
    private val scriptHost = RecordingScriptHost()
    private val removalHost = RecordingRemovalHost()
    private val rss = DefaultRssRepository(store, scriptHost, removalHost)

    @Test
    fun `query emits loading before the first SSOT snapshot`() = runTest {
        store.sources = listOf(rssSource("a", icon = "icon"))

        val states = rss.observeSources(RssRequest()).take(2).toList()

        assertEquals(RssQueryState.Loading, states.first())
        val snapshot = (states[1] as RssQueryState.Data).snapshot
        assertEquals(listOf("a"), snapshot.sources.map { it.id })
        assertEquals("icon", snapshot.sources.single().icon)
    }

    @Test
    fun `an empty shelf of sources is data, not a failure`() = runTest {
        val state = rss.observeSources(RssRequest()).take(2).toList()[1]

        assertTrue(state is RssQueryState.Data)
        assertEquals(emptyList<String>(), (state as RssQueryState.Data).snapshot.sources)
    }

    @Test
    fun `a query error is reported as retryable`() = runTest {
        store.groupsFlow = flow { error("db closed") }

        val states = rss.observeSources(RssRequest()).take(2).toList()

        assertEquals(RssQueryState.Failed(retryable = true), states[1])
    }

    @Test
    fun `a blank icon is projected as absent`() = runTest {
        store.sources = listOf(rssSource("a", icon = ""))

        val snapshot = (rss.observeSources(RssRequest()).take(2).toList()[1] as RssQueryState.Data)
            .snapshot

        assertEquals(null, snapshot.sources.single().icon)
    }

    @Test
    fun `a multi page source without start html opens the sort list`() = runTest {
        store.sources = listOf(rssSource("a"))

        assertEquals(RssOpenTarget.Sort("a"), rss.resolveOpenTarget("a").getOrThrow())
    }

    @Test
    fun `a multi page source with start html opens the reader at the start page`() = runTest {
        store.sources = listOf(rssSource("a", startHtml = "<html/>"))

        val target = rss.resolveOpenTarget("a").getOrThrow() as RssOpenTarget.Read

        assertEquals("a", target.origin)
        assertTrue(target.startPage)
    }

    @Test
    fun `a single url source resolves its sort url before opening`() = runTest {
        store.sources = listOf(rssSource("a", singleUrl = true, sortUrl = "全部::https://x/feed"))

        val target = rss.resolveOpenTarget("a").getOrThrow() as RssOpenTarget.Read

        assertEquals("https://x/feed", target.origin)
        assertTrue(!target.startPage)
    }

    @Test
    fun `a non http single url becomes an external target`() = runTest {
        store.sources = listOf(rssSource("a", singleUrl = true, sortUrl = "market://details"))

        assertEquals(
            RssOpenTarget.External("market://details"),
            rss.resolveOpenTarget("a").getOrThrow(),
        )
    }

    @Test
    fun `a js sort url is evaluated by the host`() = runTest {
        val host = RecordingScriptHost(result = "https://x/from-js")
        val repository = DefaultRssRepository(store, host, removalHost)
        store.sources = listOf(rssSource("a", singleUrl = true, sortUrl = "<js>go()</js>"))

        val target = repository.resolveOpenTarget("a").getOrThrow() as RssOpenTarget.Read

        assertEquals(listOf("go()"), host.scripts)
        assertEquals("https://x/from-js", target.origin)
    }

    @Test
    fun `a js sort url that returns nothing falls back to the configured value`() = runTest {
        store.sources = listOf(rssSource("a", singleUrl = true, sortUrl = "@js:go()"))

        val target = rss.resolveOpenTarget("a").getOrThrow()

        assertEquals(listOf("go()"), scriptHost.scripts)
        assertEquals(RssOpenTarget.External("@js:go()"), target)
    }

    @Test
    fun `an unknown source cannot be opened`() = runTest {
        assertTrue(rss.resolveOpenTarget("ghost").isFailure)
    }

    @Test
    fun `pinning moves the source above the current minimum order`() = runTest {
        store.sources = listOf(rssSource("a", order = 3), rssSource("b", order = 1))

        assertEquals(RssCommandResult.Success, rss.pinSource("a"))
        assertEquals(0, store.sources.first { it.sourceUrl == "a" }.customOrder)
    }

    @Test
    fun `repeating a disable keeps the SSOT idempotent`() = runTest {
        store.sources = listOf(rssSource("a"))

        rss.disableSource("a")
        assertEquals(RssCommandResult.Success, rss.disableSource("a"))
        assertTrue(!store.sources.single().enabled)
    }

    @Test
    fun `deletion is delegated to the single removal owner`() = runTest {
        store.sources = listOf(rssSource("a"))

        assertEquals(RssCommandResult.Success, rss.deleteSource("a"))
        assertEquals(listOf("a"), removalHost.deleted)
    }

    @Test
    fun `commands on an unknown source fail without touching the SSOT`() = runTest {
        assertTrue(rss.pinSource("ghost") is RssCommandResult.Failure)
        assertTrue(rss.disableSource("ghost") is RssCommandResult.Failure)
        assertTrue(rss.deleteSource("ghost") is RssCommandResult.Failure)
        assertEquals(emptyList<String>(), removalHost.deleted)
    }

    @Test
    fun `a failing write is reported as a command failure`() = runTest {
        store.sources = listOf(rssSource("a"))
        store.failOn = "pinSource"

        assertTrue(rss.pinSource("a") is RssCommandResult.Failure)
    }
}
