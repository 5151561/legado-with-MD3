package io.legado.app.feature.ai.api

import org.junit.Assert.assertFalse
import org.junit.Test

class AiApiContractTest {
    @Test fun `default model does not imply provider enabled`() {
        val model = AiModelSummary("m", "p", "模型", "model", enabled = false, isDefault = true)
        assertFalse(model.enabled)
    }
}
