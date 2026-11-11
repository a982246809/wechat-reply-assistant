package io.github.a982246809.replyassistant

import io.github.a982246809.replyassistant.dto.Msg
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeChatTimeAnchorParserTest {
    @Test
    fun `同一记录行的微信时间原样绑定到消息`() {
        val messages = attachWeChatTimeAnchors(
            listOf(
                WeChatVisibleChatRecord(
                    timeLabel = "昨天 晚上8:55",
                    message = Msg("我", "没啥用")
                )
            )
        )

        assertEquals("昨天 晚上8:55", messages.single().timeLabel)
        assertEquals("没啥用", messages.single().content)
    }

    @Test
    fun `独立时间行跨过无效记录后只绑定下一条有效消息`() {
        val messages = attachWeChatTimeAnchors(
            listOf(
                WeChatVisibleChatRecord(timeLabel = "晚上9:01"),
                WeChatVisibleChatRecord(message = Msg("对方", "   ")),
                WeChatVisibleChatRecord(message = Msg("对方", "那你咋不做")),
                WeChatVisibleChatRecord(message = Msg("我", "有人一起吃我就做"))
            )
        )

        assertEquals(listOf("那你咋不做", "有人一起吃我就做"), messages.map { it.content })
        assertEquals("晚上9:01", messages[0].timeLabel)
        assertNull(messages[1].timeLabel)
    }

    @Test
    fun `连续时间锚点以后一个非空原文为准`() {
        val messages = attachWeChatTimeAnchors(
            listOf(
                WeChatVisibleChatRecord(timeLabel = "晚上9:01"),
                WeChatVisibleChatRecord(timeLabel = "   "),
                WeChatVisibleChatRecord(timeLabel = "昨天 晚上9:04"),
                WeChatVisibleChatRecord(message = Msg("我", "继续聊"))
            )
        )

        assertEquals("昨天 晚上9:04", messages.single().timeLabel)
    }

    @Test
    fun `末尾孤立时间不泄漏到下一次解析`() {
        val first = attachWeChatTimeAnchors(
            listOf(WeChatVisibleChatRecord(timeLabel = "晚上9:04"))
        )
        val second = attachWeChatTimeAnchors(
            listOf(WeChatVisibleChatRecord(message = Msg("我", "下一屏")))
        )

        assertEquals(emptyList<Msg>(), first)
        assertNull(second.single().timeLabel)
    }
}
