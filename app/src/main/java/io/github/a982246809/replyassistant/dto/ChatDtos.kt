package io.github.a982246809.replyassistant.dto

import kotlinx.serialization.Serializable

object ChatMessageType {
    const val VOICE_TRANSCRIPT = "voice_transcript"
}

@Serializable
data class Msg(
    val role: String,
    val content: String,
    val timeLabel: String? = null,
    val messageType: String? = null
)

object AiRequestScene {
    const val MANUAL = "manual"
    const val REALTIME_SUGGESTION = "realtime_suggestion"
}

data class AiConversationContext(
    val chatName: String?,
    val chatRecords: List<Msg>,
    val inputText: String?
)
