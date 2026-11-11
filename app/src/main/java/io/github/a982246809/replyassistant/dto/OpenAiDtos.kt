package io.github.a982246809.replyassistant.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatCompletionMessage>
)

@Serializable
data class ChatCompletionMessage(
    val role: String,
    val content: JsonElement
)

@Serializable
data class ChatCompletionResponse(
    val choices: List<ChatCompletionChoice> = emptyList(),
    val error: OpenAiError? = null
)

@Serializable
data class ChatCompletionChoice(
    val index: Int = 0,
    val message: ChatCompletionResponseMessage? = null
)

@Serializable
data class ChatCompletionResponseMessage(
    val role: String? = null,
    val content: JsonElement? = null
)

@Serializable
data class ModelListResponse(
    val data: List<ModelInfoResponse> = emptyList()
)

@Serializable
data class ModelInfoResponse(
    val id: String,
    @SerialName("owned_by") val ownedBy: String? = null
)

@Serializable
data class OpenAiError(
    val message: String? = null,
    val type: String? = null,
    val code: String? = null
)
