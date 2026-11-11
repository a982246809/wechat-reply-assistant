package io.github.a982246809.replyassistant.accessibility

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex

internal data class SuggestionSubmitRequest(
    val segments: List<String>,
    val send: Boolean,
    val expectedInputText: String
)

internal interface SuggestionSubmitPort {
    fun isContextCurrent(): Boolean
    fun readInputText(): String?
    fun setInputText(text: String): Boolean
    fun isSendButtonReady(): Boolean
    fun clickSendButton(): Boolean
}

internal enum class SuggestionSubmitFailure {
    EMPTY_TEXT,
    INPUT_UNAVAILABLE,
    INPUT_CHANGED,
    INPUT_WRITE_FAILED,
    TEXT_NOT_CONFIRMED,
    SEND_BUTTON_UNAVAILABLE,
    SEND_ACTION_FAILED,
    SEND_COMPLETION_NOT_CONFIRMED
}

internal sealed interface SuggestionSubmitResult {
    data object Completed : SuggestionSubmitResult
    data object Busy : SuggestionSubmitResult
    data object Cancelled : SuggestionSubmitResult
    data class Failed(
        val reason: SuggestionSubmitFailure,
        val textWritten: Boolean
    ) : SuggestionSubmitResult
}

internal class SuggestionSubmitCoordinator {
    private val submissionMutex = Mutex()

    private enum class InputReadiness {
        READY,
        CANCELLED,
        INPUT_CHANGED,
        TIMED_OUT
    }

    private enum class SendReadiness {
        READY,
        CANCELLED,
        INPUT_CHANGED,
        TIMED_OUT
    }

    suspend fun submit(
        request: SuggestionSubmitRequest,
        port: SuggestionSubmitPort,
        onUsed: () -> Unit = {}
    ): SuggestionSubmitResult {
        if (!submissionMutex.tryLock()) return SuggestionSubmitResult.Busy
        return try {
            submitLocked(request, port, onUsed)
        } finally {
            submissionMutex.unlock()
        }
    }

    private suspend fun submitLocked(
        request: SuggestionSubmitRequest,
        port: SuggestionSubmitPort,
        onUsed: () -> Unit
    ): SuggestionSubmitResult {
        val segments = request.segments.filter { it.isNotBlank() }
        if (segments.isEmpty()) {
            return SuggestionSubmitResult.Failed(
                reason = SuggestionSubmitFailure.EMPTY_TEXT,
                textWritten = false
            )
        }
        when (waitForInput(request.expectedInputText, port)) {
            InputReadiness.READY -> Unit
            InputReadiness.CANCELLED -> return SuggestionSubmitResult.Cancelled
            InputReadiness.INPUT_CHANGED -> {
                return SuggestionSubmitResult.Failed(
                    reason = SuggestionSubmitFailure.INPUT_CHANGED,
                    textWritten = false
                )
            }
            InputReadiness.TIMED_OUT -> {
                return SuggestionSubmitResult.Failed(
                    reason = SuggestionSubmitFailure.INPUT_UNAVAILABLE,
                    textWritten = false
                )
            }
        }
        var usageRecorded = false
        segments.forEachIndexed { index, text ->
            if (!port.setInputText(text)) {
                return SuggestionSubmitResult.Failed(
                    reason = SuggestionSubmitFailure.INPUT_WRITE_FAILED,
                    textWritten = index > 0
                )
            }
            if (!request.send) {
                return when (waitForWrittenText(request.expectedInputText, text, port)) {
                    SendReadiness.READY -> {
                        onUsed()
                        SuggestionSubmitResult.Completed
                    }
                    SendReadiness.CANCELLED -> SuggestionSubmitResult.Cancelled
                    SendReadiness.INPUT_CHANGED -> SuggestionSubmitResult.Failed(
                        reason = SuggestionSubmitFailure.INPUT_CHANGED,
                        textWritten = true
                    )
                    SendReadiness.TIMED_OUT -> SuggestionSubmitResult.Failed(
                        reason = SuggestionSubmitFailure.TEXT_NOT_CONFIRMED,
                        textWritten = true
                    )
                }
            }
            val originalInput = if (index == 0) request.expectedInputText else ""
            when (waitForSendButton(originalInput, text, port)) {
                SendReadiness.READY -> Unit
                SendReadiness.CANCELLED -> return SuggestionSubmitResult.Cancelled
                SendReadiness.INPUT_CHANGED -> {
                    return SuggestionSubmitResult.Failed(
                        reason = SuggestionSubmitFailure.INPUT_CHANGED,
                        textWritten = true
                    )
                }
                SendReadiness.TIMED_OUT -> {
                    return SuggestionSubmitResult.Failed(
                        reason = SuggestionSubmitFailure.SEND_BUTTON_UNAVAILABLE,
                        textWritten = true
                    )
                }
            }
            if (!port.clickSendButton()) {
                return SuggestionSubmitResult.Failed(
                    reason = SuggestionSubmitFailure.SEND_ACTION_FAILED,
                    textWritten = true
                )
            }
            if (!usageRecorded) {
                usageRecorded = true
                onUsed()
            }
            if (index < segments.lastIndex) {
                when (waitForSentInputToClear(text, port)) {
                    InputReadiness.READY -> Unit
                    InputReadiness.CANCELLED -> return SuggestionSubmitResult.Cancelled
                    InputReadiness.INPUT_CHANGED -> {
                        return SuggestionSubmitResult.Failed(
                            reason = SuggestionSubmitFailure.INPUT_CHANGED,
                            textWritten = false
                        )
                    }
                    InputReadiness.TIMED_OUT -> {
                        return SuggestionSubmitResult.Failed(
                            reason = SuggestionSubmitFailure.SEND_COMPLETION_NOT_CONFIRMED,
                            textWritten = true
                        )
                    }
                }
            }
        }
        return SuggestionSubmitResult.Completed
    }

