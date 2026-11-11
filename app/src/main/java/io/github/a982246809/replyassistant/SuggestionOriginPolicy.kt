package io.github.a982246809.replyassistant

import io.github.a982246809.replyassistant.chatcontext.CapturedChatSnapshot
import io.github.a982246809.replyassistant.chatcontext.ChatTextNormalizer

internal data class SuggestionOrigin(
    val sessionGeneration: Long,
    val sourcePackage: String,
    val normalizedTitle: String,
    val frame: SuggestionFrameOrigin? = null
)

internal data class SuggestionFrameOrigin(
    val windowId: Int,
    val fingerprint: String,
    val viewportEpoch: Long
)

internal fun CapturedChatSnapshot.toSuggestionOrigin(): SuggestionOrigin = SuggestionOrigin(
    sessionGeneration = sessionGeneration,
    sourcePackage = sourcePackage,
    normalizedTitle = normalizedTitle
)

internal fun isSuggestionOriginCurrent(
    origin: SuggestionOrigin,
    currentGeneration: Long,
    currentPackageName: String?,
    currentRawTitle: String?
): Boolean {
    return origin.sessionGeneration == currentGeneration &&
        origin.sourcePackage == currentPackageName &&
        origin.normalizedTitle == ChatTextNormalizer.normalizeTitle(currentRawTitle.orEmpty())
}

internal fun isSuggestionFrameCurrent(
    origin: SuggestionOrigin,
    currentWindowId: Int?,
    currentFingerprint: String?,
    currentViewportEpoch: Long,
    currentPageIsTextReady: Boolean
): Boolean {
    val expected = origin.frame ?: return currentPageIsTextReady
    return currentWindowId == expected.windowId &&
        currentFingerprint == expected.fingerprint &&
        currentViewportEpoch == expected.viewportEpoch
}
