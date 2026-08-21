package io.legado.app.feature.ai.impl

import io.legado.app.data.entities.AiModelProfile
import io.legado.app.data.entities.AiProviderProfile
import io.legado.app.data.entities.AiTaskPreset
import io.legado.app.domain.model.AiTaskType
import io.legado.app.feature.ai.api.AiCommandResult
import io.legado.app.feature.ai.api.AiQueryState
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
 * The AI overview API contract, executed against the formal implementation. These cases previously
 * ran against the deleted `LegacyAiAdapter`; the expectations are unchanged.
 */
class AiImplContractTest {

    private val store = FakeAiProfileStore()
    private val host = RecordingDefaultModelHost()
    private val ai = DefaultAiRepository(store, host)

    @Test
    fun `overview emits loading before the first SSOT snapshot`() = runTest {
        store.providers = listOf(provider("p"))
        store.models = listOf(model("m", "p"))

        val states = ai.observeOverview().take(2).toList()

        assertEquals(AiQueryState.Loading, states.first())
        val overview = (states[1] as AiQueryState.Data).overview
        assertEquals(listOf("p"), overview.providers.map { it.id })
        assertEquals(listOf("m"), overview.models.map { it.id })
    }

    @Test
    fun `an unconfigured install is data, not a failure`() = runTest {
        val state = ai.observeOverview().take(2).toList()[1]

        assertTrue(state is AiQueryState.Data)
        assertEquals(0, (state as AiQueryState.Data).overview.presetCount)
    }

    @Test
    fun `a query error is reported as retryable`() = runTest {
        store.presetsFlow = flow { error("db closed") }

        assertEquals(
            AiQueryState.Failed(retryable = true),
            ai.observeOverview().take(2).toList()[1],
        )
    }

    @Test
    fun `a model of a disabled provider is not usable`() = runTest {
        store.providers = listOf(provider("on", enabled = true), provider("off", enabled = false))
        store.models = listOf(
            model("a", "on", enabled = true),
            model("b", "on", enabled = false),
            model("c", "off", enabled = true),
        )

        val overview = (ai.observeOverview().take(2).toList()[1] as AiQueryState.Data).overview

        assertEquals(
            mapOf("a" to true, "b" to false, "c" to false),
            overview.models.associate { it.id to it.enabled },
        )
    }

    @Test
    fun `a model of an unknown provider is not usable`() = runTest {
        store.models = listOf(model("a", "ghost", enabled = true))

        val overview = (ai.observeOverview().take(2).toList()[1] as AiQueryState.Data).overview

        assertTrue(!overview.models.single().enabled)
    }

    @Test
    fun `the default model comes from the default translate preset`() = runTest {
        store.providers = listOf(provider("p"))
        store.models = listOf(model("a", "p"), model("b", "p"))
        store.presets = listOf(
            preset(AiTaskType.CHAT, modelProfileId = "a", isDefault = true),
            preset(AiTaskType.TRANSLATE_CHAPTER, modelProfileId = "b", isDefault = false),
            preset(AiTaskType.TRANSLATE_CHAPTER, modelProfileId = "a", isDefault = true),
        )

        val overview = (ai.observeOverview().take(2).toList()[1] as AiQueryState.Data).overview

        assertEquals(
            mapOf("a" to true, "b" to false),
            overview.models.associate { it.id to it.isDefault },
        )
        assertEquals(3, overview.presetCount)
    }

    @Test
    fun `setting the default model is delegated to the single write owner`() = runTest {
        assertEquals(AiCommandResult.Success, ai.setDefaultModel("a"))
        assertEquals(listOf("a"), host.requested)
    }

    @Test
    fun `a failing default model write is reported as a command failure`() = runTest {
        host.failure = IllegalStateException("Model is required")

        val result = ai.setDefaultModel("ghost") as AiCommandResult.Failure

        assertEquals("Model is required", result.message)
    }
}

internal class FakeAiProfileStore : AiProfileStore {

    var providers: List<AiProviderProfile> = emptyList()
        set(value) { field = value; revision.value += 1 }
    var models: List<AiModelProfile> = emptyList()
        set(value) { field = value; revision.value += 1 }
    var presets: List<AiTaskPreset> = emptyList()
        set(value) { field = value; revision.value += 1 }

    private val revision = MutableStateFlow(0)
    var presetsFlow: Flow<List<AiTaskPreset>>? = null

    override fun observeProviders(): Flow<List<AiProviderProfile>> = revision.map { providers }
    override fun observeModels(): Flow<List<AiModelProfile>> = revision.map { models }
    override fun observePresets(): Flow<List<AiTaskPreset>> =
        presetsFlow ?: revision.map { presets }
}

internal class RecordingDefaultModelHost : AiDefaultModelHost {
    val requested = mutableListOf<String>()
    var failure: Throwable? = null
    override suspend fun setDefaultModel(modelId: String) {
        failure?.let { throw it }
        requested += modelId
    }
}

internal fun provider(id: String, enabled: Boolean = true) = AiProviderProfile(
    id = id,
    name = id,
    protocol = "openai_chat_completions",
    baseUrl = "https://example.invalid",
    enabled = enabled,
)

internal fun model(id: String, providerId: String, enabled: Boolean = true) = AiModelProfile(
    id = id,
    providerId = providerId,
    displayName = id,
    modelId = id,
    enabled = enabled,
)

internal fun preset(
    taskType: String,
    modelProfileId: String,
    isDefault: Boolean,
) = AiTaskPreset(
    id = "$taskType-$modelProfileId-$isDefault",
    taskType = taskType,
    name = taskType,
    modelProfileId = modelProfileId,
    promptTemplate = "",
    isDefault = isDefault,
)
