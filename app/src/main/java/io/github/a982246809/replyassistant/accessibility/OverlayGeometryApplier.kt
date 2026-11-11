package io.github.a982246809.replyassistant.accessibility

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import io.github.a982246809.replyassistant.FloatingBallDockSide
import io.github.a982246809.replyassistant.FloatingBallPixelDimensions
import io.github.a982246809.replyassistant.R
import io.github.a982246809.replyassistant.RealtimeSuggestionPixelDimensions
import io.github.a982246809.replyassistant.SuggestionListPixelDimensions

internal object OverlayGeometryApplier {
    fun applyFloatingBall(
        root: View,
        dimensions: FloatingBallPixelDimensions,
        expanded: Boolean,
        dockSide: FloatingBallDockSide,
        applyVisualWidth: Boolean = true
    ) {
        val ballView = root.findViewById<CardView>(R.id.ballView)
        val visual = root.findViewById<View>(R.id.floatingBallVisual)
        val icon = root.findViewById<ImageView>(R.id.floatingBallIcon)

        root.updateSize(dimensions.rootSizePx, dimensions.rootSizePx)
        ballView.updateSize(dimensions.rootSizePx, dimensions.rootSizePx)

        val visualWidth = if (expanded) {
            dimensions.expandedWidthPx
        } else {
            dimensions.collapsedWidthPx
        }
        if (applyVisualWidth) {
            visual.updateSize(visualWidth, dimensions.heightPx)
        } else {
            visual.updateHeight(dimensions.heightPx)
        }
        (visual.layoutParams as? FrameLayout.LayoutParams)?.let { params ->
            params.gravity =
                (if (dockSide == FloatingBallDockSide.LEFT) Gravity.START else Gravity.END) or
                    Gravity.CENTER_VERTICAL
            visual.layoutParams = params
        }
        visual.elevation = dimensions.elevationPx.toFloat()
        visual.setBackgroundResource(
            when {
                expanded -> R.drawable.floating_ball_expanded
                dockSide == FloatingBallDockSide.LEFT -> R.drawable.floating_ball_edge_left
                else -> R.drawable.floating_ball_edge_right
            }
        )
        (visual.background.mutate() as? GradientDrawable)?.apply {
            if (expanded) {
                cornerRadius = dimensions.cornerRadiusPx.toFloat()
            } else {
                val radius = dimensions.cornerRadiusPx.toFloat()
                cornerRadii = if (dockSide == FloatingBallDockSide.LEFT) {
                    floatArrayOf(0f, 0f, radius, radius, radius, radius, 0f, 0f)
                } else {
                    floatArrayOf(radius, radius, 0f, 0f, 0f, 0f, radius, radius)
                }
            }
        }

        icon.updateSize(dimensions.iconSizePx, dimensions.iconSizePx)
    }

