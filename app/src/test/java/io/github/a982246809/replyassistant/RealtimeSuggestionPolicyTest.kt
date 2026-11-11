package io.github.a982246809.replyassistant

import io.github.a982246809.replyassistant.dto.ChatMessageType
import io.github.a982246809.replyassistant.dto.Msg
import io.github.a982246809.replyassistant.chatcontext.CapturedChatSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RealtimeSuggestionPolicyTest {

    @Test
    fun `相同上下文重复刷新不会重复请求`() {
        val gate = RealtimeSuggestionRequestGate()
        val context = requireNotNull(
            buildRealtimeSuggestionContext(
                snapshot = snapshot(Msg("对方", "有心跳")),
                inputText = "那就好"
            )
        )

        assertTrue(gate.shouldRequest(context))
        assertFalse(gate.shouldRequest(context))
    }

    @Test
    fun `输入变化后允许再次请求`() {
        val gate = RealtimeSuggestionRequestGate()
        val history = snapshot(Msg("对方", "有心跳"))
        val oldContext = requireNotNull(buildRealtimeSuggestionContext(history, "那就好"))
        val newContext = requireNotNull(buildRealtimeSuggestionContext(history, "我就放心了"))

        assertTrue(gate.shouldRequest(oldContext))
        assertTrue(gate.shouldRequest(newContext))
    }

    @Test
    fun `空字符串和纯空白输入不创建上下文`() {
        val history = snapshot(Msg("对方", "有心跳"))

        assertNull(buildRealtimeSuggestionContext(history, ""))
        assertNull(buildRealtimeSuggestionContext(history, " \t\n "))
    }

    @Test
    fun `聊天记录变化后允许再次请求`() {
        val gate = RealtimeSuggestionRequestGate()
        val oldContext = requireNotNull(
            buildRealtimeSuggestionContext(snapshot(Msg("对方", "有心跳")), "继续聊")
        )
        val newContext = requireNotNull(
            buildRealtimeSuggestionContext(
                snapshot(
                    Msg("对方", "有心跳"),
                    Msg("我", "那我就放心了")
                ),
                "继续聊"
            )
        )

        assertTrue(gate.shouldRequest(oldContext))
        assertTrue(gate.shouldRequest(newContext))
    }

    @Test
    fun `消息类型变化后允许再次请求且上下文保留类型`() {
        val gate = RealtimeSuggestionRequestGate()
        val textContext = requireNotNull(
            buildRealtimeSuggestionContext(snapshot(Msg("我", "是的")), "继续聊")
        )
        val voiceContext = requireNotNull(
            buildRealtimeSuggestionContext(
                snapshot(
                    Msg(
                        role = "我",
                        content = "是的",
                        messageType = ChatMessageType.VOICE_TRANSCRIPT
                    )
                ),
                "继续聊"
            )
        )

        assertEquals(
            ChatMessageType.VOICE_TRANSCRIPT,
            voiceContext.snapshot.messages.single().messageType
        )
        assertTrue(gate.shouldRequest(textContext))
        assertTrue(gate.shouldRequest(voiceContext))
    }

    @Test
    fun `空聊天记录不创建上下文`() {
        assertNull(buildRealtimeSuggestionContext(snapshot(), "正在输入"))
    }

    @Test
    fun `重置后相同上下文可再次请求`() {
        val gate = RealtimeSuggestionRequestGate()
        val context = requireNotNull(
            buildRealtimeSuggestionContext(snapshot(Msg("对方", "有心跳")), "那就好")
        )

        assertTrue(gate.shouldRequest(context))
        assertFalse(gate.shouldRequest(context))
        gate.reset()
        assertTrue(gate.shouldRequest(context))
    }

    @Test
    fun `组合key包含输入文本`() {
        val history = snapshot(Msg("对方", "今天有空吗"), Msg("我", "有空"))

        val oldKey = buildRealtimeSuggestionKey(history, "那一起吃饭吧")
        val newKey = buildRealtimeSuggestionKey(history, "那一起看电影吧")

        assertTrue(oldKey.isNotBlank())
        assertTrue(newKey.isNotBlank())
        assertFalse(oldKey == newKey)
    }

    @Test
    fun `上下文仅裁剪输入边界并保留聊天空白`() {
        val history = snapshot(Msg(" 对方 ", "  原样保留聊天空白  "))

        val context = requireNotNull(buildRealtimeSuggestionContext(history, "  正在输入  \n"))

        assertEquals("正在输入", context.inputText)
        assertEquals(" 对方 ", context.snapshot.messages.single().role)
        assertEquals("  原样保留聊天空白  ", context.snapshot.messages.single().content)
        assertEquals(
            buildRealtimeSuggestionKey(context.snapshot, context.inputText),
            context.requestKey
        )
    }

    @Test
    fun `上下文复制聊天记录形成快照并保留时间锚点`() {
        val original = Msg("对方", "第一条", "昨天 晚上8:55")
        val history = snapshot(original)

        val context = requireNotNull(buildRealtimeSuggestionContext(history, "草稿"))

        assertNotSame(original, context.snapshot.messages.single())
        assertNotSame(history, context.snapshot)
        assertEquals(
            Msg("对方", "第一条", "昨天 晚上8:55"),
            context.snapshot.messages.single()
        )
    }

    @Test
    fun `generation或标题变化即使正文草稿相同也生成不同key`() {
        val message = Msg("对方", "同一正文")
        val base = snapshot(message, generation = 1, title = "小王")
        val nextGeneration = snapshot(message, generation = 2, title = "小王")
        val otherTitle = snapshot(message, generation = 1, title = "小李")

        val baseKey = buildRealtimeSuggestionKey(base, "草稿")

        assertFalse(baseKey == buildRealtimeSuggestionKey(nextGeneration, "草稿"))
        assertFalse(baseKey == buildRealtimeSuggestionKey(otherTitle, "草稿"))
    }

    @Test
    fun `空建议不显示胶囊`() {
        assertNull(pickSuggestion(emptyList()))
        assertNull(pickSuggestion(listOf("", "   ")))
    }

    @Test
    fun `取第一条非空建议并裁剪空白`() {
        assertEquals("那我就放心了，怕你不跳了", pickSuggestion(listOf("  ", " 那我就放心了，怕你不跳了 ")))
    }

    private fun snapshot(
        vararg messages: Msg,
        generation: Long = 1L,
        title: String = "小王"
    ) = CapturedChatSnapshot(
        sessionGeneration = generation,
        sourcePackage = "com.tencent.mm",
        rawTitle = title,
        messages = messages.toList(),
        capturedAtMillis = 100L
    )
}
