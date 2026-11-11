package io.github.a982246809.replyassistant

import io.github.a982246809.replyassistant.dto.Msg
import io.github.a982246809.replyassistant.chatcontext.ChatWindowContinuity

internal interface HoldScrollScheduler {
    fun postDelayed(delayMs: Long, action: Runnable)
    fun removeCallbacks(action: Runnable)
}

internal data class CollectedChatHistory(
    val connectedMessages: Array<Msg>,
    val detachedSegments: List<Array<Msg>>
)

internal sealed interface VisibleChatMessagesRead {
    data class Ready(val messages: Array<Msg>) : VisibleChatMessagesRead
    data object Blocked : VisibleChatMessagesRead
}

internal class HoldToScrollChatHistoryController(
    private val scheduler: HoldScrollScheduler,
    private val settleDelayMs: Long = 300L,
    private val readVisibleMessages: () -> VisibleChatMessagesRead,
    private val scrollTowardHistory: () -> Boolean,
    private val scrollTowardLatest: () -> Boolean,
    private val isSessionValid: () -> Boolean = { true },
    private val requireContinuousWindows: Boolean = false,
    private val onCancelled: () -> Unit = {},
    private val onUnavailable: () -> Unit = {},
    private val onCompleted: (CollectedChatHistory) -> Unit
) {
    constructor(
        scheduler: HoldScrollScheduler,
        settleDelayMs: Long = 300L,
        collectVisibleMessages: () -> Array<Msg>,
        scrollTowardHistory: () -> Boolean,
        scrollTowardLatest: () -> Boolean,
        isSessionValid: () -> Boolean = { true },
        requireContinuousWindows: Boolean = false,
        onCancelled: () -> Unit = {},
        onCompleted: (CollectedChatHistory) -> Unit
    ) : this(
        scheduler = scheduler,
        settleDelayMs = settleDelayMs,
        readVisibleMessages = {
            VisibleChatMessagesRead.Ready(collectVisibleMessages())
        },
        scrollTowardHistory = scrollTowardHistory,
        scrollTowardLatest = scrollTowardLatest,
        isSessionValid = isSessionValid,
        requireContinuousWindows = requireContinuousWindows,
        onCancelled = onCancelled,
        onCompleted = onCompleted
    )

    private val connectedMessages = mutableListOf<Msg>()
    private val detachedSegments = mutableListOf<MutableList<Msg>>()
    private var activeSegment: MutableList<Msg> = connectedMessages
    private var successfulScrollCount = 0
    private var isRunning = false
    private var stopRequested = false
    private var isRestoring = false
    private var readUnavailable = false
    private var cancellationNotified = false
    private var pendingTask: Runnable? = null

    fun start() {
        if (isRunning) return
        isRunning = true
        cancellationNotified = false
        if (!ensureSessionValid()) return
        collectCurrentScreen(isInitialScreen = true)
    }

    fun stop() {
        if (!isRunning || stopRequested) return
        stopRequested = true
        if (pendingTask == null) {
            restoreNextStep()
        }
    }

    fun cancel() {
        if (!isRunning) return
        cancelInternal()
    }

    private fun cancelInternal() {
        pendingTask?.let(scheduler::removeCallbacks)
        pendingTask = null
        isRunning = false
        stopRequested = true
        successfulScrollCount = 0
        readUnavailable = false
        connectedMessages.clear()
        detachedSegments.clear()
        activeSegment = connectedMessages
        if (!cancellationNotified) {
            cancellationNotified = true
            onCancelled()
        }
    }

    private fun requestHistoryScroll() {
        if (!ensureSessionValid()) return
        if (stopRequested) {
            restoreNextStep()
            return
        }
        val didScroll = scrollTowardHistory()
        if (!ensureSessionValid()) return
        if (!didScroll) {
            return
        }

        successfulScrollCount += 1
        scheduleNextStep {
            collectCurrentScreen(isInitialScreen = false)
        }
    }

    private fun collectCurrentScreen(isInitialScreen: Boolean) {
        if (!ensureSessionValid()) return
        withVisibleMessages { newMessages ->
            if (requireContinuousWindows && newMessages.isEmpty()) {
                cancelInternal()
                return@withVisibleMessages
            }
            if (isInitialScreen) {
                connectedMessages.addAll(newMessages)
                activeSegment = connectedMessages
            } else {
                val overlap = ChatWindowContinuity.forwardOverlapCount(
                    newMessages,
                    activeSegment
                )
                if (requireContinuousWindows &&
                    !ChatWindowContinuity.hasForwardContinuity(newMessages, activeSegment)
                ) {
                    val detached = newMessages.toMutableList()
                    detachedSegments.add(0, detached)
                    activeSegment = detached
                } else {
                    activeSegment.addAll(0, newMessages.dropLast(overlap))
                }
            }
            if (stopRequested) {
                restoreNextStep()
            } else {
                requestHistoryScroll()
            }
        }
    }

    private fun restoreNextStep() {
        if (!ensureSessionValid() || isRestoring && pendingTask != null) return
        isRestoring = true
        if (!readUnavailable && requireContinuousWindows) {
            withVisibleMessages { visibleMessages ->
                if (!ChatWindowContinuity.hasAnyContinuity(
                        allCollectedMessages(),
                        visibleMessages
                    )) {
                    cancelInternal()
                    return@withVisibleMessages
                }
                restoreScrollStep()
            }
            return
        }
        restoreScrollStep()
    }

    private fun restoreScrollStep() {
        if (successfulScrollCount <= 0) {
            if (readUnavailable) {
                finishUnavailable()
            } else {
                complete()
            }
            return
        }
        val didScroll = scrollTowardLatest()
        if (!ensureSessionValid()) return
        if (!didScroll) {
            successfulScrollCount = 0
            if (readUnavailable) {
                finishUnavailable()
            } else {
                complete()
            }
            return
        }

        successfulScrollCount -= 1
        scheduleNextStep(action = ::restoreNextStep)
    }

    private fun withVisibleMessages(onReady: (List<Msg>) -> Unit) {
        if (!ensureSessionValid()) return
        when (val result = readVisibleMessages()) {
            is VisibleChatMessagesRead.Ready -> {
                if (!ensureSessionValid()) return
                onReady(result.messages.toList())
            }
            VisibleChatMessagesRead.Blocked -> beginUnavailableRestore()
        }
    }

    private fun beginUnavailableRestore() {
        if (readUnavailable) return
        readUnavailable = true
        stopRequested = true
        isRestoring = false
        restoreNextStep()
    }

    private fun finishUnavailable() {
        if (!isRunning) return
        isRunning = false
        pendingTask = null
        connectedMessages.clear()
        detachedSegments.clear()
        activeSegment = connectedMessages
        onUnavailable()
    }

    private fun scheduleNextStep(
        delayMs: Long = settleDelayMs,
        action: () -> Unit
    ) {
        val task = object : Runnable {
            override fun run() {
                if (pendingTask !== this) return
                pendingTask = null
                if (!ensureSessionValid()) return
                action()
            }
        }
        pendingTask = task
        scheduler.postDelayed(delayMs, task)
    }

    private fun complete() {
        if (!ensureSessionValid()) return
        isRunning = false
        pendingTask = null
        readUnavailable = false
        onCompleted(
            CollectedChatHistory(
                connectedMessages = connectedMessages.toTypedArray(),
                detachedSegments = detachedSegments.map { it.toTypedArray() }
            )
        )
    }

    private fun ensureSessionValid(): Boolean {
        if (!isRunning) return false
        if (isSessionValid()) return true
        cancelInternal()
        return false
    }

    private fun allCollectedMessages(): List<Msg> = buildList {
        detachedSegments.forEach(::addAll)
        addAll(connectedMessages)
    }

}
