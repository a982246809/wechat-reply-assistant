package io.github.a982246809.replyassistant

internal interface AccessibilityRefreshScheduler {
    fun postDelayed(delayMs: Long, action: Runnable)
    fun removeCallbacks(action: Runnable)
}

internal class AccessibilityRefreshCoordinator(
    private val scheduler: AccessibilityRefreshScheduler,
    private val refreshWindowMs: Long = 100L,
    private val onRefresh: (forceNavigationBoundary: Boolean) -> Unit
) {
    private var pauseDepth = 0
    private var refreshRequested = false
    private var forceNavigationBoundary = false
    private var refreshScheduled = false

    private val refreshAction = Runnable {
        refreshScheduled = false
        if (pauseDepth > 0 || !refreshRequested) return@Runnable

        val forceBoundary = forceNavigationBoundary
        refreshRequested = false
        forceNavigationBoundary = false
        onRefresh(forceBoundary)
    }

    fun request(forceNavigationBoundary: Boolean = false) {
        refreshRequested = true
        this.forceNavigationBoundary =
            this.forceNavigationBoundary || forceNavigationBoundary
        scheduleIfReady()
    }

    fun pause() {
        pauseDepth += 1
        if (refreshScheduled) {
            scheduler.removeCallbacks(refreshAction)
            refreshScheduled = false
        }
    }

    fun resume() {
        if (pauseDepth == 0) return
        pauseDepth -= 1
        scheduleIfReady()
    }

    fun cancel() {
        if (refreshScheduled) {
            scheduler.removeCallbacks(refreshAction)
        }
        pauseDepth = 0
        refreshRequested = false
        forceNavigationBoundary = false
        refreshScheduled = false
    }

    private fun scheduleIfReady() {
        if (pauseDepth > 0 || !refreshRequested || refreshScheduled) return
        refreshScheduled = true
        scheduler.postDelayed(refreshWindowMs, refreshAction)
    }
}
