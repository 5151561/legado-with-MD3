package io.legado.app.feature.catalog.impl

import io.legado.app.data.entities.SourceCatalogCounts
import io.legado.app.feature.catalog.api.SourceCatalogKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 画板 D-00 的计数投影契约，跑在正式实现上。 */
class SourceHubImplContractTest {

    private val counts = SourceCatalogCounts(
        bookSourceTotal = 312,
        bookSourceEnabled = 208,
        bookSourceUnhealthy = 3,
        rssSourceTotal = 8,
        rssSourceEnabled = 8,
        httpTtsTotal = 4,
        replaceRuleTotal = 23,
        replaceRuleEnabled = 19,
        txtTocRuleTotal = 9,
        txtTocRuleBuiltIn = 6,
        dictRuleTotal = 3,
        dictRuleEnabled = 3,
        contentHighlightTotal = 6,
        contentHighlightEnabled = 6,
        tagHighlightTotal = 4,
        tagHighlightEnabled = 4,
        ruleSubscriptionTotal = 2,
    )

    private val store = FakeSourceCatalogStore(counts)
    private val defaultTtsId = MutableStateFlow<String?>(null)
    private val hub = DefaultSourceHubRepository(store) { defaultTtsId }

    @Test
    fun `every kind is reported, in the declared order`() = runTest {
        val kinds = hub.observeSourceCatalog().first().map { it.kind }

        assertEquals(SourceCatalogKind.entries, kinds)
    }

    @Test
    fun `book source health comes from the persisted invalid markers`() = runTest {
        val bookSource = hub.observeSourceCatalog().first()
            .single { it.kind == SourceCatalogKind.BookSource }

        assertEquals(312, bookSource.total)
        assertEquals(208, bookSource.enabled)
        assertEquals(3, bookSource.unhealthy)
    }

    @Test
    fun `the default tts engine is named only when http tts is the one selected`() = runTest {
        // ttsEngine 一个字段承载三种引擎；选的是系统或云引擎时，这个 id 在 httpTTS 表里查不到。
        assertNull(httpTts().defaultName)

        defaultTtsId.value = "1700000000000"
        store.httpTtsNames["1700000000000"] = "云雀"

        assertEquals("云雀", httpTts().defaultName)
    }

    private suspend fun httpTts() = hub.observeSourceCatalog().first()
        .single { it.kind == SourceCatalogKind.HttpTts }

    @Test
    fun `kinds without an enable switch report null instead of zero`() = runTest {
        val all = hub.observeSourceCatalog().first().associateBy { it.kind }

        assertNull(all.getValue(SourceCatalogKind.HttpTts).enabled)
        assertNull(all.getValue(SourceCatalogKind.TxtTocRule).enabled)
        assertNull(all.getValue(SourceCatalogKind.RuleSubscription).enabled)
    }

    @Test
    fun `built in txt toc rules are counted separately`() = runTest {
        val txtToc = hub.observeSourceCatalog().first()
            .single { it.kind == SourceCatalogKind.TxtTocRule }

        assertEquals(9, txtToc.total)
        assertEquals(6, txtToc.builtIn)
    }
}
