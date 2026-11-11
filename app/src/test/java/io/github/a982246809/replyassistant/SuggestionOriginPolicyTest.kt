package io.github.a982246809.replyassistant

import io.github.a982246809.replyassistant.dto.Msg
import io.github.a982246809.replyassistant.chatcontext.CapturedChatSnapshot
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionOriginPolicyTest {
    private val snapshot = CapturedChatSnapshot(
        sessionGeneration = 7L,
        sourcePackage = WECHAT_ACCESSIBILITY_PACKAGE,
        rawTitle = " Alice ",
        messages = listOf(Msg(role = "friend", content = "hi")),
        capturedAtMillis = 1L
    )

    @Test
    fun `origin matches only the chat and generation that produced it`() {
        val origin = snapshot.toSuggestionOrigin()

        assertTrue(
            isSuggestionOriginCurrent(
                origin = origin,
                currentGeneration = 7L,
                currentPackageName = WECHAT_ACCESSIBILITY_PACKAGE,
                currentRawTitle = "Alice"
            )
        )
        assertFalse(
            isSuggestionOriginCurrent(
                origin = origin,
                currentGeneration = 7L,
                currentPackageName = WECHAT_ACCESSIBILITY_PACKAGE,
                currentRawTitle = "Bob"
            )
        )
        assertFalse(
            isSuggestionOriginCurrent(
                origin = origin,
                currentGeneration = 8L,
                currentPackageName = WECHAT_ACCESSIBILITY_PACKAGE,
                currentRawTitle = "Alice"
            )
        )
        assertFalse(
            isSuggestionOriginCurrent(
                origin = origin,
                currentGeneration = 7L,
                currentPackageName = "other.package",
                currentRawTitle = "Alice"
            )
        )
    }

    @Test
    fun `text suggestion still requires a text ready page`() {
        val origin = snapshot.toSuggestionOrigin()

        assertTrue(isSuggestionFrameCurrent(origin, null, null, 4, true))
        assertFalse(isSuggestionFrameCurrent(origin, null, null, 4, false))
    }

    @Test
    fun `visual suggestion remains usable only on its exact viewport frame`() {
        val origin = snapshot.toSuggestionOrigin().copy(
            frame = SuggestionFrameOrigin(
                windowId = 3,
                fingerprint = "visual-frame",
                viewportEpoch = 9
            )
        )

        assertTrue(isSuggestionFrameCurrent(origin, 3, "visual-frame", 9, false))
        assertFalse(isSuggestionFrameCurrent(origin, 4, "visual-frame", 9, false))
        assertFalse(isSuggestionFrameCurrent(origin, 3, "changed", 9, false))
        assertFalse(isSuggestionFrameCurrent(origin, 3, "visual-frame", 10, false))
    }
}
