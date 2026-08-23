package io.legado.app.feature.catalog.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SourceHubApiContractTest {

    @Test
    fun `absent signal is null, not zero`() {
        val count = SourceCatalogCount(SourceCatalogKind.DictRule, total = 3)
        // 词典规则没有启用 / 停用的概念，也没有健康信号。
        // 若这两处默认成 0，UI 会画出「启用 0」和「0 个失效」两句假话。
        assertNull(count.enabled)
        assertNull(count.unhealthy)
        assertNotEquals(0, count.unhealthy)
    }

    @Test
    fun `unhealthy zero means nothing is flagged, which is a real answer`() {
        val count = SourceCatalogCount(SourceCatalogKind.BookSource, total = 312, enabled = 208, unhealthy = 0)
        assertEquals(0, count.unhealthy)
    }

    @Test
    fun `content and tag highlight rules are distinct kinds`() {
        // 旧 App 里这两者名字相近、去处不明，是 D-00 要解决的问题；
        // 在 api 层合并会让问题原样长回来。
        assertNotEquals(SourceCatalogKind.ContentHighlightRule, SourceCatalogKind.TagHighlightRule)
    }

    @Test
    fun `rule subscription is not an rss source`() {
        assertNotEquals(SourceCatalogKind.RuleSubscription, SourceCatalogKind.RssSource)
    }

    @Test
    fun `only http tts carries a default name`() {
        val tts = SourceCatalogCount(SourceCatalogKind.HttpTts, total = 4, defaultName = "云雀")
        assertEquals("云雀", tts.defaultName)
        assertNull(SourceCatalogCount(SourceCatalogKind.BookSource, total = 312).defaultName)
    }
}
