package io.github.a982246809.replyassistant

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View

internal fun calculateFloatingBallPosition(
    initialBallX: Int,
    initialBallY: Int,
    downRawX: Int,
    downRawY: Int,
    currentRawX: Int,
    currentRawY: Int
): Pair<Int, Int> {
    val deltaX = currentRawX - downRawX
    val deltaY = currentRawY - downRawY
    return (initialBallX + deltaX) to (initialBallY + deltaY)
}

internal data class FloatingBallBounds(
    val minX: Int,
    val maxX: Int,
    val minY: Int,
    val maxY: Int
)

internal data class FloatingBallDockTarget(
    val x: Int,
    val y: Int,
    val position: FloatingBallDockPosition
)

internal fun clampFloatingBallPosition(
    x: Int,
    y: Int,
    bounds: FloatingBallBounds
): Pair<Int, Int> {
    return x.coerceIn(bounds.minX, bounds.maxX) to
        y.coerceIn(bounds.minY, bounds.maxY)
}

internal fun calculateFloatingBallDockTarget(
    x: Int,
    y: Int,
    bounds: FloatingBallBounds
): FloatingBallDockTarget {
    val (clampedX, clampedY) = clampFloatingBallPosition(x, y, bounds)
    val side = if (clampedX <= (bounds.minX + bounds.maxX) / 2) {
        FloatingBallDockSide.LEFT
    } else {
        FloatingBallDockSide.RIGHT
    }
    val yRange = bounds.maxY - bounds.minY
    val yRatio = if (yRange == 0) {
        0.5f
    } else {
        (clampedY - bounds.minY).toFloat() / yRange.toFloat()
    }
    return FloatingBallDockTarget(
        x = if (side == FloatingBallDockSide.LEFT) bounds.minX else bounds.maxX,
        y = clampedY,
        position = FloatingBallDockPosition(side, yRatio).normalized()
    )
}

internal fun calculateFloatingBallPositionFromDock(
    position: FloatingBallDockPosition,
    bounds: FloatingBallBounds
): Pair<Int, Int> {
    val normalized = position.normalized()
    val x = if (normalized.side == FloatingBallDockSide.LEFT) bounds.minX else bounds.maxX
    val y = bounds.minY + ((bounds.maxY - bounds.minY) * normalized.yRatio).toInt()
    return x to y
}

internal interface GestureCallbackScheduler {
    fun postDelayed(delayMs: Long, action: Runnable)
    fun removeCallbacks(action: Runnable)
}

internal class HandlerGestureCallbackScheduler(
    private val handler: Handler
) : GestureCallbackScheduler {
    override fun postDelayed(delayMs: Long, action: Runnable) {
        handler.postDelayed(action, delayMs)
    }

    override fun removeCallbacks(action: Runnable) {
        handler.removeCallbacks(action)
    }
}

internal class FloatingBallGestureController(
    private val touchSlopPx: Int,
    private val longPressTimeoutMs: Long = 450L,
    private val positionProvider: () -> Pair<Int, Int>,
    private val scheduler: GestureCallbackScheduler,
    private val onPressedChanged: (Boolean) -> Unit,
    private val onPositionChanged: (Int, Int) -> Unit,
    private val onDragFinished: () -> Unit,
    private val onTap: () -> Unit,
    private val onLongPress: () -> Unit,
    private val onLongPressReleased: () -> Unit = {}
) {
    private var downRawX = 0
    private var downRawY = 0
    private var lastRawX = 0
    private var lastRawY = 0
    private var initialBallX = 0
    private var initialBallY = 0
    private var isPressed = false
    private var isDragging = false
    private var longPressTriggered = false
    private var pendingLongPress: Runnable? = null

    fun onDown(rawX: Int, rawY: Int) {
        clearPendingLongPress()
        downRawX = rawX
        downRawY = rawY
        lastRawX = rawX
        lastRawY = rawY
        val (currentX, currentY) = positionProvider()
        initialBallX = currentX
        initialBallY = currentY
        isPressed = true
        isDragging = false
        longPressTriggered = false
        onPressedChanged(true)

        val longPressAction = Runnable {
            if (isPressed && !isDragging && !longPressTriggered) {
                longPressTriggered = true
                onLongPress()
            }
        }
        pendingLongPress = longPressAction
        scheduler.postDelayed(longPressTimeoutMs, longPressAction)
    }

    fun onMove(rawX: Int, rawY: Int) {
        if (!isPressed) return
        if (longPressTriggered) return
        val deltaX = rawX - downRawX
        val deltaY = rawY - downRawY
        if (!isDragging && deltaX * deltaX + deltaY * deltaY > touchSlopPx * touchSlopPx) {
            isDragging = true
            clearPendingLongPress()
        }
        if (!isDragging) return

        val (newX, newY) = calculateFloatingBallPosition(
            initialBallX = initialBallX,
            initialBallY = initialBallY,
            downRawX = downRawX,
            downRawY = downRawY,
            currentRawX = rawX,
            currentRawY = rawY
        )
        lastRawX = rawX
        lastRawY = rawY
        onPositionChanged(newX, newY)
    }

    fun onUp(rawX: Int, rawY: Int) {
        if (!isPressed) return
        if (!isDragging || rawX != lastRawX || rawY != lastRawY) {
            onMove(rawX, rawY)
        }
        clearPendingLongPress()
        val completedDrag = isDragging
        val completedLongPress = longPressTriggered
        resetPressState()
        onPressedChanged(false)
        when {
            completedDrag -> onDragFinished()
            completedLongPress -> onLongPressReleased()
            else -> onTap()
        }
    }

    fun onCancel() {
        if (!isPressed) return
        clearPendingLongPress()
        val cancelledLongPress = longPressTriggered
        resetPressState()
        onPressedChanged(false)
        if (cancelledLongPress) {
            onLongPressReleased()
        }
    }

    private fun clearPendingLongPress() {
        pendingLongPress?.let(scheduler::removeCallbacks)
        pendingLongPress = null
    }

    private fun resetPressState() {
        isPressed = false
        isDragging = false
        longPressTriggered = false
    }
}

internal class FloatingBallTouchHandler(
    touchSlopPx: Int,
    positionProvider: () -> Pair<Int, Int>,
    onPressedChanged: (Boolean) -> Unit,
    onPositionChanged: (Int, Int) -> Unit,
    onDragFinished: () -> Unit,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onLongPressReleased: () -> Unit = {},
    scheduler: GestureCallbackScheduler = HandlerGestureCallbackScheduler(
        Handler(Looper.getMainLooper())
    )
) : View.OnTouchListener {
    private val controller = FloatingBallGestureController(
        touchSlopPx = touchSlopPx,
        positionProvider = positionProvider,
        scheduler = scheduler,
        onPressedChanged = onPressedChanged,
        onPositionChanged = onPositionChanged,
        onDragFinished = onDragFinished,
        onTap = onTap,
        onLongPress = onLongPress,
        onLongPressReleased = onLongPressReleased
    )

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> controller.onDown(event.rawX.toInt(), event.rawY.toInt())
            MotionEvent.ACTION_MOVE -> controller.onMove(event.rawX.toInt(), event.rawY.toInt())
            MotionEvent.ACTION_UP -> controller.onUp(event.rawX.toInt(), event.rawY.toInt())
            MotionEvent.ACTION_CANCEL -> controller.onCancel()
            else -> return false
        }
        return true
    }

    fun cancelGesture() {
        controller.onCancel()
    }
}
