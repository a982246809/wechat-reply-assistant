package io.github.a982246809.replyassistant

import io.github.a982246809.replyassistant.dto.Msg
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionTriggerStateTest {

    @Test
    fun `对方新增消息只触发一次自动列表`() {
        val state = SuggestionTriggerState()
        val initial = listOf(Msg("自己", "你好"), Msg("对方", "在的"), Msg("自己", "什么事"))
        val appended = initial.drop(1) + Msg("对方", "你说")
        state.update("张三", keyboardVisible = false, hasEditableInput = true, messages = initial)

        val received = state.update(
            "张三",
            keyboardVisible = false,
            hasEditableInput = true,
            messages = appended
        )
        val unchanged = state.update(
            "张三",
            keyboardVisible = false,
            hasEditableInput = true,
            messages = appended
        )

        assertTrue(received.incomingMessageReceived)
        assertFalse(unchanged.incomingMessageReceived)
    }

    @Test
    fun `自己新增消息不触发自动列表`() {
        val state = SuggestionTriggerState()
        val initial = listOf(Msg("对方", "你好"))
        state.update("张三", keyboardVisible = false, hasEditableInput = true, messages = initial)

        val sent = state.update(
            "张三",
            keyboardVisible = false,
            hasEditableInput = true,
            messages = initial + Msg("自己", "你好")
        )

        assertFalse(sent.incomingMessageReceived)
    }

    @Test
    fun `向后滚动到不连续历史不触发自动列表`() {
        val state = SuggestionTriggerState()
        state.update(
            "张三",
            keyboardVisible = false,
            hasEditableInput = true,
            messages = listOf(Msg("对方", "最近消息"))
        )

        val history = state.update(
            "张三",
            keyboardVisible = false,
            hasEditableInput = true,
            messages = listOf(Msg("对方", "更早消息"))
        )

        assertFalse(history.incomingMessageReceived)
    }

    @Test
    fun `synchronizing current chat prevents duplicate enter action`() {
        val state = SuggestionTriggerState()

        state.synchronizeChat(" Zhang San ")
        val refreshed = state.update("Zhang San", keyboardVisible = false, hasEditableInput = true)

        assertFalse(refreshed.chatEntered)
    }

    @Test
    fun `blank synchronization does not clear current chat`() {
        val state = SuggestionTriggerState()
        state.update("Zhang San", keyboardVisible = false, hasEditableInput = true)

        state.synchronizeChat("   ")
        val refreshed = state.update("Zhang San", keyboardVisible = false, hasEditableInput = true)

        assertFalse(refreshed.chatEntered)
    }

    @Test
    fun `synchronizing chat does not overwrite keyboard state`() {
        val state = SuggestionTriggerState()
        state.update("Zhang San", keyboardVisible = true, hasEditableInput = true)

        state.synchronizeChat("Zhang San")
        val refreshed = state.update("Zhang San", keyboardVisible = true, hasEditableInput = true)

        assertFalse(refreshed.keyboardOpened)
    }

    @Test
    fun `进入聊天页只触发一次自动列表`() {
        val state = SuggestionTriggerState()

        val entered = state.update("张三", keyboardVisible = false, hasEditableInput = true)
        val unchanged = state.update("张三", keyboardVisible = false, hasEditableInput = true)

        assertTrue(entered.chatEntered)
        assertFalse(unchanged.chatEntered)
    }

    @Test
    fun `切换聊天和退出后重进都会触发自动列表`() {
        val state = SuggestionTriggerState()
        state.update("张三", keyboardVisible = false, hasEditableInput = true)

        val switched = state.update("李四", keyboardVisible = false, hasEditableInput = true)
        val exited = state.update(null, keyboardVisible = false, hasEditableInput = false)
        val reentered = state.update("李四", keyboardVisible = false, hasEditableInput = true)

        assertTrue(switched.chatEntered)
        assertTrue(exited.chatExited)
        assertTrue(reentered.chatEntered)
    }

    @Test
    fun `输入法从关闭到打开时只触发一次气泡`() {
        val state = SuggestionTriggerState()
        state.update("张三", keyboardVisible = false, hasEditableInput = true)

        val opened = state.update("张三", keyboardVisible = true, hasEditableInput = true)
        val unchanged = state.update("张三", keyboardVisible = true, hasEditableInput = true)
        state.update("张三", keyboardVisible = false, hasEditableInput = true)
        val reopened = state.update("张三", keyboardVisible = true, hasEditableInput = true)

        assertTrue(opened.keyboardOpened)
        assertFalse(unchanged.keyboardOpened)
        assertTrue(reopened.keyboardOpened)
    }

    @Test
    fun `非聊天页或没有可编辑输入框时不触发气泡`() {
        val state = SuggestionTriggerState()

        val outsideChat = state.update(null, keyboardVisible = true, hasEditableInput = false)
        val noInput = state.update("张三", keyboardVisible = true, hasEditableInput = false)
        val inputReady = state.update("张三", keyboardVisible = true, hasEditableInput = true)

        assertFalse(outsideChat.keyboardOpened)
        assertFalse(noInput.keyboardOpened)
        assertTrue(inputReady.keyboardOpened)
    }
}
