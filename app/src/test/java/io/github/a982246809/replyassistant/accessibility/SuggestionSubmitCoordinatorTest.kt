package io.github.a982246809.replyassistant.accessibility

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SuggestionSubmitCoordinatorTest {

    @Test
    fun `ready send completes immediately and records use once`() = runTest {
        val port = FakeSuggestionSubmitPort(inputText = "")
        val coordinator = SuggestionSubmitCoordinator()
        var usageCount = 0

        val result = coordinator.submit(
            request = SuggestionSubmitRequest(
                segments = listOf("你好"),
                send = true,
                expectedInputText = ""
            ),
            port = port,
            onUsed = { usageCount += 1 }
        )

        assertEquals(SuggestionSubmitResult.Completed, result)
        assertEquals(0L, testScheduler.currentTime)
        assertEquals(listOf("你好"), port.writtenTexts)
        assertEquals(1, port.clickCount)
        assertEquals(1, usageCount)
    }

    @Test
    fun `send waits until button becomes ready`() = runTest {
        val port = FakeSuggestionSubmitPort(
            inputText = "",
            sendButtonReady = false,
            sendReadyAfterChecks = 3
        )
        val coordinator = SuggestionSubmitCoordinator()

        val result = coordinator.submit(
            request = SuggestionSubmitRequest(
                segments = listOf("稍后发送"),
                send = true,
                expectedInputText = ""
            ),
            port = port,
            onUsed = {}
        )

        assertEquals(SuggestionSubmitResult.Completed, result)
        assertEquals(100L, testScheduler.currentTime)
        assertEquals(1, port.clickCount)
    }

    @Test
    fun `send waits until input becomes available`() = runTest {
        val port = FakeSuggestionSubmitPort(
            inputText = "",
            inputAvailableAfterReads = 3
        )
        val coordinator = SuggestionSubmitCoordinator()

        val result = coordinator.submit(
            request = SuggestionSubmitRequest(
                segments = listOf("等待输入框"),
                send = true,
                expectedInputText = ""
            ),
            port = port,
            onUsed = {}
        )

        assertEquals(SuggestionSubmitResult.Completed, result)
        assertEquals(100L, testScheduler.currentTime)
        assertEquals(listOf("等待输入框"), port.writtenTexts)
        assertEquals(1, port.clickCount)
    }

    @Test
    fun `segmented send waits for each cleared input and records use once`() = runTest {
        val port = FakeSuggestionSubmitPort(inputText = "")
        val coordinator = SuggestionSubmitCoordinator()
        var usageCount = 0

        val result = coordinator.submit(
            request = SuggestionSubmitRequest(
                segments = listOf("第一段", "第二段"),
                send = true,
                expectedInputText = ""
            ),
            port = port,
            onUsed = { usageCount += 1 }
        )

        assertEquals(SuggestionSubmitResult.Completed, result)
        assertEquals(listOf("第一段", "第二段"), port.writtenTexts)
        assertEquals(2, port.clickCount)
        assertEquals(1, usageCount)
    }

    @Test
    fun `concurrent submit is rejected as busy`() = runTest {
        val coordinator = SuggestionSubmitCoordinator()
        val waitingPort = FakeSuggestionSubmitPort(
            inputText = "",
            sendButtonReady = false
        )
        val first = async {
            coordinator.submit(
                request = SuggestionSubmitRequest(
                    segments = listOf("第一条"),
                    send = true,
                    expectedInputText = ""
                ),
                port = waitingPort,
                onUsed = {}
            )
        }
        runCurrent()

        val second = coordinator.submit(
            request = SuggestionSubmitRequest(
                segments = listOf("第二条"),
                send = true,
                expectedInputText = ""
            ),
            port = FakeSuggestionSubmitPort(inputText = ""),
            onUsed = {}
        )

        assertEquals(SuggestionSubmitResult.Busy, second)
        waitingPort.sendButtonReady = true
        advanceUntilIdle()
        assertEquals(SuggestionSubmitResult.Completed, first.await())
    }

    @Test
    fun `insert waits until written text is observable before recording use`() = runTest {
        val port = FakeSuggestionSubmitPort(
            inputText = "原输入",
            applyWrittenTextAfterReads = 3
        )
        val coordinator = SuggestionSubmitCoordinator()
        var usageCount = 0

        val result = coordinator.submit(
            request = SuggestionSubmitRequest(
                segments = listOf("建议文本"),
                send = false,
                expectedInputText = "原输入"
            ),
            port = port,
            onUsed = { usageCount += 1 }
        )

        assertEquals(SuggestionSubmitResult.Completed, result)
        assertEquals(100L, testScheduler.currentTime)
        assertEquals("建议文本", port.inputText)
        assertEquals(0, port.clickCount)
        assertEquals(1, usageCount)
    }

    @Test
    fun `send timeout preserves written text without clicking or recording use`() = runTest {
        val port = FakeSuggestionSubmitPort(
            inputText = "",
            sendButtonReady = false
        )
        val coordinator = SuggestionSubmitCoordinator()
        var usageCount = 0

        val result = coordinator.submit(
            request = SuggestionSubmitRequest(
                segments = listOf("请手动发送"),
                send = true,
                expectedInputText = ""
            ),
            port = port,
            onUsed = { usageCount += 1 }
        )

        assertEquals(
            SuggestionSubmitResult.Failed(
                reason = SuggestionSubmitFailure.SEND_BUTTON_UNAVAILABLE,
                textWritten = true
            ),
            result
        )
        assertEquals(1_500L, testScheduler.currentTime)
        assertEquals("请手动发送", port.inputText)
        assertEquals(0, port.clickCount)
        assertEquals(0, usageCount)
    }

    @Test
    fun `input timeout stops before writing or recording use`() = runTest {
        val port = FakeSuggestionSubmitPort(
            inputText = "",
            inputAvailableAfterReads = Int.MAX_VALUE
        )
        val coordinator = SuggestionSubmitCoordinator()
        var usageCount = 0

        val result = coordinator.submit(
            request = SuggestionSubmitRequest(
                segments = listOf("不会写入"),
                send = true,
                expectedInputText = ""
            ),
            port = port,
            onUsed = { usageCount += 1 }
        )

        assertEquals(
            SuggestionSubmitResult.Failed(
                reason = SuggestionSubmitFailure.INPUT_UNAVAILABLE,
                textWritten = false
            ),
            result
        )
        assertEquals(1_500L, testScheduler.currentTime)
        assertEquals(emptyList<String>(), port.writtenTexts)
        assertEquals(0, port.clickCount)
        assertEquals(0, usageCount)
    }

    @Test
    fun `user input change stops pending send without overwriting again`() = runTest {
        val port = FakeSuggestionSubmitPort(
            inputText = "原输入",
            sendButtonReady = false
        )
        val coordinator = SuggestionSubmitCoordinator()
        val submission = async {
            coordinator.submit(
                request = SuggestionSubmitRequest(
                    segments = listOf("建议文本"),
                    send = true,
                    expectedInputText = "原输入"
                ),
                port = port,
                onUsed = {}
            )
        }
        runCurrent()

        port.inputText = "用户新输入"
        advanceUntilIdle()

        assertEquals(
            SuggestionSubmitResult.Failed(
                reason = SuggestionSubmitFailure.INPUT_CHANGED,
                textWritten = true
            ),
            submission.await()
        )
        assertEquals(listOf("建议文本"), port.writtenTexts)
        assertEquals("用户新输入", port.inputText)
        assertEquals(0, port.clickCount)
    }

    @Test
    fun `chat context change cancels pending send`() = runTest {
        val port = FakeSuggestionSubmitPort(
            inputText = "",
            sendButtonReady = false
        )
        val coordinator = SuggestionSubmitCoordinator()
        val submission = async {
            coordinator.submit(
                request = SuggestionSubmitRequest(
                    segments = listOf("建议文本"),
                    send = true,
                    expectedInputText = ""
                ),
                port = port,
                onUsed = {}
            )
        }
        runCurrent()

        port.contextCurrent = false
        advanceUntilIdle()

        assertEquals(SuggestionSubmitResult.Cancelled, submission.await())
        assertEquals(0, port.clickCount)
    }

    @Test
    fun `segmented send waits for delayed clear before writing next segment`() = runTest {
        val port = FakeSuggestionSubmitPort(
            inputText = "",
            clearInputOnClick = false
        )
        val coordinator = SuggestionSubmitCoordinator()
        val submission = async {
            coordinator.submit(
                request = SuggestionSubmitRequest(
                    segments = listOf("第一段", "第二段"),
                    send = true,
                    expectedInputText = ""
                ),
                port = port,
                onUsed = {}
            )
        }
        runCurrent()

        assertEquals(listOf("第一段"), port.writtenTexts)
        port.inputText = ""
        advanceUntilIdle()

        assertEquals(SuggestionSubmitResult.Completed, submission.await())
        assertEquals(listOf("第一段", "第二段"), port.writtenTexts)
        assertEquals(2, port.clickCount)
    }

    @Test
    fun `segmented send timeout stops before overwriting unconfirmed first segment`() = runTest {
        val port = FakeSuggestionSubmitPort(
            inputText = "",
            clearInputOnClick = false
        )
        val coordinator = SuggestionSubmitCoordinator()
        var usageCount = 0

        val result = coordinator.submit(
            request = SuggestionSubmitRequest(
                segments = listOf("第一段", "第二段"),
                send = true,
                expectedInputText = ""
            ),
            port = port,
            onUsed = { usageCount += 1 }
        )

        assertEquals(
            SuggestionSubmitResult.Failed(
                reason = SuggestionSubmitFailure.SEND_COMPLETION_NOT_CONFIRMED,
                textWritten = true
            ),
            result
        )
        assertEquals(2_500L, testScheduler.currentTime)
        assertEquals(listOf("第一段"), port.writtenTexts)
        assertEquals("第一段", port.inputText)
        assertEquals(1, port.clickCount)
        assertEquals(1, usageCount)
    }

    private class FakeSuggestionSubmitPort(
        var inputText: String?,
        var contextCurrent: Boolean = true,
        var sendButtonReady: Boolean = true,
        private val sendReadyAfterChecks: Int? = null,
        private val inputAvailableAfterReads: Int? = null,
        private val applyWrittenTextAfterReads: Int? = null,
        private val clearInputOnClick: Boolean = true
    ) : SuggestionSubmitPort {
        val writtenTexts = mutableListOf<String>()
        var clickCount = 0
        private var sendReadyChecks = 0
        private var inputReadCount = 0
        private var readsAfterSet = 0
        private var pendingText: String? = null

        override fun isContextCurrent(): Boolean = contextCurrent

        override fun readInputText(): String? {
            inputReadCount += 1
            if (inputAvailableAfterReads?.let { inputReadCount < it } == true) return null
            pendingText?.let { text ->
                readsAfterSet += 1
                if (applyWrittenTextAfterReads?.let { readsAfterSet >= it } == true) {
                    inputText = text
                    pendingText = null
                }
            }
            return inputText
        }

        override fun setInputText(text: String): Boolean {
            writtenTexts += text
            if (applyWrittenTextAfterReads == null) {
                inputText = text
            } else {
                pendingText = text
                readsAfterSet = 0
            }
            return true
        }

        override fun isSendButtonReady(): Boolean {
            sendReadyChecks += 1
            return sendButtonReady || sendReadyAfterChecks?.let { sendReadyChecks >= it } == true
        }

        override fun clickSendButton(): Boolean {
            clickCount += 1
            if (clearInputOnClick) inputText = ""
            return true
        }
    }
}
