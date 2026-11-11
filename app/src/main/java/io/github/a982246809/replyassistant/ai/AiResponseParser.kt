package io.github.a982246809.replyassistant.ai

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiResponseParser @Inject constructor(
    private val json: Json
) {
    fun parse(content: String, replyCount: Int): List<String> {
        val cleaned = content
            .trim()
            .removePrefix("```json")
            .removePrefix("```JSON")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val parsed = runCatching { json.parseToJsonElement(cleaned) }
            .getOrElse { throw AiProviderFailure.InvalidResponse }

        if (parsed is JsonObject) {
            val error = (parsed["error"] as? JsonPrimitive)?.contentOrNull()
            if (error == "VISION_CONTENT_UNREADABLE") throw AiProviderFailure.VisionUnreadable
        }

        val rawChoices = when (parsed) {
            is JsonArray -> parsed.mapNotNull { it.asMessageText() }
            is JsonObject -> parsed["choices"]?.let { choices ->
                (choices as? JsonArray)?.mapNotNull { it.asMessageText() }
            }.orEmpty()
            else -> emptyList()
        }

        val choices = rawChoices
            .map { it.replace(Regex("[!！]"), " ").trim() }
            .filter(String::isNotBlank)
            .take(replyCount)
        if (choices.isEmpty()) throw AiProviderFailure.InvalidResponse
        return choices
    }

    private fun kotlinx.serialization.json.JsonElement.asMessageText(): String? {
        val primitive = this as? JsonPrimitive
        if (primitive != null) return primitive.contentOrNull()
        val message = (this as? JsonObject)?.get("message") as? JsonObject
        return (message?.get("content") as? JsonPrimitive)?.contentOrNull()
    }

    private fun JsonPrimitive.contentOrNull(): String? =
        if (isString) content.trim().takeIf(String::isNotBlank) else null
}
