package io.github.a982246809.replyassistant

import io.github.a982246809.replyassistant.dto.Msg
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HoldToScrollChatHistoryControllerTest {

    @Test
    fun `按住期间持续滚动并在停止后恢复全部成功步骤`() {
        val scheduler = FakeHoldScrollScheduler()
        var visibleScreen = 0
        var historyScrollCount = 0
        var latestScrollCount = 0
        val completedResults = mutableListOf<CollectedChatHistory>()
        val controller = HoldToScrollChatHistoryController(
            scheduler = scheduler,
            settleDelayMs = 300,
            collectVisibleMessages = {
                arrayOf(Msg("对方", "message-$visibleScreen"))
            },
            scrollTowardHistory = {
                historyScrollCount += 1
                visibleScreen += 1
                true
            },
            scrollTowardLatest = {
                latestScrollCount += 1
                true
            },
            onCompleted = { completedResults += it }
        )

        controller.start()
        repeat(35) {
            scheduler.advanceTimeBy(300)
        }

        assertEquals(36, historyScrollCount)
        assertTrue(completedResults.isEmpty())

        controller.stop()
        scheduler.advanceTimeBy(300)
        repeat(36) {
            scheduler.advanceTimeBy(300)
        }

        assertEquals(36, historyScrollCount)
        assertEquals(36, latestScrollCount)
        assertEquals(
            (36 downTo 0).map { "message-$it" },
            completedResults.single().connectedMessages.map(Msg::content)
        )
    }

    @Test
    fun `取消会话应移除待执行任务且不返回残缺结果`() {
        val scheduler = FakeHoldScrollScheduler()
        var historyScrollCount = 0
        var latestScrollCount = 0
        val completedResults = mutableListOf<CollectedChatHistory>()
        val controller = HoldToScrollChatHistoryController(
            scheduler = scheduler,
            settleDelayMs = 300,
            collectVisibleMessages = { arrayOf(Msg("对方", "message")) },
            scrollTowardHistory = {
                historyScrollCount += 1
                true
            },
            scrollTowardLatest = {
                latestScrollCount += 1
                true
            },
            onCompleted = { completedResults += it }
        )

        controller.start()
        controller.cancel()
        scheduler.advanceTimeBy(3_000)

        assertEquals(1, historyScrollCount)
        assertEquals(0, latestScrollCount)
        assertTrue(completedResults.isEmpty())
    }

    @Test
    fun `到达顶部后应保持会话并在抬手时完成`() {
        val scheduler = FakeHoldScrollScheduler()
        var historyScrollCount = 0
        val completedResults = mutableListOf<CollectedChatHistory>()
        val controller = HoldToScrollChatHistoryController(
            scheduler = scheduler,
            collectVisibleMessages = { arrayOf(Msg("对方", "top-message")) },
            scrollTowardHistory = {
                historyScrollCount += 1
                false
            },
            scrollTowardLatest = { error("到达顶部前没有成功滚动，不应恢复") },
            onCompleted = { completedResults += it }
        )

        controller.start()
        scheduler.advanceTimeBy(3_000)

        assertEquals(1, historyScrollCount)
        assertTrue(completedResults.isEmpty())

        controller.stop()
        controller.stop()

        assertEquals(
            listOf("top-message"),
            completedResults.single().connectedMessages.map(Msg::content)
        )
    }

    @Test
    fun `相邻屏幕应只合并有序重叠且保留真实重复消息`() {
        val scheduler = FakeHoldScrollScheduler()
        var visibleScreen = 0
        val completedResults = mutableListOf<CollectedChatHistory>()
        val controller = HoldToScrollChatHistoryController(
            scheduler = scheduler,
            collectVisibleMessages = {
                if (visibleScreen == 0) {
                    arrayOf(
                        Msg("对方", "重复"),
                        Msg("对方", "重复"),
                        Msg("对方", "latest-message")
                    )
                } else {
                    arrayOf(
                        Msg("对方", "oldest-message"),
                        Msg("对方", "重复"),
                        Msg("对方", "重复")
                    )
                }
            },
            scrollTowardHistory = {
                if (visibleScreen == 0) {
                    visibleScreen = 1
                    true
                } else {
                    false
                }
            },
            scrollTowardLatest = { true },
            onCompleted = { completedResults += it }
        )

        controller.start()
        scheduler.advanceTimeBy(300)
        controller.stop()
        scheduler.advanceTimeBy(300)

        assertEquals(
            listOf(
                Msg("对方", "oldest-message"),
                Msg("对方", "重复"),
                Msg("对方", "重复"),
                Msg("对方", "latest-message")
            ),
            completedResults.single().connectedMessages.toList()
        )
    }

    @Test
    fun `切聊后取消待执行采集且不恢复新页面也不完成`() {
        val scheduler = FakeHoldScrollScheduler()
        var sessionValid = true
        var latestScrollCount = 0
        var cancelledCount = 0
        val completedResults = mutableListOf<CollectedChatHistory>()
        val controller = HoldToScrollChatHistoryController(
            scheduler = scheduler,
            collectVisibleMessages = { arrayOf(Msg("对方", "A消息")) },
            scrollTowardHistory = { true },
            scrollTowardLatest = {
                latestScrollCount += 1
                true
            },
            isSessionValid = { sessionValid },
            onCancelled = { cancelledCount += 1 },
            onCompleted = { completedResults += it }
        )

        controller.start()
        sessionValid = false
        scheduler.advanceTimeBy(300)
        controller.stop()
        scheduler.advanceTimeBy(3_000)

        assertEquals(0, latestScrollCount)
        assertEquals(1, cancelledCount)
        assertTrue(completedResults.isEmpty())
    }

    @Test
    fun `恢复失败时应结束且完成回调只执行一次`() {
        val scheduler = FakeHoldScrollScheduler()
        var visibleScreen = 0
        var restoreAttemptCount = 0
        val completedResults = mutableListOf<CollectedChatHistory>()
        val controller = HoldToScrollChatHistoryController(
            scheduler = scheduler,
            collectVisibleMessages = { arrayOf(Msg("对方", "message-$visibleScreen")) },
            scrollTowardHistory = {
                if (visibleScreen == 0) {
                    visibleScreen = 1
                    true
                } else {
                    false
                }
            },
            scrollTowardLatest = {
                restoreAttemptCount += 1
                false
            },
            onCompleted = { completedResults += it }
        )

        controller.start()
        scheduler.advanceTimeBy(300)
        controller.stop()
        controller.stop()
        scheduler.advanceTimeBy(3_000)

        assertEquals(1, restoreAttemptCount)
        assertEquals(1, completedResults.size)
        assertEquals(
            listOf("message-1", "message-0"),
            completedResults.single().connectedMessages.map(Msg::content)
        )
    }

    @Test
    fun `长按采集无连续重叠时保存独立 segment 且 AI 只使用初始 connected segment`() {
        val scheduler = FakeHoldScrollScheduler()
        var screen = 0
        var historyScrollCount = 0
        val completedResults = mutableListOf<CollectedChatHistory>()
        val controller = HoldToScrollChatHistoryController(
            scheduler = scheduler,
            collectVisibleMessages = {
                if (screen == 0) {
                    arrayOf(Msg("对方", "a"), Msg("对方", "b"), Msg("对方", "c"))
                } else {
                    arrayOf(Msg("对方", "x"), Msg("对方", "y"), Msg("对方", "z"))
                }
            },
            scrollTowardHistory = {
                historyScrollCount += 1
                if (historyScrollCount == 1) {
                    screen = 1
                    true
                } else {
                    false
                }
            },
            scrollTowardLatest = { screen = 0; true },
            requireContinuousWindows = true,
            onCompleted = { completedResults += it }
        )

        controller.start()
        scheduler.advanceTimeBy(300)
        controller.stop()
        scheduler.advanceTimeBy(300)

        val result = completedResults.single()
        assertEquals(listOf("a", "b", "c"), result.connectedMessages.map(Msg::content))
        assertEquals(
            listOf(listOf("x", "y", "z")),
            result.detachedSegments.map { segment -> segment.map(Msg::content) }
        )
    }

    @Test
    fun `长按恢复每一步仍要求当前窗口属于 collected timeline`() {
        val scheduler = FakeHoldScrollScheduler()
        var screen = 0
        var historyScrollCount = 0
        var cancelledCount = 0
        var latestScrollCount = 0
        val controller = HoldToScrollChatHistoryController(
            scheduler = scheduler,
            collectVisibleMessages = {
                when (screen) {
                    0 -> arrayOf(Msg("对方", "c"), Msg("对方", "d"), Msg("对方", "e"))
                    1 -> arrayOf(Msg("对方", "a"), Msg("对方", "b"), Msg("对方", "c"), Msg("对方", "d"))
                    else -> arrayOf(Msg("对方", "x"), Msg("对方", "y"), Msg("对方", "z"))
                }
            },
            scrollTowardHistory = {
                historyScrollCount += 1
                if (historyScrollCount == 1) {
                    screen = 1
                    true
                } else {
                    false
                }
            },
            scrollTowardLatest = { latestScrollCount += 1; true },
            requireContinuousWindows = true,
            onCancelled = { cancelledCount += 1 },
            onCompleted = { error("切聊后不应完成") }
        )

        controller.start()
        scheduler.advanceTimeBy(300)
        screen = 2
        controller.stop()

        assertEquals(1, cancelledCount)
        assertEquals(0, latestScrollCount)
    }

    @Test
    fun `滚动后页面不可读应恢复位置并丢弃整批结果`() {
        val scheduler = FakeHoldScrollScheduler()
        var screen = 0
        var latestScrollCount = 0
        var unavailableCount = 0
        val completedResults = mutableListOf<CollectedChatHistory>()
        val controller = HoldToScrollChatHistoryController(
            scheduler = scheduler,
            readVisibleMessages = {
                if (screen == 0) {
                    VisibleChatMessagesRead.Ready(arrayOf(Msg("对方", "当前消息")))
                } else {
                    VisibleChatMessagesRead.Blocked
                }
            },
            scrollTowardHistory = {
                if (screen == 0) {
                    screen = 1
                    true
                } else {
                    false
                }
            },
            scrollTowardLatest = {
                latestScrollCount += 1
                screen = 0
                true
            },
            requireContinuousWindows = true,
            onUnavailable = { unavailableCount += 1 },
            onCompleted = { completedResults += it }
        )

        controller.start()
        scheduler.advanceTimeBy(300)
        scheduler.advanceTimeBy(300)

        assertEquals(1, latestScrollCount)
        assertEquals(1, unavailableCount)
        assertTrue(completedResults.isEmpty())
    }

    private class FakeHoldScrollScheduler : HoldScrollScheduler {
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
