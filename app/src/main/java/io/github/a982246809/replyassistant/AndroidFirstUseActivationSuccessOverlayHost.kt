package io.github.a982246809.replyassistant

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import kotlin.math.roundToInt

internal class AndroidFirstUseActivationSuccessOverlayHost(
    private val context: Context,
    private val addView: (View, WindowManager.LayoutParams) -> Unit,
    private val removeView: (View) -> Unit,
    private val onFailure: (Throwable) -> Unit
) : FirstUseActivationSuccessOverlayHost {
    constructor(
        context: Context,
        windowManager: WindowManager,
        onFailure: (Throwable) -> Unit
    ) : this(
        context = context,
        addView = windowManager::addView,
        removeView = windowManager::removeView,
        onFailure = onFailure
    )

    private var attachedView: View? = null

    override fun show(): Boolean {
        if (attachedView != null) return true

        return runCatching {
            val view = LayoutInflater.from(context)
                .inflate(R.layout.first_use_activation_success_overlay, null) as TextView
            view.text = context.getString(
                R.string.first_use_activation_success_overlay,
                context.getString(R.string.app_name)
            )
            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = (48 * context.resources.displayMetrics.density).roundToInt()
            }
            addView(view, layoutParams)
            attachedView = view
        }.onFailure(onFailure).isSuccess
    }

    override fun hide() {
        val view = attachedView ?: return
        attachedView = null
        runCatching { removeView(view) }.onFailure(onFailure)
    }
}

internal class HandlerFirstUseActivationSuccessOverlayScheduler(
    private val handler: Handler
) : FirstUseActivationSuccessOverlayScheduler {
    override fun postDelayed(action: Runnable, delayMillis: Long) {
        handler.postDelayed(action, delayMillis)
    }

    override fun removeCallbacks(action: Runnable) {
        handler.removeCallbacks(action)
    }
}
