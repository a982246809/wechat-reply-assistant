package io.github.a982246809.replyassistant

import android.app.Application
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AndroidFirstUseActivationSuccessOverlayHostTest {
    @Test
    fun `成功条使用无障碍窗口且不拦截系统设置操作`() {
        val context: Application = RuntimeEnvironment.getApplication()
        var addedView: View? = null
        var addedParams: WindowManager.LayoutParams? = null
        var removedView: View? = null
        val host = AndroidFirstUseActivationSuccessOverlayHost(
            context = context,
            addView = { view, params ->
                addedView = view
                addedParams = params
            },
            removeView = { view -> removedView = view },
            onFailure = { error -> throw java.lang.AssertionError(error) }
        )

        assertTrue(host.show())

        val view = addedView as TextView
        val params = requireNotNull(addedParams)
        assertEquals("${context.getString(R.string.app_name)} 已开启，请返回应用", view.text)
        assertNotNull(view.background)
        assertFalse(view.isClickable)
        assertFalse(view.isFocusable)
        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO, view.importantForAccessibility)
        assertEquals(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, params.type)
        assertEquals(PixelFormat.TRANSLUCENT, params.format)
        assertTrue(params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0)
        assertTrue(params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0)
        assertTrue(params.flags and WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN != 0)
        assertEquals(Gravity.TOP or Gravity.CENTER_HORIZONTAL, params.gravity)
        assertTrue(params.y > 0)

        host.hide()

        assertEquals(view, removedView)
    }

    @Test
    fun `窗口系统拒绝添加时返回失败且不会尝试移除`() {
        val context: Application = RuntimeEnvironment.getApplication()
        var removedView: View? = null
        var capturedError: Throwable? = null
        val host = AndroidFirstUseActivationSuccessOverlayHost(
            context = context,
            addView = { _, _ -> throw IllegalStateException("window unavailable") },
            removeView = { view -> removedView = view },
            onFailure = { error -> capturedError = error }
        )

        assertFalse(host.show())
        host.hide()

        assertEquals("window unavailable", capturedError?.message)
        assertNull(removedView)
    }
}
