package io.github.a982246809.replyassistant.accessibility

import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import io.github.a982246809.replyassistant.dto.ChatMessageType
import io.github.a982246809.replyassistant.dto.Msg
import java.security.MessageDigest

/** Synchronous result for the currently visible WeChat frame. No screenshot is taken while reading. */
internal sealed interface WeChatChatMessagesResult {
    val frameKey: WeChatVoiceFrameKey?

    data class TextReady(
        override val frameKey: WeChatVoiceFrameKey,
        val messages: List<Msg>
    ) : WeChatChatMessagesResult

    data class VisionRequired(
        override val frameKey: WeChatVoiceFrameKey,
        val accessibleMessages: List<Msg>,
        val windowId: Int
    ) : WeChatChatMessagesResult

    data class Blocked(
        override val frameKey: WeChatVoiceFrameKey?,
        val reason: WeChatChatMessagesBlockedReason
    ) : WeChatChatMessagesResult
}

internal enum class WeChatChatMessagesBlockedReason {
    VOICE_CONVERSION_REQUIRED,
    READ_FAILED
}

/** Stable identity for one visible chat frame, including voice-node bounds. */
internal data class WeChatVoiceFrameKey(
    val generation: Long,
    val normalizedChatTitle: String,
    val windowId: Int,
    val fingerprint: String
)

internal data class WeChatWindowBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val isEmpty: Boolean get() = left >= right || top >= bottom
    val fingerprintValue: String get() = "$left,$top,$right,$bottom"
}

/**
 * Deep module at the "visible WeChat nodes -> text/vision/blocked" boundary.
 *
 * The module never starts asynchronous work. In particular, a background page read can never
 * cause a screenshot or upload.
 */
internal class WeChatPageReader(
    private val frameReader: WeChatVoiceFrameReader = AccessibilityWeChatVoiceFrameReader()
) {
    fun read(
        generation: Long,
        chatTitle: String?,
        root: AccessibilityNodeInfo
    ): WeChatChatMessagesResult = try {
        when (val frame = frameReader.read(generation, chatTitle, root)) {
            is WeChatFrameRead.TextReady -> WeChatChatMessagesResult.TextReady(
                frame.frameKey,
                frame.messages
            )
            is WeChatFrameRead.VisionRequired -> WeChatChatMessagesResult.VisionRequired(
                frame.frameKey,
                frame.accessibleMessages,
                frame.windowId
            )
            is WeChatFrameRead.Blocked -> WeChatChatMessagesResult.Blocked(
                frame.frameKey,
                frame.reason
            )
        }
    } catch (_: Throwable) {
        WeChatChatMessagesResult.Blocked(
            frameKey = null,
            reason = WeChatChatMessagesBlockedReason.READ_FAILED
        )
    }

    fun currentFrameKey(
        generation: Long,
        chatTitle: String?,
        root: AccessibilityNodeInfo
    ): WeChatVoiceFrameKey? = try {
        frameReader.read(generation, chatTitle, root).frameKey
    } catch (_: Throwable) {
        null
    }
}

internal fun interface WeChatVoiceFrameReader {
    fun read(
        generation: Long,
        chatTitle: String?,
        root: AccessibilityNodeInfo
    ): WeChatFrameRead
}

internal sealed interface WeChatFrameRead {
    val frameKey: WeChatVoiceFrameKey

    data class TextReady(
        override val frameKey: WeChatVoiceFrameKey,
        val messages: List<Msg>
    ) : WeChatFrameRead

    data class VisionRequired(
        override val frameKey: WeChatVoiceFrameKey,
        val accessibleMessages: List<Msg>,
        val windowId: Int
    ) : WeChatFrameRead

    data class Blocked(
        override val frameKey: WeChatVoiceFrameKey,
        val reason: WeChatChatMessagesBlockedReason
    ) : WeChatFrameRead
}

internal data class WeChatParsedRecord(
    val timeLabel: String?,
    val role: String?,
    val rawMessageText: String?,
    val content: String?,
    val unresolvedVoice: Boolean = false,
    val messageType: String? = null
)

internal data class WeChatRecordNodeSnapshot(
    val timeLabel: String?,
    val rawMessageText: String?,
    val directionHasSecondChild: Boolean,
    val firstDirectionChildClassName: String?,
    val visibleVoiceContainers: List<WeChatVoiceContainerSnapshot>
)

internal data class WeChatVoiceContainerSnapshot(
    val transcriptNodeTexts: List<String>,
    val visibleTranscriptNodeCount: Int = transcriptNodeTexts.size,
    val boundsInWindow: WeChatWindowBounds?
)

