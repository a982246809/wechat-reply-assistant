package io.github.a982246809.replyassistant

import io.github.a982246809.replyassistant.dto.Msg
import io.github.a982246809.replyassistant.chatcontext.ChatWindowContinuity

data class SuggestionTriggerActions(
    val chatEntered: Boolean = false,
    val chatExited: Boolean = false,
    val keyboardOpened: Boolean = false,
    val incomingMessageReceived: Boolean = false
)

class SuggestionTriggerState {
    private var activeChatKey: String? = null
    private var eligibleKeyboardVisible: Boolean = false
    private var observedMessages: List<Msg>? = null

    internal fun synchronizeChat(chatKey: String?) {
        val normalizedChatKey = chatKey?.trim()?.takeIf { it.isNotEmpty() } ?: return
        if (normalizedChatKey != activeChatKey) observedMessages = null
        activeChatKey = normalizedChatKey
    }

    fun update(
        chatKey: String?,
        keyboardVisible: Boolean,
        hasEditableInput: Boolean,
        messages: List<Msg> = emptyList()
    ): SuggestionTriggerActions {
        val normalizedChatKey = chatKey?.trim()?.takeIf { it.isNotEmpty() }
        val previousChatKey = activeChatKey
        val chatEntered = normalizedChatKey != null && normalizedChatKey != previousChatKey
        val chatExited = normalizedChatKey == null && previousChatKey != null
        val nextEligibleKeyboardVisible =
            normalizedChatKey != null && keyboardVisible && hasEditableInput
        val keyboardOpened = nextEligibleKeyboardVisible && !eligibleKeyboardVisible
        var incomingMessageReceived = false

        if (normalizedChatKey == null || chatEntered) {
            observedMessages = messages.takeIf { it.isNotEmpty() }?.toList()
        } else {
            val previousMessages = observedMessages
            if (previousMessages == null) {
                observedMessages = messages.takeIf { it.isNotEmpty() }?.toList()
            } else {
                val overlap = ChatWindowContinuity.forwardOverlapCount(
                    previousMessages,
                    messages
                )
                if (overlap > 0) {
                    incomingMessageReceived = messages.drop(overlap).any { it.role == "对方" }
                    observedMessages = messages.toList()
                }
            }
        }

        activeChatKey = normalizedChatKey
        eligibleKeyboardVisible = nextEligibleKeyboardVisible

        return SuggestionTriggerActions(
            chatEntered = chatEntered,
            chatExited = chatExited,
            keyboardOpened = keyboardOpened,
            incomingMessageReceived = incomingMessageReceived
        )
    }

    fun reset() {
        activeChatKey = null
        eligibleKeyboardVisible = false
        observedMessages = null
    }
}
