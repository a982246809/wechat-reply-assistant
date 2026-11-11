package io.github.a982246809.replyassistant.chatcontext

import io.github.a982246809.replyassistant.dto.Msg
import java.util.concurrent.atomic.AtomicReference

internal data class ChatSessionIdentity(
    val sourcePackage: String,
    val normalizedTitle: String
)

internal data class ChatSessionToken(
    val generation: Long,
    val identity: ChatSessionIdentity
)

private data class AtomicChatSessionState(
    val generation: Long,
    val identity: ChatSessionIdentity?
)

internal object ChatWindowContinuity {
    private data class ContinuityMessage(
        val normalized: NormalizedMessage,
        val messageType: String?
    )

    fun hasForwardContinuity(previous: List<Msg>, current: List<Msg>): Boolean {
        val old = normalize(previous)
        val fresh = normalize(current)
        return normalizedForwardOverlapCount(old, fresh) >= requiredOverlap(old.size, fresh.size)
    }

    fun hasAnyContinuity(first: List<Msg>, second: List<Msg>): Boolean {
        val left = normalize(first)
        val right = normalize(second)
        val required = requiredOverlap(left.size, right.size)
        if (required == Int.MAX_VALUE) return false
        for (leftStart in left.indices) {
            for (rightStart in right.indices) {
                var size = 0
                while (leftStart + size < left.size &&
                    rightStart + size < right.size &&
                    left[leftStart + size] == right[rightStart + size]
                ) {
                    size += 1
                    if (size >= required) return true
                }
            }
        }
        return false
    }

    fun forwardOverlapCount(previous: List<Msg>, current: List<Msg>): Int =
        normalizedForwardOverlapCount(normalize(previous), normalize(current))

    private fun normalizedForwardOverlapCount(
        previous: List<ContinuityMessage>,
        current: List<ContinuityMessage>
    ): Int {
        for (size in minOf(previous.size, current.size) downTo 1) {
            if (previous.takeLast(size) == current.take(size)) return size
        }
        return 0
    }

    private fun requiredOverlap(firstSize: Int, secondSize: Int): Int {
        val existing = minOf(firstSize, secondSize)
        if (existing == 0) return Int.MAX_VALUE
        return if (existing >= 2) 2 else 1
    }

    private fun normalize(messages: List<Msg>): List<ContinuityMessage> =
        messages.map { message ->
            ContinuityMessage(
                normalized = ChatTextNormalizer.normalizeMessage(message),
                messageType = message.messageType
            )
        }
}

internal class ChatContextSession(
    initialTrackingEnabled: Boolean = true
) {
    private val atomicState = AtomicReference(
        AtomicChatSessionState(generation = 0L, identity = null)
    )

    private var trackingEnabled = initialTrackingEnabled
    private var activeWindow: List<Msg> = emptyList()

    val generation: Long get() = atomicState.get().generation
    val activeIdentity: ChatSessionIdentity? get() = atomicState.get().identity

    fun synchronizeIdentity(
        identity: ChatSessionIdentity?,
        forceNewGeneration: Boolean = false
    ) {
        if (identity != activeIdentity) {
            advanceGeneration(identity)
            activeWindow = emptyList()
        } else if (forceNewGeneration && identity != null) {
            advanceGeneration(identity)
            activeWindow = emptyList()
        }
    }

    fun observe(
        identity: ChatSessionIdentity?,
        forceNewGeneration: Boolean = false
    ) {
        synchronizeIdentity(identity, forceNewGeneration)
    }

    fun observe(
        snapshot: CapturedChatSnapshot?,
        forceNewGeneration: Boolean = false
    ) {
        val identity = snapshot?.let {
            ChatSessionIdentity(it.sourcePackage, it.normalizedTitle)
        }
        if (forceNewGeneration &&
            identity != null &&
            identity == activeIdentity
        ) {
            advanceGeneration(identity)
            activeWindow = emptyList()
        }
        if (identity != activeIdentity) {
            advanceGeneration(identity)
        }
        snapshot?.let { accumulate(it.copy(sessionGeneration = generation)) }
    }

    fun setTrackingEnabled(enabled: Boolean) {
        if (trackingEnabled == enabled) return
        trackingEnabled = enabled
        if (!enabled) activeWindow = emptyList()
    }

    fun pauseCapture() {
        setTrackingEnabled(false)
    }

    fun resumeCapture() {
        setTrackingEnabled(true)
    }

    fun invalidate() {
        advanceGeneration(null)
        activeWindow = emptyList()
    }

    fun invalidateAndDiscardPending() {
        advanceGeneration(nextIdentity = null)
        activeWindow = emptyList()
    }

    suspend fun shutdown() = Unit

    fun isCurrent(
        expectedGeneration: Long,
        expectedIdentity: ChatSessionIdentity
    ): Boolean {
        val current = atomicState.get()
        return current.generation == expectedGeneration && current.identity == expectedIdentity
    }

    fun tokenFor(snapshot: CapturedChatSnapshot): ChatSessionToken? {
        val identity = ChatSessionIdentity(
            snapshot.sourcePackage,
            snapshot.normalizedTitle
        )
        val current = atomicState.get()
        return ChatSessionToken(snapshot.sessionGeneration, identity).takeIf {
            current.generation == it.generation && current.identity == it.identity
        }
    }

    fun isTokenCurrent(token: ChatSessionToken): Boolean {
        val current = atomicState.get()
        return current.generation == token.generation && current.identity == token.identity
    }

    fun isCurrent(
        expected: CapturedChatSnapshot,
        observed: CapturedChatSnapshot
    ): Boolean {
        val expectedIdentity = ChatSessionIdentity(
            expected.sourcePackage,
            expected.normalizedTitle
        )
        val observedIdentity = ChatSessionIdentity(
            observed.sourcePackage,
            observed.normalizedTitle
        )
        return isCurrent(expected.sessionGeneration, expectedIdentity) &&
            observedIdentity == expectedIdentity &&
            ChatWindowContinuity.hasForwardContinuity(expected.messages, observed.messages)
    }

    private fun advanceGeneration(nextIdentity: ChatSessionIdentity?) {
        atomicState.updateAndGet { current ->
            AtomicChatSessionState(current.generation + 1, nextIdentity)
        }
    }

    private fun accumulate(snapshot: CapturedChatSnapshot) {
        if (!trackingEnabled || snapshot.messages.isEmpty()) return
        val capturableSnapshot = snapshot.forCapture()
        if (capturableSnapshot.messages.isEmpty()) return
        val current = atomicState.get()
        val identity = ChatSessionIdentity(
            capturableSnapshot.sourcePackage,
            capturableSnapshot.normalizedTitle
        )
        if (current.identity != identity) return
        activeWindow = capturableSnapshot.messages.map { it.copy() }
    }
}
