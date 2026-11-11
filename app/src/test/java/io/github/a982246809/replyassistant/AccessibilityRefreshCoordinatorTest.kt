package io.github.a982246809.replyassistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityRefreshCoordinatorTest {

    @Test
    fun `100毫秒内连续事件只刷新一次`() {
        val scheduler = FakeAccessibilityRefreshScheduler()
        val refreshes = mutableListOf<Boolean>()
        val coordinator = AccessibilityRefreshCoordinator(
            scheduler = scheduler,
            onRefresh = refreshes::add
        )

        repeat(100) {
            coordinator.request(forceNavigationBoundary = false)
        }

        scheduler.advanceTimeBy(99)
        assertTrue(refreshes.isEmpty())
        scheduler.advanceTimeBy(1)
        assertEquals(listOf(false), refreshes)
    }

    @Test
    fun `合并事件不会丢失导航边界`() {
        val scheduler = FakeAccessibilityRefreshScheduler()
        val refreshes = mutableListOf<Boolean>()
        val coordinator = AccessibilityRefreshCoordinator(
            scheduler = scheduler,
            onRefresh = refreshes::add
        )

        coordinator.request(forceNavigationBoundary = false)
        coordinator.request(forceNavigationBoundary = true)
        coordinator.request(forceNavigationBoundary = false)
        scheduler.advanceTimeBy(100)

        assertEquals(listOf(true), refreshes)
    }

    @Test
    fun `嵌套暂停全部恢复后只补一次刷新`() {
        val scheduler = FakeAccessibilityRefreshScheduler()
        val refreshes = mutableListOf<Boolean>()
        val coordinator = AccessibilityRefreshCoordinator(
            scheduler = scheduler,
            onRefresh = refreshes::add
        )

        coordinator.pause()
        coordinator.pause()
        coordinator.request(forceNavigationBoundary = false)
        coordinator.request(forceNavigationBoundary = true)
        scheduler.advanceTimeBy(500)
        coordinator.resume()
        scheduler.advanceTimeBy(500)
        assertTrue(refreshes.isEmpty())

        coordinator.resume()
        scheduler.advanceTimeBy(100)

        assertEquals(listOf(true), refreshes)
    }

    @Test
    fun `已经排队的刷新在暂停后等待恢复`() {
        val scheduler = FakeAccessibilityRefreshScheduler()
        val refreshes = mutableListOf<Boolean>()
        val coordinator = AccessibilityRefreshCoordinator(
            scheduler = scheduler,
            onRefresh = refreshes::add
        )

        coordinator.request(forceNavigationBoundary = false)
        coordinator.pause()
        scheduler.advanceTimeBy(100)
        assertTrue(refreshes.isEmpty())

        coordinator.resume()
        scheduler.advanceTimeBy(100)

        assertEquals(listOf(false), refreshes)
    }

    @Test
    fun `取消会清理待执行刷新`() {
        val scheduler = FakeAccessibilityRefreshScheduler()
        val refreshes = mutableListOf<Boolean>()
        val coordinator = AccessibilityRefreshCoordinator(
            scheduler = scheduler,
            onRefresh = refreshes::add
        )

        coordinator.request(forceNavigationBoundary = true)
        coordinator.cancel()
        scheduler.advanceTimeBy(100)

        assertTrue(refreshes.isEmpty())
    }

    private class FakeAccessibilityRefreshScheduler : AccessibilityRefreshScheduler {
        private data class ScheduledTask(
            val runnable: Runnable,
            val executeAtMs: Long
        )

        private var currentTimeMs = 0L
        private val tasks = mutableListOf<ScheduledTask>()

        override fun postDelayed(delayMs: Long, action: Runnable) {
            tasks += ScheduledTask(action, currentTimeMs + delayMs)
        }

        override fun removeCallbacks(action: Runnable) {
            tasks.removeAll { it.runnable == action }
        }

        fun advanceTimeBy(deltaMs: Long) {
            currentTimeMs += deltaMs
            val dueTasks = tasks
                .filter { it.executeAtMs <= currentTimeMs }
                .sortedBy(ScheduledTask::executeAtMs)
            tasks.removeAll(dueTasks.toSet())
            dueTasks.forEach { it.runnable.run() }
        }
    }
}
