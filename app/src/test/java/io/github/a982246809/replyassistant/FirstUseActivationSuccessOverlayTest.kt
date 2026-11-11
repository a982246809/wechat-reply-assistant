package io.github.a982246809.replyassistant

import io.github.a982246809.replyassistant.data.datastore.FirstUseGuidePreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstUseActivationSuccessOverlayTest {
    @Test
    fun `只有引导待启用且未完成时显示成功条`() {
        assertTrue(
            shouldShowFirstUseActivationSuccess(
                FirstUseGuidePreferences(activationRequested = true, completed = false)
            )
        )
        assertFalse(
            shouldShowFirstUseActivationSuccess(
                FirstUseGuidePreferences(activationRequested = false, completed = false)
            )
        )
        assertFalse(
            shouldShowFirstUseActivationSuccess(
                FirstUseGuidePreferences(activationRequested = true, completed = true)
            )
        )
    }

    @Test
    fun `显示成功条后八秒关闭且重复显示保持幂等`() {
        val host = RecordingOverlayHost()
        val scheduler = RecordingOverlayScheduler()
        val overlay = FirstUseActivationSuccessOverlay(host, scheduler)

        overlay.show()
        overlay.show()

        assertEquals(1, host.showCount)
        assertEquals(listOf(8_000L), scheduler.delays)

        scheduler.runPending()
        overlay.show()

        assertEquals(1, host.hideCount)
        assertEquals(1, host.showCount)
        assertEquals(listOf(8_000L), scheduler.delays)
        assertEquals(0, scheduler.pendingCount)
    }

    @Test
    fun `手动关闭会撤销计时并只移除一次窗口`() {
        val host = RecordingOverlayHost()
        val scheduler = RecordingOverlayScheduler()
        val overlay = FirstUseActivationSuccessOverlay(host, scheduler)

        overlay.show()
        overlay.hide()
        overlay.hide()

        assertEquals(1, host.hideCount)
        assertEquals(0, scheduler.pendingCount)
    }

    @Test
    fun `窗口添加失败时不安排关闭也不尝试移除`() {
        val host = RecordingOverlayHost(showResult = false)
        val scheduler = RecordingOverlayScheduler()
        val overlay = FirstUseActivationSuccessOverlay(host, scheduler)

        overlay.show()
        overlay.hide()

        assertEquals(1, host.showCount)
        assertEquals(0, host.hideCount)
        assertEquals(0, scheduler.pendingCount)
    }
}

private class RecordingOverlayHost(
    private val showResult: Boolean = true
) : FirstUseActivationSuccessOverlayHost {
    var showCount = 0
    var hideCount = 0

    override fun show(): Boolean {
        showCount += 1
        return showResult
    }

    override fun hide() {
        hideCount += 1
    }
}

private class RecordingOverlayScheduler : FirstUseActivationSuccessOverlayScheduler {
    private val actions = mutableListOf<Runnable>()
    val delays = mutableListOf<Long>()
    val pendingCount: Int get() = actions.size

    override fun postDelayed(action: Runnable, delayMillis: Long) {
        actions += action
        delays += delayMillis
    }

    override fun removeCallbacks(action: Runnable) {
        actions.removeAll { it === action }
    }

    fun runPending() {
        actions.toList().forEach { action ->
            actions.remove(action)
            action.run()
        }
    }
}
