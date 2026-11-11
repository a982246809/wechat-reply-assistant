package io.github.a982246809.replyassistant.accessibility

import android.app.Application
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import io.github.a982246809.replyassistant.FloatingBallDockSide
import io.github.a982246809.replyassistant.OverlaySizePolicy
import io.github.a982246809.replyassistant.R
import io.github.a982246809.replyassistant.toPixels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FloatingOverlayResponsiveLayoutTest {

    @Test
    fun `production overlays render within 320 360 and 411 dp hosts`() {
        val context = RuntimeEnvironment.getApplication()
        val density = context.resources.displayMetrics.density
        val minimumTouchPx = (48f * density).toInt()

        val cases = listOf(
            AppliedGeometry(320f, 960, 144, 73, 110, 55, 734, 643, 597),
            AppliedGeometry(360f, 1_080, 144, 82, 122, 61, 816, 714, 663),
            AppliedGeometry(411f, 1_233, 163, 93, 140, 70, 932, 815, 757)
        )

        cases.forEach { expected ->
            val dimensions = OverlaySizePolicy.resolve(expected.widthDp).toPixels(density)
            val inflater = LayoutInflater.from(context)

            val ball = inflater.inflate(R.layout.floating_ball, null)
            OverlayGeometryApplier.applyFloatingBall(
                ball,
                dimensions.ball,
                expanded = false,
                dockSide = FloatingBallDockSide.LEFT
            )
            assertEquals(expected.ballRootPx, ball.layoutParams.width)
            assertTrue(ball.layoutParams.width >= minimumTouchPx)
            assertEquals(null, ball.background)
            assertEquals(
                expected.ballCollapsedPx,
                ball.findViewById<View>(R.id.floatingBallVisual).layoutParams.width
            )
            OverlayGeometryApplier.applyFloatingBall(
                ball,
                dimensions.ball,
                expanded = true,
                dockSide = FloatingBallDockSide.RIGHT
            )
            assertEquals(
                expected.ballExpandedPx,
                ball.findViewById<View>(R.id.floatingBallVisual).layoutParams.width
            )
            assertEquals(
                expected.ballIconPx,
                ball.findViewById<ImageView>(R.id.floatingBallIcon).layoutParams.width
            )

            val listRoot = inflater.inflate(R.layout.floating_list_window, null)
            OverlayGeometryApplier.applySuggestionList(listRoot, dimensions.list)
            val listView = listRoot.findViewById<MaxHeightListView>(R.id.resPromptRecycler)
            listView.adapter = SuggestionListAdapter(
                context,
                List(20) {
                    SuggestionRowUiModel("这是一条用于验证换行和限高的建议 $it", SuggestionRowKind.REPLY)
                },
                dimensions.list
            )
            listRoot.measure(
                View.MeasureSpec.makeMeasureSpec(
                    expected.panelWidthPx,
                    View.MeasureSpec.EXACTLY
                ),
                View.MeasureSpec.makeMeasureSpec(6_000, View.MeasureSpec.AT_MOST)
            )
            listRoot.layout(0, 0, listRoot.measuredWidth, listRoot.measuredHeight)
            assertEquals(expected.panelWidthPx, listRoot.measuredWidth)
            assertTrue(listRoot.measuredWidth <= expected.screenWidthPx)
            assertEquals(expected.listMaxHeightPx, listView.maxHeightPx)
            assertTrue(listView.measuredHeight <= expected.listMaxHeightPx)
            assertTrue(listRoot.right <= expected.screenWidthPx)
            assertTrue(
                listRoot.findViewById<View>(R.id.refreshButton).measuredWidth >= minimumTouchPx
            )
            assertTrue(
                listRoot.findViewById<View>(R.id.closeButton).measuredHeight >= minimumTouchPx
            )

            val rowParent = FrameLayout(context)
            val row = (listView.adapter as SuggestionListAdapter).getView(0, null, rowParent)
            row.measure(
                View.MeasureSpec.makeMeasureSpec(
                    expected.panelWidthPx,
                    View.MeasureSpec.EXACTLY
                ),
                View.MeasureSpec.makeMeasureSpec(2_000, View.MeasureSpec.AT_MOST)
            )
            assertTrue(row.measuredHeight >= minimumTouchPx)
            assertTrue(
                row.findViewById<View>(R.id.suggestionRowContent).minimumHeight >= minimumTouchPx
            )
            assertTextSizeSp(row.findViewById(R.id.suggestionText), 14f)
            assertTextSizeSp(row.findViewById(R.id.suggestionActionHint), 12f)

            val realtime = inflater.inflate(R.layout.floating_realtime_suggestion, null)
            val realtimeText = realtime.findViewById<TextView>(R.id.realtimeSuggestionText)
            realtimeText.text = "这是一条足够长、需要自动换行但不会改变系统字体缩放的实时建议"
            OverlayGeometryApplier.applyRealtimeSuggestion(realtime, dimensions.realtime)
            realtimeText.text = "automatic wrapping remains width constrained ".repeat(40)
            realtime.measure(
                View.MeasureSpec.makeMeasureSpec(expected.screenWidthPx, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(2_000, View.MeasureSpec.AT_MOST)
            )
            realtime.layout(0, 0, realtime.measuredWidth, realtime.measuredHeight)
            assertTrue(realtime.measuredHeight >= minimumTouchPx)
            assertTrue(realtime.measuredWidth <= expected.screenWidthPx)
            assertEquals(
                Color.TRANSPARENT,
                (realtime.background as ColorDrawable).color
            )
            assertTrue(
                "Expected automatic wrapping at ${expected.widthDp}dp",
                realtimeText.lineCount > 1
            )
            assertEquals(3, realtimeText.maxLines)
            assertEquals(expected.realtimeTextMaxWidthPx, realtimeText.maxWidth)
            assertTextSizeSp(realtimeText, 14f)
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `large system font keeps production rows and realtime text inside the panel`() {
        val context = RuntimeEnvironment.getApplication()
        val configuration = Configuration(context.resources.configuration).apply {
            fontScale = 1.5f
        }
        context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
        val inflater = LayoutInflater.from(context)
        listOf(
            Triple(320f, 960, 734),
            Triple(360f, 1_080, 816),
            Triple(411f, 1_233, 932)
        ).forEach { (widthDp, screenWidthPx, panelWidthPx) ->
            val dimensions = OverlaySizePolicy.resolve(widthDp).toPixels(3f)
            val adapter = SuggestionListAdapter(
                context,
                listOf(
                    SuggestionRowUiModel(
                        "Large system text wraps inside the reply row",
                        SuggestionRowKind.REPLY
                    )
                ),
                dimensions.list
            )
            val row = adapter.getView(0, null, FrameLayout(context))
            row.measure(
                View.MeasureSpec.makeMeasureSpec(panelWidthPx, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(2_000, View.MeasureSpec.AT_MOST)
            )
            row.layout(0, 0, row.measuredWidth, row.measuredHeight)
            val rowText = row.findViewById<TextView>(R.id.suggestionText)
            val rowTextLayout = requireNotNull(rowText.layout)
            assertEquals(panelWidthPx, row.measuredWidth)
            assertTrue(rowText.right <= row.width)
            assertTrue(row.measuredHeight >= 144)
            assertTrue(
                "widthDp=$widthDp rowLineCount=${rowTextLayout.lineCount}",
                rowTextLayout.lineCount > 1
            )
            assertTrue(
                "widthDp=$widthDp rowLineCount=${rowTextLayout.lineCount} " +
                    "maxLines=${rowText.maxLines}",
                rowTextLayout.lineCount <= rowText.maxLines
            )
            assertEquals(
                "widthDp=$widthDp rowLastLineEnd",
                rowText.text.length,
                rowTextLayout.getLineEnd(rowTextLayout.lineCount - 1)
            )
            assertTrue(
                "widthDp=$widthDp rowLayoutHeight=${rowTextLayout.height} " +
                    "compoundPadding=${rowText.compoundPaddingTop + rowText.compoundPaddingBottom} " +
                    "textHeight=${rowText.height}",
                rowTextLayout.height + rowText.compoundPaddingTop + rowText.compoundPaddingBottom <=
                    rowText.height
            )
            assertTextSizeSp(rowText, 14f)
            assertTextSizeSp(row.findViewById(R.id.suggestionActionHint), 12f)

            val realtime = inflater.inflate(R.layout.floating_realtime_suggestion, null)
            val realtimeText = realtime.findViewById<TextView>(R.id.realtimeSuggestionText)
            realtimeText.text =
                "Large system text wraps before the input anchor safely"
            OverlayGeometryApplier.applyRealtimeSuggestion(realtime, dimensions.realtime)
            realtime.measure(
                View.MeasureSpec.makeMeasureSpec(screenWidthPx, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(2_000, View.MeasureSpec.AT_MOST)
            )
            realtime.layout(0, 0, realtime.measuredWidth, realtime.measuredHeight)
            val realtimeTextLayout = requireNotNull(realtimeText.layout)
            assertTrue(realtime.measuredWidth <= screenWidthPx)
            assertTrue(
                "widthDp=$widthDp realtimeLineCount=${realtimeTextLayout.lineCount}",
                realtimeTextLayout.lineCount > 1
            )
            assertTrue(
                "widthDp=$widthDp realtimeLineCount=${realtimeTextLayout.lineCount} " +
                    "maxLines=${realtimeText.maxLines}",
                realtimeTextLayout.lineCount <= realtimeText.maxLines
            )
            assertEquals(
                "widthDp=$widthDp realtimeLastLineEnd",
                realtimeText.text.length,
                realtimeTextLayout.getLineEnd(realtimeTextLayout.lineCount - 1)
            )
            assertTrue(
                "widthDp=$widthDp realtimeLayoutHeight=${realtimeTextLayout.height} " +
                    "compoundPadding=${realtimeText.compoundPaddingTop + realtimeText.compoundPaddingBottom} " +
                    "textHeight=${realtimeText.height}",
                realtimeTextLayout.height +
                    realtimeText.compoundPaddingTop +
                    realtimeText.compoundPaddingBottom <= realtimeText.height
            )
            assertTrue(realtimeText.right <= realtime.width)
            assertTextSizeSp(realtimeText, 14f)
        }
    }

    private fun assertTextSizeSp(textView: TextView, expectedSp: Float) {
        val expectedPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            expectedSp,
            textView.resources.displayMetrics
        )
        assertEquals(expectedPx, textView.textSize, 0.01f)
    }

    private data class AppliedGeometry(
        val widthDp: Float,
        val screenWidthPx: Int,
        val ballRootPx: Int,
        val ballCollapsedPx: Int,
        val ballExpandedPx: Int,
        val ballIconPx: Int,
        val panelWidthPx: Int,
        val listMaxHeightPx: Int,
        val realtimeTextMaxWidthPx: Int
    )
}
