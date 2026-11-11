package io.github.a982246809.replyassistant

import io.github.a982246809.replyassistant.dto.Msg

internal data class WeChatVisibleChatRecord(
    val timeLabel: String? = null,
    val message: Msg? = null
)

internal fun attachWeChatTimeAnchors(
    records: List<WeChatVisibleChatRecord>
): List<Msg> {
    var pendingTimeLabel: String? = null
    return buildList {
        records.forEach { record ->
            record.timeLabel
                ?.takeIf { it.isNotBlank() }
                ?.let { pendingTimeLabel = it }
            record.message
                ?.takeIf { it.content.isNotBlank() }
                ?.let { message ->
                    add(message.copy(timeLabel = pendingTimeLabel))
                    pendingTimeLabel = null
                }
        }
    }
}