/** WeChat-specific policy for filtering interface labels from accessible node text. */
internal object WeChatNodeTextPolicy {
    private val durationLabel = Regex(
        "^\\d+\\s*(?:[\"″”“′']|秒|s|sec)$",
        RegexOption.IGNORE_CASE
    )
    private val interfaceLabels = setOf(
        "转文字",
        "转文字失败",
        "重新转文字",
        "取消转文字",
        "正在转文字",
        "文字转换中",
        "转成文字",
        "转换为文字",
        "转换文字",
        "语音转文字",
        "语音转文本",
        "转文本",
        "重新转换",
        "正在转换",
        "识别中",
        "收起",
        "展开",
        "播放",
        "暂停"
    )
    private val interfaceLabelWithDuration = Regex(
        "^(?:转文字|重新转文字|取消转文字|正在转文字|文字转换中|转成文字|" +
            "转换为文字|转换文字|语音转文字|语音转文本|转文本|重新转换|正在转换|识别中)" +
            "\\s*\\d*(?:[\"″”“′']|秒|s|sec)?$",
        RegexOption.IGNORE_CASE
    )
    private val interfaceFragments = listOf(
        "转文字",
        "转成文字",
        "转换为文字",
        "转换文字",
        "语音转文本",
        "转文本",
        "文字转换中",
        "重新转换",
        "正在转换",
        "识别中"
    )

    fun normalize(value: String): String = value.trim { character ->
        character.isWhitespace() || Character.isSpaceChar(character)
    }

    fun isInterfaceLabel(value: String): Boolean {
        val normalized = normalize(value)
        val collapsedWhitespace = normalized.replace(Regex("\\s+"), " ")
        return durationLabel.matches(collapsedWhitespace) ||
            collapsedWhitespace in interfaceLabels ||
            interfaceLabelWithDuration.matches(collapsedWhitespace) ||
            interfaceFragments.any(collapsedWhitespace::contains)
    }
}

/** Pure parser used by the production accessibility reader and JVM tests. */
internal object WeChatVoiceRecordParser {
    private val voiceDurationMarker = Regex("^\\d+\"$")

    fun parse(
        generation: Long,
        chatTitle: String?,
        windowId: Int,
        records: List<WeChatRecordNodeSnapshot>
    ): WeChatFrameRead {
        val parsedRecords = mutableListOf<WeChatParsedRecord>()
        var needsVision = false
        var needsConversion = false
        var readFailed = false

        records.forEach { record ->
            val rawText = record.rawMessageText
            val normalizedRawText = rawText?.let(WeChatNodeTextPolicy::normalize)
            val role = readRole(record)
            if (normalizedRawText == null || !voiceDurationMarker.matches(normalizedRawText)) {
                parsedRecords += WeChatParsedRecord(
                    timeLabel = record.timeLabel,
                    role = role,
                    rawMessageText = rawText,
                    content = rawText?.takeIf { role != null }
                )
                return@forEach
            }

            if (role == null) {
                readFailed = true
                parsedRecords += WeChatParsedRecord(record.timeLabel, null, rawText, null)
                return@forEach
            }

            when (record.visibleVoiceContainers.size) {
                0 -> {
                    needsConversion = true
                    parsedRecords += WeChatParsedRecord(
                        record.timeLabel,
                        role,
                        rawText,
                        content = null,
                        unresolvedVoice = true
                    )
                }
                1 -> {
                    val container = record.visibleVoiceContainers.single()
                    if (container.visibleTranscriptNodeCount != 1) {
                        readFailed = true
                        parsedRecords += WeChatParsedRecord(
                            record.timeLabel,
                            role,
                            rawText,
                            content = null,
                            unresolvedVoice = true
                        )
                        return@forEach
                    }
                    val candidates = container.transcriptNodeTexts
                        .map(WeChatNodeTextPolicy::normalize)
                        .filter(String::isNotEmpty)
                        .filterNot(WeChatNodeTextPolicy::isInterfaceLabel)
                        .distinct()
                    when (candidates.size) {
                        0 -> {
                            needsVision = true
                            parsedRecords += WeChatParsedRecord(
                                record.timeLabel,
                                role,
                                rawText,
                                content = null,
                                unresolvedVoice = true
                            )
                        }
                        1 -> parsedRecords += WeChatParsedRecord(
                            record.timeLabel,
                            role,
                            rawText,
                            content = candidates.single(),
                            messageType = ChatMessageType.VOICE_TRANSCRIPT
                        )
                        else -> {
                            readFailed = true
                            parsedRecords += WeChatParsedRecord(
                                record.timeLabel,
                                role,
                                rawText,
                                content = null,
                                unresolvedVoice = true
                            )
                        }
                    }
                }
                else -> {
                    readFailed = true
                    parsedRecords += WeChatParsedRecord(
                        record.timeLabel,
                        role,
                        rawText,
                        content = null,
                        unresolvedVoice = true
                    )
                }
            }
        }

        val frameKey = WeChatVoiceFrameKey(
            generation = generation,
            normalizedChatTitle = chatTitle?.let(WeChatNodeTextPolicy::normalize).orEmpty(),
            windowId = windowId,
            fingerprint = fingerprint(records, parsedRecords)
        )
        val accessibleMessages = visibleMessages(parsedRecords)
        return when {
            needsConversion -> WeChatFrameRead.Blocked(
                frameKey,
                WeChatChatMessagesBlockedReason.VOICE_CONVERSION_REQUIRED
            )
            readFailed -> WeChatFrameRead.Blocked(
                frameKey,
                WeChatChatMessagesBlockedReason.READ_FAILED
            )
            needsVision -> WeChatFrameRead.VisionRequired(
                frameKey,
                accessibleMessages,
                windowId
            )
            else -> WeChatFrameRead.TextReady(frameKey, accessibleMessages)
        }
    }

