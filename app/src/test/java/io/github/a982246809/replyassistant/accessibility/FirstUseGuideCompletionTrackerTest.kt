package io.github.a982246809.replyassistant.accessibility

import io.github.a982246809.replyassistant.data.datastore.FirstUseGuidePreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FirstUseGuideCompletionTrackerTest {
    @Test
    fun `运行中从已完成重置为未完成后允许再次完成且保持单次触发`() = runTest {
        val preferences = MutableStateFlow(
            FirstUseGuidePreferences(initialized = true, completed = true)
        )
        val tracker = FirstUseGuideCompletionTracker(initialCompleted = true)
        tracker.observe(backgroundScope, preferences)
        runCurrent()

        assertFalse(tracker.tryMarkCompleted())

        preferences.value = FirstUseGuidePreferences(initialized = true, completed = false)
        runCurrent()

        assertTrue(tracker.tryMarkCompleted())
        assertFalse(tracker.tryMarkCompleted())
    }

    @Test
    fun `取消观察后不再同步重建前服务状态`() = runTest {
        val preferences = MutableStateFlow(
            FirstUseGuidePreferences(initialized = true, completed = true)
        )
        val tracker = FirstUseGuideCompletionTracker(initialCompleted = true)
        val job = tracker.observe(backgroundScope, preferences)
        runCurrent()

        job.cancel()
        preferences.value = FirstUseGuidePreferences(initialized = true, completed = false)
        runCurrent()

        assertFalse(tracker.tryMarkCompleted())
    }
}