    fun applySuggestionList(root: View, dimensions: SuggestionListPixelDimensions) {
        val panel = root.findViewById<View>(R.id.suggestionPanel)
        val toolbar = root.findViewById<View>(R.id.suggestionToolbar)
        val refresh = root.findViewById<ImageView>(R.id.refreshButton)
        val hint = root.findViewById<TextView>(R.id.gestureHintText)
        val toolbarDivider = root.findViewById<View>(R.id.suggestionToolbarDivider)
        val progress = root.findViewById<View>(R.id.progressBar)
        val loading = root.findViewById<TextView>(R.id.loadingTextView)
        val empty = root.findViewById<TextView>(R.id.emptyStateView)
        val list = root.findViewById<MaxHeightListView>(R.id.resPromptRecycler)
        val closeBar = root.findViewById<View>(R.id.closeBar)
        val closeVisual = root.findViewById<View>(R.id.closeButtonVisual)
        val closeIcon = root.findViewById<ImageView>(R.id.closeButtonIcon)
        val closeButton = root.findViewById<View>(R.id.closeButton)

        panel.background = roundedBackground(
            color = ContextCompat.getColor(root.context, R.color.suggestion_panel),
            cornerRadiusPx = dimensions.cornerRadiusPx
        )
        toolbar.updateHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
        toolbar.minimumHeight = dimensions.toolbarHeightPx
        refresh.updateSize(dimensions.actionTouchSizePx, dimensions.actionTouchSizePx)
        refresh.setPadding(
            dimensions.actionIconPaddingPx,
            dimensions.actionIconPaddingPx,
            dimensions.actionIconPaddingPx,
            dimensions.actionIconPaddingPx
        )
        (hint.layoutParams as? RelativeLayout.LayoutParams)?.let { params ->
            params.marginEnd = dimensions.actionTouchSizePx
            hint.layoutParams = params
        }
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, dimensions.hintTextSizeSp)
        toolbarDivider.updateHeight(dimensions.dividerThicknessPx)
        progress.updateHeight(dimensions.progressThicknessPx)
        listOf(loading, empty).forEach { stateView ->
            stateView.updateHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
            stateView.minimumHeight = dimensions.stateHeightPx
            stateView.setPadding(
                dimensions.stateHorizontalPaddingPx,
                stateView.paddingTop,
                dimensions.stateHorizontalPaddingPx,
                stateView.paddingBottom
            )
            stateView.setTextSize(TypedValue.COMPLEX_UNIT_SP, dimensions.textSizeSp)
        }
        list.maxHeightPx = dimensions.maxHeightPx
        list.setPadding(
            list.paddingLeft,
            dimensions.verticalPaddingPx,
            list.paddingRight,
            dimensions.verticalPaddingPx
        )
        closeBar.updateHeight(dimensions.actionTouchSizePx)
        closeVisual.updateSize(dimensions.closeVisualSizePx, dimensions.closeVisualSizePx)
        (closeVisual.layoutParams as? FrameLayout.LayoutParams)?.let { params ->
            params.topMargin = dimensions.closeVisualTopMarginPx
            closeVisual.layoutParams = params
        }
        closeIcon.updateSize(dimensions.closeIconSizePx, dimensions.closeIconSizePx)
        closeButton.updateSize(dimensions.actionTouchSizePx, dimensions.actionTouchSizePx)
        closeButton.background = RippleDrawable(
            ColorStateList.valueOf(
                ContextCompat.getColor(root.context, R.color.suggestion_ripple)
            ),
            null,
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(android.graphics.Color.WHITE)
            }
        )
    }

    fun applySuggestionRow(row: View, dimensions: SuggestionListPixelDimensions) {
        val content = row.findViewById<View>(R.id.suggestionRowContent)
        val text = row.findViewById<TextView>(R.id.suggestionText)
        val actionHint = row.findViewById<TextView>(R.id.suggestionActionHint)
        val divider = row.findViewById<View>(R.id.suggestionDivider)

        content.minimumHeight = dimensions.rowMinHeightPx
        content.setPadding(
            dimensions.rowHorizontalPaddingPx,
            dimensions.rowVerticalPaddingPx,
            dimensions.rowHorizontalPaddingPx,
            dimensions.rowVerticalPaddingPx
        )
        text.setLineSpacing(dimensions.rowLineSpacingExtraPx.toFloat(), 1f)
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, dimensions.textSizeSp)
        actionHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, dimensions.hintTextSizeSp)
        (actionHint.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
            params.marginStart = dimensions.actionHintMarginPx
            actionHint.layoutParams = params
        }
        divider.updateHeight(dimensions.dividerThicknessPx)
    }

    fun applyRealtimeSuggestion(root: View, dimensions: RealtimeSuggestionPixelDimensions) {
        val visual = root.findViewById<View>(R.id.realtimeSuggestionVisual)
        val text = root.findViewById<TextView>(R.id.realtimeSuggestionText)

        root.minimumHeight = dimensions.touchMinHeightPx
        root.minimumWidth = dimensions.touchMinHeightPx
        visual.minimumHeight = dimensions.visualMinHeightPx
        visual.setPadding(
            dimensions.horizontalPaddingPx,
            visual.paddingTop,
            dimensions.horizontalPaddingPx,
            visual.paddingBottom
        )
        (visual.layoutParams as? FrameLayout.LayoutParams)?.let { params ->
            params.setMargins(
                dimensions.outerMarginPx,
                dimensions.outerMarginPx,
                dimensions.outerMarginPx,
                dimensions.outerMarginPx
            )
            visual.layoutParams = params
        }
        visual.background = roundedBackground(
            color = ContextCompat.getColor(root.context, R.color.suggestion_panel),
            cornerRadiusPx = dimensions.cornerRadiusPx,
            strokeWidthPx = dimensions.strokeWidthPx,
            strokeColor = ContextCompat.getColor(root.context, R.color.suggestion_border)
        )
        text.maxWidth = dimensions.textMaxWidthPx
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, dimensions.textSizeSp)
    }

    private fun roundedBackground(
        color: Int,
        cornerRadiusPx: Int,
        strokeWidthPx: Int = 0,
        strokeColor: Int = android.graphics.Color.TRANSPARENT
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = cornerRadiusPx.toFloat()
            if (strokeWidthPx > 0) {
                setStroke(strokeWidthPx, strokeColor)
            }
        }
    }

    private fun View.updateSize(width: Int, height: Int) {
        val params = layoutParams ?: ViewGroup.LayoutParams(width, height)
        params.width = width
        params.height = height
        layoutParams = params
    }

    private fun View.updateHeight(height: Int) {
        val params = layoutParams ?: ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            height
        )
        params.height = height
        layoutParams = params
    }
}
