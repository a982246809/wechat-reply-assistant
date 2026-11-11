package io.github.a982246809.replyassistant

import android.view.WindowManager

internal data class FloatingBallState(
    val alpha: Float,
    val isEnabled: Boolean,
    val flags: Int
)

internal enum class FloatingBallDockSide {
    LEFT,
    RIGHT
}

internal data class FloatingBallDockPosition(
    val side: FloatingBallDockSide = FloatingBallDockSide.RIGHT,
    val yRatio: Float = 0.5f
) {
    fun normalized(): FloatingBallDockPosition = copy(yRatio = yRatio.coerceIn(0f, 1f))
}

internal enum class FloatingBallInteractionState {
    COLLAPSED,
    EXPANDED,
    PRESSED,
    DRAGGING
}

internal enum class FloatingBallOperationState {
    IDLE,
    LOADING,
    SUCCESS,
    ERROR
}

internal enum class FloatingBallStatusIndicator {
    NONE,
    LOADING,
    SUCCESS,
    ERROR
}

internal data class FloatingBallPresentation(
    val expanded: Boolean,
    val alpha: Float,
    val acceptsBusinessAction: Boolean,
    val canDrag: Boolean,
    val statusIndicator: FloatingBallStatusIndicator
)

internal fun buildFloatingBallPresentation(
    interactionState: FloatingBallInteractionState,
    operationState: FloatingBallOperationState
): FloatingBallPresentation {
    val expanded = interactionState != FloatingBallInteractionState.COLLAPSED ||
        operationState != FloatingBallOperationState.IDLE
    val indicator = when (operationState) {
        FloatingBallOperationState.IDLE -> FloatingBallStatusIndicator.NONE
        FloatingBallOperationState.LOADING -> FloatingBallStatusIndicator.LOADING
        FloatingBallOperationState.SUCCESS -> FloatingBallStatusIndicator.SUCCESS
        FloatingBallOperationState.ERROR -> FloatingBallStatusIndicator.ERROR
    }
    return FloatingBallPresentation(
        expanded = expanded,
        alpha = if (expanded) 1f else 0.7f,
        acceptsBusinessAction = operationState != FloatingBallOperationState.LOADING,
        canDrag = true,
        statusIndicator = indicator
    )
}

internal fun floatingBallStatusDurationMs(state: FloatingBallOperationState): Long? {
    return when (state) {
        FloatingBallOperationState.SUCCESS -> 700L
        FloatingBallOperationState.ERROR -> 1200L
        else -> null
    }
}

internal fun parseFloatingBallDockPosition(
    sideValue: String?,
    yRatio: Float?
): FloatingBallDockPosition {
    val side = runCatching {
        sideValue?.let(FloatingBallDockSide::valueOf)
    }.getOrNull() ?: FloatingBallDockSide.RIGHT
    return FloatingBallDockPosition(side, yRatio ?: 0.5f).normalized()
}

internal fun shouldShowFloatingBallLongPressHint(
    alreadyShown: Boolean,
    completedNormalTap: Boolean
): Boolean = !alreadyShown && completedNormalTap

internal fun buildFloatingBallState(currentFlags: Int, visible: Boolean): FloatingBallState {
    val nextFlags = if (visible) {
        currentFlags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
    } else {
        currentFlags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
    }

    return FloatingBallState(
        alpha = if (visible) 1f else 0f,
        isEnabled = visible,
        flags = nextFlags
    )
}
