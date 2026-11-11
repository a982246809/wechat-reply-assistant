package io.github.a982246809.replyassistant

import io.github.a982246809.replyassistant.chatcontext.CapturedChatSnapshot

internal data class RealtimeSuggestionContext(
    val snapshot: CapturedChatSnapshot,
    val inputText: String,
    val requestKey: String
)

internal class RealtimeSuggestionRequestGate {
    private var lastRequestKey: String? = null

    fun shouldRequest(context: RealtimeSuggestionContext): Boolean {
        if (context.requestKey == lastRequestKey) {
            return false
        }

        lastRequestKey = context.requestKey
        return true
    }

    fun reset() {
        lastRequestKey = null
    }
}

internal fun buildRealtimeSuggestionContext(
    snapshot: CapturedChatSnapshot,
    inputText: String
): RealtimeSuggestionContext? {
    if (snapshot.messages.isEmpty() || inputText.isBlank()) {
        return null
    }

    val frozenSnapshot = snapshot.copy(
        messages = snapshot.messages.map { it.copy() }
    )
    val normalizedInputText = inputText.trim()
    return RealtimeSuggestionContext(
        snapshot = frozenSnapshot,
        inputText = normalizedInputText,
        requestKey = buildRealtimeSuggestionKey(frozenSnapshot, normalizedInputText)
    )
}

internal fun buildRealtimeSuggestionKey(snapshot: CapturedChatSnapshot, inputText: String): String {
    val encodedHistory = buildString {
        append(snapshot.messages.size).append(':')
        snapshot.messages.forEach { msg ->
            append(msg.role.length).append(':').append(msg.role)
            append(msg.content.length).append(':').append(msg.content)
            append('M')
            val messageType = msg.messageType
            if (messageType == null) {
                append("-1:")
            } else {
                append(messageType.length).append(':').append(messageType)
            }
        }
    }

    return buildString {
        append('G').append(snapshot.sessionGeneration).append(':')
        append('T').append(snapshot.normalizedTitle.length).append(':')
            .append(snapshot.normalizedTitle)
        append('H').append(encodedHistory.length).append(':').append(encodedHistory)
        append('I').append(inputText.length).append(':').append(inputText)
    }
}

fun pickSuggestion(choices: List<String>): String? {
    return choices.firstOrNull { it.isNotBlank() }?.trim()
}