    private fun readRole(record: WeChatRecordNodeSnapshot): String? {
        if (record.rawMessageText == null || !record.directionHasSecondChild) return null
        return when (record.firstDirectionChildClassName ?: return null) {
            "android.view.View", "android.widget.LinearLayout" -> "我"
            else -> "对方"
        }
    }

    private fun visibleMessages(records: List<WeChatParsedRecord>): List<Msg> {
        var pendingTimeLabel: String? = null
        return buildList {
            records.forEach { record ->
                record.timeLabel
                    ?.takeIf(String::isNotBlank)
                    ?.let { pendingTimeLabel = it }
                if (record.unresolvedVoice) {
                    pendingTimeLabel = null
                    return@forEach
                }
                val content = record.content?.takeIf(String::isNotBlank) ?: return@forEach
                val role = record.role ?: return@forEach
                add(
                    Msg(
                        role = role,
                        content = content,
                        timeLabel = pendingTimeLabel,
                        messageType = record.messageType
                    )
                )
                pendingTimeLabel = null
            }
        }
    }

    private fun fingerprint(
        records: List<WeChatRecordNodeSnapshot>,
        parsedRecords: List<WeChatParsedRecord>
    ): String {
        val digest = MessageDigest.getInstance("SHA-256")
        records.forEachIndexed { index, record ->
            val parsed = parsedRecords[index]
            digest.addLengthPrefixed(index.toString())
            digest.addLengthPrefixed(record.timeLabel)
            digest.addLengthPrefixed(parsed.role)
            digest.addLengthPrefixed(record.rawMessageText)
            digest.addLengthPrefixed(parsed.content)
            digest.addLengthPrefixed(parsed.messageType)
            digest.addLengthPrefixed(record.visibleVoiceContainers.size.toString())
            record.visibleVoiceContainers.forEach { container ->
                digest.addLengthPrefixed(container.visibleTranscriptNodeCount.toString())
                container.transcriptNodeTexts.forEach { text ->
                    digest.addLengthPrefixed(text)
                }
                digest.addLengthPrefixed(container.boundsInWindow?.fingerprintValue)
            }
        }
        return digest.digest().joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private fun MessageDigest.addLengthPrefixed(value: String?) {
        val bytes = value.orEmpty().toByteArray(Charsets.UTF_8)
        update(bytes.size.toString().toByteArray(Charsets.UTF_8))
        update(':'.code.toByte())
        update(bytes)
        update('|'.code.toByte())
    }
}

internal enum class WeChatReadConsumer {
    SHORT_TAP,
    LONG_PRESS,
    MANUAL_REFRESH,
    AUTOMATIC,
    REALTIME
}

internal enum class WeChatReadAction {
    USE_TEXT,
    CAPTURE_WINDOW,
    PROMPT_CONVERSION,
    PROMPT_SHORT_TAP,
    PLATFORM_UNSUPPORTED,
    SKIP_SILENTLY,
    FAIL
}

internal fun decideWeChatReadAction(
    result: WeChatChatMessagesResult,
    consumer: WeChatReadConsumer,
    apiLevel: Int = Build.VERSION.SDK_INT
): WeChatReadAction {
    val background = consumer == WeChatReadConsumer.AUTOMATIC ||
        consumer == WeChatReadConsumer.REALTIME
    return when (result) {
        is WeChatChatMessagesResult.TextReady -> WeChatReadAction.USE_TEXT
        is WeChatChatMessagesResult.VisionRequired -> when {
            background -> WeChatReadAction.SKIP_SILENTLY
            consumer == WeChatReadConsumer.LONG_PRESS ||
                consumer == WeChatReadConsumer.MANUAL_REFRESH ->
                WeChatReadAction.PROMPT_SHORT_TAP
            apiLevel < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                WeChatReadAction.PLATFORM_UNSUPPORTED
            else -> WeChatReadAction.CAPTURE_WINDOW
        }
        is WeChatChatMessagesResult.Blocked -> when {
            background -> WeChatReadAction.SKIP_SILENTLY
            result.reason == WeChatChatMessagesBlockedReason.VOICE_CONVERSION_REQUIRED ->
                WeChatReadAction.PROMPT_CONVERSION
            else -> WeChatReadAction.FAIL
        }
    }
}
