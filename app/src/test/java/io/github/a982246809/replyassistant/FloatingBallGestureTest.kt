package io.github.a982246809.replyassistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingBallGestureTest {

    @Test
    fun `拖拽位置应按手指位移增量计算`() {
        val position = calculateFloatingBallPosition(
            initialBallX = 300,
            initialBallY = 600,
            downRawX = 100,
            downRawY = 200,
            currentRawX = 130,
            currentRawY = 245
        )

        assertEquals(330, position.first)
        assertEquals(645, position.second)
    }

    @Test
    fun `拖拽位置应限制在安全区域内`() {
        val bounds = FloatingBallBounds(minX = 8, maxX = 1016, minY = 24, maxY = 1800)

        assertEquals(8 to 1800, clampFloatingBallPosition(-20, 1900, bounds))
        assertEquals(400 to 600, clampFloatingBallPosition(400, 600, bounds))
    }

    @Test
    fun `松手时应吸附最近侧边并保存纵向比例`() {
        val bounds = FloatingBallBounds(minX = 8, maxX = 1016, minY = 24, maxY = 1824)

        val left = calculateFloatingBallDockTarget(300, 924, bounds)
        val right = calculateFloatingBallDockTarget(800, 474, bounds)

        assertEquals(FloatingBallDockSide.LEFT, left.position.side)
        assertEquals(8, left.x)
        assertEquals(0.5f, left.position.yRatio, 0.0001f)
        assertEquals(FloatingBallDockSide.RIGHT, right.position.side)
        assertEquals(1016, right.x)
        assertEquals(0.25f, right.position.yRatio, 0.0001f)
    }

    @Test
    fun `保存位置应按新屏幕安全区域恢复`() {
        val position = FloatingBallDockPosition(FloatingBallDockSide.LEFT, 0.75f)

        val restored = calculateFloatingBallPositionFromDock(
            position,
            FloatingBallBounds(minX = 12, maxX = 492, minY = 20, maxY = 1020)
        )

        assertEquals(12 to 770, restored)
    }

    @Test
    fun `短按抬手时应立即且仅触发一次单击`() {
        val triggeredActions = mutableListOf<String>()
        val scheduler = FakeGestureCallbackScheduler()
        val controller = FloatingBallGestureController(
            touchSlopPx = 10,
            positionProvider = { 300 to 600 },
            scheduler = scheduler,
            onPressedChanged = {},
            onPositionChanged = { _, _ -> },
            onDragFinished = {},
            onTap = { triggeredActions += "tap" },
            onLongPress = { triggeredActions += "long" }
        )

        controller.onDown(rawX = 100, rawY = 200)
        controller.onUp(rawX = 104, rawY = 204)

        assertEquals(listOf("tap"), triggeredActions)
        scheduler.advanceTimeBy(500)
        assertEquals(listOf("tap"), triggeredActions)
    }

    @Test
    fun `长按450毫秒应触发更多读取且抬手不再触发单击`() {
        val triggeredActions = mutableListOf<String>()
        val scheduler = FakeGestureCallbackScheduler()
        val controller = FloatingBallGestureController(
            touchSlopPx = 10,
            longPressTimeoutMs = 450,
            positionProvider = { 300 to 600 },
            scheduler = scheduler,
            onPressedChanged = {},
            onPositionChanged = { _, _ -> },
            onDragFinished = {},
            onTap = { triggeredActions += "tap" },
            onLongPress = { triggeredActions += "long" },
            onLongPressReleased = { triggeredActions += "long-released" }
        )

        controller.onDown(rawX = 100, rawY = 200)
        scheduler.advanceTimeBy(449)
        assertTrue(triggeredActions.isEmpty())
        scheduler.advanceTimeBy(1)
        controller.onUp(rawX = 100, rawY = 200)

        assertEquals(listOf("long", "long-released"), triggeredActions)
    }

    @Test
    fun `长按触发后移动手指应保持滚动状态直到抬手`() {
        val events = mutableListOf<String>()
        val scheduler = FakeGestureCallbackScheduler()
        val controller = FloatingBallGestureController(
            touchSlopPx = 10,
            longPressTimeoutMs = 450,
            positionProvider = { 300 to 600 },
            scheduler = scheduler,
            onPressedChanged = {},
            onPositionChanged = { _, _ -> events += "move" },
            onDragFinished = { events += "drag-finished" },
            onTap = { events += "tap" },
            onLongPress = { events += "long" },
            onLongPressReleased = { events += "long-released" }
        )

        controller.onDown(rawX = 100, rawY = 200)
        scheduler.advanceTimeBy(450)
        controller.onMove(rawX = 130, rawY = 230)
        controller.onUp(rawX = 130, rawY = 230)

        assertEquals(listOf("long", "long-released"), events)
    }

    @Test
    fun `长按触发后取消触摸应停止持续滚动`() {
        val events = mutableListOf<String>()
        val scheduler = FakeGestureCallbackScheduler()
        val controller = FloatingBallGestureController(
            touchSlopPx = 10,
            longPressTimeoutMs = 450,
            positionProvider = { 300 to 600 },
            scheduler = scheduler,
            onPressedChanged = {},
            onPositionChanged = { _, _ -> },
            onDragFinished = {},
            onTap = { events += "tap" },
            onLongPress = { events += "long" },
            onLongPressReleased = { events += "long-released" }
        )

        controller.onDown(rawX = 100, rawY = 200)
        scheduler.advanceTimeBy(450)
        controller.onCancel()

        assertEquals(listOf("long", "long-released"), events)
    }

    @Test
    fun `移动超过阈值应取消长按并在抬手时完成拖拽`() {
        val events = mutableListOf<String>()
        val scheduler = FakeGestureCallbackScheduler()
        val controller = FloatingBallGestureController(
            touchSlopPx = 10,
            positionProvider = { 300 to 600 },
            scheduler = scheduler,
            onPressedChanged = {},
            onPositionChanged = { x, y -> events += "move:$x,$y" },
            onDragFinished = { events += "drag-finished" },
            onTap = { events += "tap" },
            onLongPress = { events += "long" }
        )

        controller.onDown(rawX = 100, rawY = 200)
        controller.onMove(rawX = 120, rawY = 230)
        scheduler.advanceTimeBy(500)
        controller.onUp(rawX = 120, rawY = 230)

        assertEquals(listOf("move:320,630", "drag-finished"), events)
    }

    @Test
    fun `抬手位移超过阈值即使没有移动事件也不应误触单击`() {
        val events = mutableListOf<String>()
        val scheduler = FakeGestureCallbackScheduler()
        val controller = FloatingBallGestureController(
            touchSlopPx = 10,
            positionProvider = { 300 to 600 },
            scheduler = scheduler,
            onPressedChanged = {},
            onPositionChanged = { x, y -> events += "move:$x,$y" },
            onDragFinished = { events += "drag-finished" },
            onTap = { events += "tap" },
            onLongPress = { events += "long" }
        )

        controller.onDown(rawX = 100, rawY = 200)
        controller.onUp(rawX = 130, rawY = 230)

        assertEquals(listOf("move:330,630", "drag-finished"), events)
    }

    @Test
    fun `取消触摸应清理长按且不触发业务操作`() {
        val events = mutableListOf<String>()
        val scheduler = FakeGestureCallbackScheduler()
        val controller = FloatingBallGestureController(
            touchSlopPx = 10,
            positionProvider = { 300 to 600 },
            scheduler = scheduler,
            onPressedChanged = { events += "pressed:$it" },
            onPositionChanged = { _, _ -> },
            onDragFinished = { events += "drag-finished" },
            onTap = { events += "tap" },
            onLongPress = { events += "long" }
        )

        controller.onDown(rawX = 100, rawY = 200)
        controller.onCancel()
        scheduler.advanceTimeBy(500)

        assertEquals(listOf("pressed:true", "pressed:false"), events)
    }

    private class FakeGestureCallbackScheduler : GestureCallbackScheduler {
        data class ScheduledTask(
            val runnable: Runnable,
            val executeAtMs: Long
        )

        var currentTimeMs: Long = 0
            private set

        private val tasks = mutableListOf<ScheduledTask>()

        override fun postDelayed(delayMs: Long, action: Runnable) {
            tasks += ScheduledTask(
                runnable = action,
                executeAtMs = currentTimeMs + delayMs
            )
        }

        override fun removeCallbacks(action: Runnable) {
            tasks.removeAll { it.runnable == action }
        }

        fun advanceTimeBy(deltaMs: Long) {
            currentTimeMs += deltaMs
            val dueTasks = tasks
                .filter { it.executeAtMs <= currentTimeMs }
                .sortedBy { it.executeAtMs }

            tasks.removeAll(dueTasks.toSet())
            dueTasks.forEach { it.runnable.run() }
        }
    }
}
