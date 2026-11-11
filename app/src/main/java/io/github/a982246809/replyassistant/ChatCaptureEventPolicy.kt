package io.github.a982246809.replyassistant

import android.view.accessibility.AccessibilityEvent

internal const val WECHAT_ACCESSIBILITY_PACKAGE = "com.tencent.mm"

internal val accessibilityRefreshEventTypes: Int =
    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
        AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
        AccessibilityEvent.TYPE_VIEW_FOCUSED or
        AccessibilityEvent.TYPE_VIEW_CLICKED or
        AccessibilityEvent.TYPE_VIEW_SCROLLED or
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
        AccessibilityEvent.TYPE_WINDOWS_CHANGED

internal fun shouldRefreshChatCapture(eventType: Int): Boolean = when (eventType) {
    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
    AccessibilityEvent.TYPE_VIEW_FOCUSED,
    AccessibilityEvent.TYPE_VIEW_CLICKED,
    AccessibilityEvent.TYPE_VIEW_SCROLLED,
    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
    AccessibilityEvent.TYPE_WINDOWS_CHANGED -> true
    else -> false
}

internal fun shouldRefreshAccessibilityState(
    eventType: Int,
    eventPackageName: String?,
    ownPackageName: String
): Boolean {
    if (!shouldRefreshChatCapture(eventType)) return false
    if (eventPackageName == ownPackageName) {
        return eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
    }
    if (eventPackageName == WECHAT_ACCESSIBILITY_PACKAGE) return true
    return eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
        eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED
}

internal fun isWeChatNavigationBoundaryEvent(
    eventType: Int,
    eventPackageName: String?
): Boolean {
    if (eventPackageName != WECHAT_ACCESSIBILITY_PACKAGE) return false
    return eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
        eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
        eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED
}
