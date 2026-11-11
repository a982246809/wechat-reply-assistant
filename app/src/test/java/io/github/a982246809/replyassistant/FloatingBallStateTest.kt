package io.github.a982246809.replyassistant

import android.view.WindowManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingBallStateTest {

    @Test
    fun `隐藏悬浮球时必须不可触摸`() {
        val initialFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        val state = buildFloatingBallState(initialFlags, visible = false)

        assertEquals(0f, state.alpha)
        assertFalse(state.isEnabled)
        assertTrue(
            state.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0
        )
    }

    @Test
    fun `显示悬浮球时应恢复可触摸并保留其他标记`() {
        val initialFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        val state = buildFloatingBallState(initialFlags, visible = true)

        assertEquals(1f, state.alpha)
        assertTrue(state.isEnabled)
        assertFalse(
            state.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0
        )
        assertTrue(
            state.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0
        )
        assertTrue(
            state.flags and WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN != 0
        )
    }

    @Test
    fun `加载状态应展开并阻止重复业务操作但仍允许拖拽`() {
        val presentation = buildFloatingBallPresentation(
            interactionState = FloatingBallInteractionState.COLLAPSED,
            operationState = FloatingBallOperationState.LOADING
        )

        assertTrue(presentation.expanded)
        assertFalse(presentation.acceptsBusinessAction)
        assertTrue(presentation.canDrag)
        assertEquals(FloatingBallStatusIndicator.LOADING, presentation.statusIndicator)
    }

    @Test
    fun `闲置收起状态应使用低透明度且允许业务操作`() {
        val presentation = buildFloatingBallPresentation(
            interactionState = FloatingBallInteractionState.COLLAPSED,
            operationState = FloatingBallOperationState.IDLE
        )

        assertFalse(presentation.expanded)
        assertEquals(0.7f, presentation.alpha)
        assertTrue(presentation.acceptsBusinessAction)
        assertEquals(FloatingBallStatusIndicator.NONE, presentation.statusIndicator)
    }

    @Test
    fun `成功和失败状态应使用固定恢复时长`() {
        assertEquals(700L, floatingBallStatusDurationMs(FloatingBallOperationState.SUCCESS))
        assertEquals(1200L, floatingBallStatusDurationMs(FloatingBallOperationState.ERROR))
        assertEquals(null, floatingBallStatusDurationMs(FloatingBallOperationState.LOADING))
    }

    @Test
    fun `停靠偏好缺失或非法时应恢复右侧居中默认值`() {
        assertEquals(
            FloatingBallDockPosition(FloatingBallDockSide.RIGHT, 0.5f),
            parseFloatingBallDockPosition(sideValue = null, yRatio = null)
        )
        assertEquals(
            FloatingBallDockPosition(FloatingBallDockSide.RIGHT, 1f),
            parseFloatingBallDockPosition(sideValue = "unknown", yRatio = 2f)
        )
    }

    @Test
    fun `长按提示只应在首次普通单击完成后展示`() {
        assertTrue(shouldShowFloatingBallLongPressHint(alreadyShown = false, completedNormalTap = true))
        assertFalse(shouldShowFloatingBallLongPressHint(alreadyShown = true, completedNormalTap = true))
        assertFalse(shouldShowFloatingBallLongPressHint(alreadyShown = false, completedNormalTap = false))
    }
}
