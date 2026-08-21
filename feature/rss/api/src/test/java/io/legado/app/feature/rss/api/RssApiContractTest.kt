package io.legado.app.feature.rss.api

import org.junit.Assert.assertEquals
import org.junit.Test

class RssApiContractTest {
    @Test fun `read target keeps origin separate from link`() {
        val target = RssOpenTarget.Read("文章", "source", "article")
        assertEquals("source", target.origin)
        assertEquals("article", target.link)
    }
}
