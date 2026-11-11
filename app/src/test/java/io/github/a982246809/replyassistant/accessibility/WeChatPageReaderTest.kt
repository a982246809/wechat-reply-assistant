package io.github.a982246809.replyassistant.accessibility

import android.view.accessibility.AccessibilityNodeInfo
import io.github.a982246809.replyassistant.dto.Msg
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeChatPageReaderTest {
    private val root = mockk<AccessibilityNodeInfo>(relaxed = true)
    private val key = WeChatVoiceFrameKey(1, "好友", 9, "frame")

    @Test
    fun `文本帧同步返回且不会经过截图等待状态`() {
        val reader = WeChatPageReader(
            WeChatVoiceFrameReader { _, _, _ ->
                WeChatFrameRead.TextReady(key, listOf(Msg("对方", "你好")))
            }
        )

        val result = reader.read(1, "好友", root) as WeChatChatMessagesResult.TextReady

        assertEquals(listOf(Msg("对方", "你好")), result.messages)
        assertEquals(key, reader.currentFrameKey(1, "好友", root))
    }

    @Test
    fun `空 brv 帧同步返回 VisionRequired 且保留窗口和可访问文本`() {
        val reader = WeChatPageReader(
            WeChatVoiceFrameReader { _, _, _ ->
                WeChatFrameRead.VisionRequired(
                    frameKey = key,
                    accessibleMessages = listOf(Msg("对方", "前文")),
                    windowId = 9
                )
            }
        )

        val result = reader.read(1, "好友", root) as WeChatChatMessagesResult.VisionRequired

        assertEquals(9, result.windowId)
        assertEquals(listOf(Msg("对方", "前文")), result.accessibleMessages)
    }

    @Test
    fun `节点读取异常按普通读取失败封闭`() {
        val reader = WeChatPageReader(
            WeChatVoiceFrameReader { _, _, _ -> error("node raced") }
        )

        val result = reader.read(1, "好友", root) as WeChatChatMessagesResult.Blocked

        assertEquals(WeChatChatMessagesBlockedReason.READ_FAILED, result.reason)
        assertTrue(result.frameKey == null)
    }
}
