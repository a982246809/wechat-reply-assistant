package io.github.a982246809.replyassistant.data.repository

import android.util.Base64
import io.github.a982246809.replyassistant.ai.AiPromptBuilder
import io.github.a982246809.replyassistant.ai.AiProviderConfig
import io.github.a982246809.replyassistant.ai.AiProviderFailure
import io.github.a982246809.replyassistant.ai.AiProviderState
import io.github.a982246809.replyassistant.ai.AiResponseParser
import io.github.a982246809.replyassistant.data.datastore.UserPreferences
import io.github.a982246809.replyassistant.data.provider.AiProviderConfigStore
import io.github.a982246809.replyassistant.data.provider.AiProviderService
import io.github.a982246809.replyassistant.data.provider.AiSecretStore
import io.github.a982246809.replyassistant.dto.AiConversationContext
import io.github.a982246809.replyassistant.dto.AiRequestScene
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRepository @Inject constructor(
    private val configStore: AiProviderConfigStore,
    private val providerService: AiProviderService,
    private val promptBuilder: AiPromptBuilder,
    private val responseParser: AiResponseParser,
    private val userPreferences: UserPreferences,
    private val secretStore: AiSecretStore
) {
    val providerState: Flow<AiProviderState> = configStore.state

    suspend fun generateReplies(
        chatRecords: List<io.github.a982246809.replyassistant.dto.Msg>,
        inputText: String? = null,
        chatName: String? = null,
        replyCountOverride: Int? = null,
        requestScene: String = AiRequestScene.MANUAL
    ): Result<List<String>> = generateReplies(
        context = AiConversationContext(chatName, chatRecords, inputText),
        replyCountOverride = replyCountOverride,
        requestScene = requestScene
    )

    suspend fun generateReplies(
        context: AiConversationContext,
        replyCountOverride: Int? = null,
        @Suppress("UNUSED_PARAMETER") requestScene: String = AiRequestScene.MANUAL
    ): Result<List<String>> = runProviderRequest {
        val config = requireConfig()
        val replyCount = replyCountOverride ?: userPreferences.replyCount.first()
        val raw = providerService.complete(
            config = config,
            model = config.textModel,
            messages = promptBuilder.textMessages(context, replyCount),
            apiKey = secretStore.readApiKey()
        ).getOrThrow()
        responseParser.parse(raw, replyCount)
    }

    suspend fun generateVisualReplies(
        context: AiConversationContext,
        screenshotJpeg: ByteArray,
        replyCountOverride: Int? = null
    ): Result<List<String>> = runProviderRequest {
        val config = requireConfig()
        val replyCount = replyCountOverride ?: userPreferences.replyCount.first()
        val base64 = Base64.encodeToString(screenshotJpeg, Base64.NO_WRAP)
        val raw = providerService.complete(
            config = config,
            model = config.effectiveVisionModel,
            messages = promptBuilder.visionMessages(context, base64, replyCount),
            apiKey = secretStore.readApiKey()
        ).getOrThrow()
        responseParser.parse(raw, replyCount)
    }

    suspend fun listModels(): Result<List<String>> = runProviderRequest {
        providerService.listModels(requireConfig(), secretStore.readApiKey()).getOrThrow()
    }

    fun getReplyCount(): Flow<Int> = userPreferences.replyCount

    suspend fun setReplyCount(count: Int) {
        userPreferences.setReplyCount(count)
    }

    fun getFloatingBallEnabled(): Flow<Boolean> = userPreferences.floatingBallEnabled

    suspend fun setFloatingBallEnabled(enabled: Boolean) {
        userPreferences.setFloatingBallEnabled(enabled)
    }

    private suspend fun requireConfig(): AiProviderConfig =
        configStore.load() ?: throw AiProviderFailure.NotConfigured

    private suspend inline fun <T> runProviderRequest(
        crossinline block: suspend () -> T
    ): Result<T> = try {
        Result.success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        Result.failure(error.asProviderFailure())
    }

    private fun Throwable.asProviderFailure(): Throwable =
        if (this is AiProviderFailure) this else AiProviderFailure.Server("AI 请求失败，请稍后重试")
}
