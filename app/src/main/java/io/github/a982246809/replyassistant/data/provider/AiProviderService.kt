package io.github.a982246809.replyassistant.data.provider

import io.github.a982246809.replyassistant.ai.AiProviderConfig
import io.github.a982246809.replyassistant.ai.AiProviderFailure
import io.github.a982246809.replyassistant.api.OpenAiCompatibleApi
import io.github.a982246809.replyassistant.dto.ChatCompletionMessage
import io.github.a982246809.replyassistant.dto.ChatCompletionRequest
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import okhttp3.ResponseBody
import retrofit2.HttpException
import java.io.InterruptedIOException
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiProviderService @Inject constructor(
    private val api: OpenAiCompatibleApi,
    private val json: Json
) {
    suspend fun listModels(
        config: AiProviderConfig,
        apiKey: String?
    ): Result<List<String>> = try {
        val response = api.listModels(
            config.modelsUrl,
            config.authorizationHeader(apiKey)
        )
        Result.success(response.data.map { it.id }.filter(String::isNotBlank).distinct().sorted())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        Result.failure(error.toProviderFailure(apiKey))
    }

    suspend fun complete(
        config: AiProviderConfig,
        model: String,
        messages: List<ChatCompletionMessage>,
        apiKey: String?
    ): Result<String> = try {
        val response = api.chatCompletions(
            config.chatCompletionsUrl,
            config.authorizationHeader(apiKey),
            ChatCompletionRequest(model = model, messages = messages)
        )
        val content = response.choices.firstOrNull()?.message?.content
            ?: throw AiProviderFailure.InvalidResponse
        Result.success(content.asTextContent())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        Result.failure(error.toProviderFailure(apiKey))
    }

    private fun AiProviderConfig.authorizationHeader(apiKey: String?): String? =
        apiKey?.takeIf(String::isNotBlank)?.let { "Bearer $it" }

    private fun Throwable.toProviderFailure(apiKey: String?): AiProviderFailure = when (this) {
        is AiProviderFailure -> this
        is SocketTimeoutException -> AiProviderFailure.Network("模型请求超时，请检查网络或服务状态")
        is InterruptedIOException -> AiProviderFailure.Network("模型请求超时，请检查网络或服务状态")
        is HttpException -> mapHttpFailure(code(), response()?.errorBody(), apiKey)
        is IOException -> AiProviderFailure.Network("无法连接模型服务，请检查 URL 和网络")
        else -> AiProviderFailure.Server("模型请求失败，请稍后重试")
    }

    private fun mapHttpFailure(
        code: Int,
        errorBody: ResponseBody?,
        apiKey: String?
    ): AiProviderFailure {
        val message = runCatching { errorBody?.string() }.getOrNull()
            ?.takeIf(String::isNotBlank)
            ?.let(::extractErrorMessage)
            ?.redactApiKey(apiKey)
        return when (code) {
            401, 403 -> AiProviderFailure.Unauthorized
            404 -> AiProviderFailure.NotFound
            429 -> AiProviderFailure.RateLimited
            in 500..599 -> AiProviderFailure.Server("模型服务暂时不可用")
            400 -> when {
                message?.contains("vision", true) == true ||
                    message?.contains("image", true) == true -> AiProviderFailure.VisionUnsupported
                else -> AiProviderFailure.InvalidConfiguration(message.orEmpty())
            }
            else -> AiProviderFailure.Server(
                message?.takeIf(String::isNotBlank) ?: "模型请求失败（HTTP $code）"
            )
        }
    }

    private fun extractErrorMessage(raw: String): String? = runCatching {
        json.parseToJsonElement(raw)
            .jsonObject["error"]?.jsonObject?.get("message")?.let { element ->
                if (element is JsonPrimitive) element.content else null
            }
    }.getOrNull()?.take(200)

    private fun String?.redactApiKey(apiKey: String?): String? {
        if (this == null || apiKey.isNullOrBlank()) return this
        return replace(apiKey, "[REDACTED]")
    }

    private fun kotlinx.serialization.json.JsonElement.asTextContent(): String {
        val primitive = this as? JsonPrimitive
        if (primitive != null && primitive.isString) return primitive.content
        val array = this as? JsonArray ?: return toString()
        return array.mapNotNull { element ->
            val objectValue = element as? kotlinx.serialization.json.JsonObject ?: return@mapNotNull null
            val text = objectValue["text"] as? JsonPrimitive
            text?.takeIf(JsonPrimitive::isString)?.content
        }.joinToString("")
    }
}
