package io.github.a982246809.replyassistant.accessibility

import android.app.Application
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import io.github.a982246809.replyassistant.FloatingBallDockSide
import io.github.a982246809.replyassistant.FloatingBallPixelDimensions
import io.github.a982246809.replyassistant.OverlaySizePolicy
import io.github.a982246809.replyassistant.R
import io.github.a982246809.replyassistant.toPixels
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class OverlayGeometryApplierTest {

    @Test
    fun `floating ball applies scaled hit target visual state icon and edge radius`() {
        val context = RuntimeEnvironment.getApplication()
        val root = LayoutInflater.from(context).inflate(R.layout.floating_ball, null)
        val ballView = root.findViewById<CardView>(R.id.ballView)
        val visual = root.findViewById<View>(R.id.floatingBallVisual)
        val icon = root.findViewById<ImageView>(R.id.floatingBallIcon)
        val dimensions = FloatingBallPixelDimensions(
            rootSizePx = 192,
            collapsedWidthPx = 110,
            expandedWidthPx = 164,
            heightPx = 164,
            iconSizePx = 82,
            cornerRadiusPx = 82,
            elevationPx = 27,
            verticalMarginPx = 27
        )

        OverlayGeometryApplier.applyFloatingBall(
            root = root,
            dimensions = dimensions,
            expanded = false,
            dockSide = FloatingBallDockSide.LEFT
        )

        assertEquals(192, root.layoutParams.width)
        assertEquals(192, root.layoutParams.height)
        assertEquals(192, ballView.layoutParams.width)
        assertEquals(192, ballView.layoutParams.height)
        assertEquals(110, visual.layoutParams.width)
        assertEquals(164, visual.layoutParams.height)
        assertEquals(82, icon.layoutParams.width)
        assertEquals(82, icon.layoutParams.height)
        assertEquals(27f, visual.elevation)
        val collapsedRadii = requireNotNull(
            (visual.background as GradientDrawable).cornerRadii
        )
        assertEquals(0f, collapsedRadii[0])
        assertEquals(82f, collapsedRadii[2])

        OverlayGeometryApplier.applyFloatingBall(
            root = root,
            dimensions = dimensions,
            expanded = true,
            dockSide = FloatingBallDockSide.RIGHT
        )

        assertEquals(164, visual.layoutParams.width)
        assertEquals(82f, (visual.background as GradientDrawable).cornerRadius)
    }

    @Test
    fun `suggestion list applies scaled chrome and a real maximum list height`() {
        val context = RuntimeEnvironment.getApplication()
        val root = LayoutInflater.from(context).inflate(R.layout.floating_list_window, null)
        val dimensions = OverlaySizePolicy.resolve(360f).toPixels(3f).list

        OverlayGeometryApplier.applySuggestionList(root, dimensions)

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

        assertEquals(51f, (panel.background as GradientDrawable).cornerRadius)
        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, toolbar.layoutParams.height)
        assertEquals(144, toolbar.minimumHeight)
        assertEquals(144, refresh.layoutParams.width)
        assertEquals(47, refresh.paddingLeft)
        assertEquals(144, (hint.layoutParams as RelativeLayout.LayoutParams).marginEnd)
        assertEquals(3, toolbarDivider.layoutParams.height)
        assertEquals(5, progress.layoutParams.height)
        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, loading.layoutParams.height)
        assertEquals(184, loading.minimumHeight)
        assertEquals(51, empty.paddingLeft)
        assertEquals(714, list.maxHeightPx)
        assertEquals(5, list.paddingTop)
        assertEquals(144, closeBar.layoutParams.height)
        assertEquals(61, closeVisual.layoutParams.width)
        assertEquals(10, (closeVisual.layoutParams as FrameLayout.LayoutParams).topMargin)
        assertEquals(26, closeIcon.layoutParams.width)
        assertEquals(144, closeButton.layoutParams.width)

        list.adapter = SuggestionListAdapter(
            context,
            List(20) { SuggestionRowUiModel("reply $it", SuggestionRowKind.REPLY) },
            dimensions
        )
        list.measure(
            View.MeasureSpec.makeMeasureSpec(960, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(3_000, View.MeasureSpec.AT_MOST)
        )
        assertEquals(714, list.measuredHeight)
    }

    @Test
    fun `adapter reapplies row geometry to recycled rows without scaling text`() {
        val context = RuntimeEnvironment.getApplication()
        val parent = FrameLayout(context)
        val large = OverlaySizePolicy.resolve(411f).toPixels(3f).list
        val compact = OverlaySizePolicy.resolve(320f).toPixels(3f).list
        val adapter = SuggestionListAdapter(
            context,
            listOf(SuggestionRowUiModel("你好", SuggestionRowKind.REPLY)),
            large
        )

        val row = adapter.getView(0, null, parent)
        val content = row.findViewById<View>(R.id.suggestionRowContent)
        val text = row.findViewById<TextView>(R.id.suggestionText)
        val hint = row.findViewById<TextView>(R.id.suggestionActionHint)
        val divider = row.findViewById<View>(R.id.suggestionDivider)

        assertEquals(151, content.minimumHeight)
        assertEquals(41, content.paddingLeft)
        assertEquals(6f, text.lineSpacingExtra)
        assertEquals(35, (hint.layoutParams as ViewGroup.MarginLayoutParams).marginStart)
        assertEquals(3, divider.layoutParams.height)
        assertTextSizeSp(text, 14f)

        adapter.updateDimensions(compact)
        val recycled = adapter.getView(0, row, parent)
        assertEquals(row, recycled)
        assertEquals(144, content.minimumHeight)
        assertEquals(32, content.paddingLeft)
        assertTextSizeSp(text, 14f)
    }

    @Test
    fun `realtime suggestion separates compact visual from forty eight dp hit target`() {
        val context = RuntimeEnvironment.getApplication()
        val root = LayoutInflater.from(context)
            .inflate(R.layout.floating_realtime_suggestion, null)
        val dimensions = OverlaySizePolicy.resolve(360f).toPixels(3f).realtime

        OverlayGeometryApplier.applyRealtimeSuggestion(root, dimensions)

        val visual = root.findViewById<View>(R.id.realtimeSuggestionVisual)
        val text = root.findViewById<TextView>(R.id.realtimeSuggestionText)
        val visualParams = visual.layoutParams as FrameLayout.LayoutParams
        assertEquals(144, root.minimumHeight)
        assertEquals(144, root.minimumWidth)
        assertEquals(92, visual.minimumHeight)
        assertEquals(10, visualParams.topMargin)
        assertEquals(36, visual.paddingLeft)
        assertEquals(663, text.maxWidth)
        assertEquals(56f, (visual.background as GradientDrawable).cornerRadius)
        assertTextSizeSp(text, 14f)

        root.measure(
            View.MeasureSpec.makeMeasureSpec(2_000, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(2_000, View.MeasureSpec.AT_MOST)
        )
        assertEquals(144, root.measuredHeight)
    }

    private fun assertTextSizeSp(textView: TextView, expectedSp: Float) {
        val expectedPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            expectedSp,
            textView.resources.displayMetrics
        )
        assertEquals(expectedPx, textView.textSize, 0.01f)
    }
}
