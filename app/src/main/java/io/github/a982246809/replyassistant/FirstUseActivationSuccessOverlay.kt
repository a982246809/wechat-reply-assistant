package io.github.a982246809.replyassistant

import io.github.a982246809.replyassistant.data.datastore.FirstUseGuidePreferences

private const val FIRST_USE_ACTIVATION_SUCCESS_DURATION_MILLIS = 8_000L

internal fun shouldShowFirstUseActivationSuccess(
    preferences: FirstUseGuidePreferences
): Boolean = preferences.activationRequested && !preferences.completed

internal interface FirstUseActivationSuccessOverlayHost {
    fun show(): Boolean
    fun hide()
}

internal interface FirstUseActivationSuccessOverlayScheduler {
    fun postDelayed(action: Runnable, delayMillis: Long)
    fun removeCallbacks(action: Runnable)
}

internal class FirstUseActivationSuccessOverlay(
    private val host: FirstUseActivationSuccessOverlayHost,
    private val scheduler: FirstUseActivationSuccessOverlayScheduler
) {
    private var visible = false
    private var shown = false
    private val hideAction = Runnable(::hide)

    fun show() {
        if (shown || !host.show()) return

        shown = true
        visible = true
        scheduler.postDelayed(hideAction, FIRST_USE_ACTIVATION_SUCCESS_DURATION_MILLIS)
    }

    fun hide() {
        scheduler.removeCallbacks(hideAction)
        if (!visible) return

        visible = false
        host.hide()
    }
}
