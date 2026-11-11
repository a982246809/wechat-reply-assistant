package io.github.a982246809.replyassistant.chatcontext

import io.github.a982246809.replyassistant.WECHAT_SOURCE_PACKAGE
import io.github.a982246809.replyassistant.dto.AiConversationContext
import io.github.a982246809.replyassistant.dto.Msg
import io.github.a982246809.replyassistant.isWeChatDirectChatTitle
import java.text.Normalizer

data class CapturedChatSnapshot(
    val sessionGeneration: Long,
    val sourcePackage: String,
    val rawTitle: String,
    val normalizedTitle: String = ChatTextNormalizer.normalizeTitle(rawTitle),
    val messages: List<Msg>,
    val capturedAtMillis: Long
)

data class NormalizedMessage(
    val direction: String,
    val content: String
)

object ChatTextNormalizer {
    fun normalizeTitle(value: String): String = normalizeLineEndingsAndNfc(value).trim()

    fun normalizeMessage(message: Msg): NormalizedMessage = NormalizedMessage(
        direction = normalizeLineEndingsAndNfc(message.role).trim(),
        content = normalizeLineEndingsAndNfc(message.content)
    )

    private fun normalizeLineEndingsAndNfc(value: String): String = Normalizer.normalize(
        value.replace("\r\n", "\n").replace('\r', '\n'),
        Normalizer.Form.NFC
    )
}

internal fun CapturedChatSnapshot.forCapture(): CapturedChatSnapshot = copy(
    messages = messages.filter { it.messageType == null }.map { it.copy() }
)

class ConversationContextAssembler @javax.inject.Inject constructor() {
    fun assemble(
        snapshot: CapturedChatSnapshot,
        inputText: String?
    ): AiConversationContext = AiConversationContext(
        chatName = snapshot.rawTitle,
        chatRecords = snapshot.messages.map { it.copy() },
        inputText = inputText?.trim()?.takeIf(String::isNotEmpty)
    )
}

internal fun buildCapturedChatSnapshot(
    generation: Long,
    sourcePackage: String?,
    rawTitle: String?,
    messages: List<Msg>,
    capturedAtMillis: Long
): CapturedChatSnapshot? {
    if (sourcePackage != WECHAT_SOURCE_PACKAGE) return null
    val title = rawTitle?.takeIf { it.isNotBlank() } ?: return null
    if (!isWeChatDirectChatTitle(title)) return null
    return CapturedChatSnapshot(
        sessionGeneration = generation,
        sourcePackage = sourcePackage,
        rawTitle = title,
        messages = messages.map { it.copy() },
        capturedAtMillis = capturedAtMillis
    )
}