    private suspend fun waitForInput(
        expectedInput: String,
        port: SuggestionSubmitPort
    ): InputReadiness {
        var elapsedMs = 0L
        while (elapsedMs <= INPUT_READY_TIMEOUT_MS) {
            if (!port.isContextCurrent()) return InputReadiness.CANCELLED
            when (val currentInput = port.readInputText()) {
                expectedInput -> return InputReadiness.READY
                null -> Unit
                else -> if (currentInput != expectedInput) return InputReadiness.INPUT_CHANGED
            }
            if (elapsedMs == INPUT_READY_TIMEOUT_MS) break
            val waitMs = minOf(POLL_INTERVAL_MS, INPUT_READY_TIMEOUT_MS - elapsedMs)
            delay(waitMs)
            elapsedMs += waitMs
        }
        return InputReadiness.TIMED_OUT
    }

    private suspend fun waitForWrittenText(
        originalInput: String,
        targetText: String,
        port: SuggestionSubmitPort
    ): SendReadiness {
        var elapsedMs = 0L
        while (elapsedMs <= SEND_READY_TIMEOUT_MS) {
            if (!port.isContextCurrent()) return SendReadiness.CANCELLED
            val currentInput = port.readInputText()
            if (currentInput == targetText) return SendReadiness.READY
            if (currentInput != null && currentInput != originalInput) {
                return SendReadiness.INPUT_CHANGED
            }
            if (elapsedMs == SEND_READY_TIMEOUT_MS) break
            val waitMs = minOf(POLL_INTERVAL_MS, SEND_READY_TIMEOUT_MS - elapsedMs)
            delay(waitMs)
            elapsedMs += waitMs
        }
        return SendReadiness.TIMED_OUT
    }

    private suspend fun waitForSentInputToClear(
        sentText: String,
        port: SuggestionSubmitPort
    ): InputReadiness {
        var elapsedMs = 0L
        while (elapsedMs <= SEND_COMPLETION_TIMEOUT_MS) {
            if (!port.isContextCurrent()) return InputReadiness.CANCELLED
            when (val currentInput = port.readInputText()) {
                "" -> return InputReadiness.READY
                null, sentText -> Unit
                else -> if (currentInput != sentText) return InputReadiness.INPUT_CHANGED
            }
            if (elapsedMs == SEND_COMPLETION_TIMEOUT_MS) break
            val waitMs = minOf(POLL_INTERVAL_MS, SEND_COMPLETION_TIMEOUT_MS - elapsedMs)
            delay(waitMs)
            elapsedMs += waitMs
        }
        return InputReadiness.TIMED_OUT
    }

    private suspend fun waitForSendButton(
        originalInput: String,
        targetText: String,
        port: SuggestionSubmitPort
    ): SendReadiness {
        var elapsedMs = 0L
        while (elapsedMs <= SEND_READY_TIMEOUT_MS) {
            if (!port.isContextCurrent()) return SendReadiness.CANCELLED
            val currentInput = port.readInputText()
            if (currentInput != null && currentInput != originalInput && currentInput != targetText) {
                return SendReadiness.INPUT_CHANGED
            }
            if (currentInput == targetText && port.isSendButtonReady()) {
                return SendReadiness.READY
            }
            if (elapsedMs == SEND_READY_TIMEOUT_MS) break
            val waitMs = minOf(POLL_INTERVAL_MS, SEND_READY_TIMEOUT_MS - elapsedMs)
            delay(waitMs)
            elapsedMs += waitMs
        }
        return SendReadiness.TIMED_OUT
    }

    private companion object {
        const val POLL_INTERVAL_MS = 50L
        const val INPUT_READY_TIMEOUT_MS = 1_500L
        const val SEND_READY_TIMEOUT_MS = 1_500L
        const val SEND_COMPLETION_TIMEOUT_MS = 2_500L
    }
}
