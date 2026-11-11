package io.github.a982246809.replyassistant.accessibility

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ListView

internal class MaxHeightListView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.listViewStyle
) : ListView(context, attrs, defStyleAttr) {

    var maxHeightPx: Int = Int.MAX_VALUE
        set(value) {
            val normalized = value.coerceAtLeast(0)
            if (field == normalized) return
            field = normalized
            requestLayout()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (maxHeightPx == Int.MAX_VALUE) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }
        val parentMode = View.MeasureSpec.getMode(heightMeasureSpec)
        val parentSize = View.MeasureSpec.getSize(heightMeasureSpec)
        val availableHeight = when (parentMode) {
            View.MeasureSpec.UNSPECIFIED -> maxHeightPx
            else -> minOf(parentSize, maxHeightPx)
        }
        super.onMeasure(
            widthMeasureSpec,
            View.MeasureSpec.makeMeasureSpec(availableHeight, View.MeasureSpec.AT_MOST)
        )
    }
}
