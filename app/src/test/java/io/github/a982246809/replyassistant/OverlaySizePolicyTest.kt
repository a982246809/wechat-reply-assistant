package io.github.a982246809.replyassistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlaySizePolicyTest {

    @Test
    fun `common phone widths apply the fifteen percent visible geometry scale`() {
        val cases = listOf(
            VisibleGeometryExpectation(280f, 0.90f, 36.72f, 236.8f),
            VisibleGeometryExpectation(320f, 0.90f, 36.72f, 244.8f),
            VisibleGeometryExpectation(360f, 1.00f, 40.8f, 272f),
            VisibleGeometryExpectation(411f, 1.1416667f, 46.58f, 310.53334f),
            VisibleGeometryExpectation(430f, 1.15f, 46.92f, 312.8f),
            VisibleGeometryExpectation(600f, 1.15f, 46.92f, 312.8f)
        )

        cases.forEach { expected ->
            val dimensions = OverlaySizePolicy.resolve(expected.widthDp)

            assertEquals(expected.screenScale, dimensions.scale, FLOAT_TOLERANCE)
            assertEquals(expected.expandedBallDp, dimensions.ball.expandedWidthDp, FLOAT_TOLERANCE)
            assertEquals(expected.expandedBallDp, dimensions.ball.heightDp, FLOAT_TOLERANCE)
            assertEquals(expected.panelWidthDp, dimensions.list.windowWidthDp, FLOAT_TOLERANCE)
        }
    }

    @Test
    fun `screen scale clamps while visual geometry remains reduced`() {
        val compact = OverlaySizePolicy.resolve(280f)
        val largePhone = OverlaySizePolicy.resolve(430f)
        val tablet = OverlaySizePolicy.resolve(600f)

        assertEquals(0.90f, compact.scale, FLOAT_TOLERANCE)
        assertEquals(1.15f, largePhone.scale, FLOAT_TOLERANCE)
        assertEquals(1.15f, tablet.scale, FLOAT_TOLERANCE)
        assertEquals(36.72f, compact.ball.expandedWidthDp, FLOAT_TOLERANCE)
        assertEquals(46.92f, largePhone.ball.expandedWidthDp, FLOAT_TOLERANCE)
        assertEquals(46.92f, tablet.ball.expandedWidthDp, FLOAT_TOLERANCE)
    }

    @Test
    fun `reference width reduces visible geometry but retains touch targets and text`() {
        val dimensions = OverlaySizePolicy.resolve(360f)

        assertEquals(48f, dimensions.ball.rootSizeDp, FLOAT_TOLERANCE)
        assertEquals(27.2f, dimensions.ball.collapsedWidthDp, FLOAT_TOLERANCE)
        assertEquals(40.8f, dimensions.ball.expandedWidthDp, FLOAT_TOLERANCE)
        assertEquals(40.8f, dimensions.ball.heightDp, FLOAT_TOLERANCE)
        assertEquals(20.4f, dimensions.ball.iconSizeDp, FLOAT_TOLERANCE)
        assertEquals(20.4f, dimensions.ball.cornerRadiusDp, FLOAT_TOLERANCE)
        assertEquals(6.8f, dimensions.ball.elevationDp, FLOAT_TOLERANCE)
        assertEquals(6.8f, dimensions.ball.verticalMarginDp, FLOAT_TOLERANCE)

        assertEquals(48f, dimensions.list.toolbarHeightDp, FLOAT_TOLERANCE)
        assertEquals(48f, dimensions.list.actionTouchSizeDp, FLOAT_TOLERANCE)
        assertEquals(48f, dimensions.list.rowMinHeightDp, FLOAT_TOLERANCE)
        assertEquals(11.9f, dimensions.list.rowHorizontalPaddingDp, FLOAT_TOLERANCE)
        assertEquals(6.8f, dimensions.list.rowVerticalPaddingDp, FLOAT_TOLERANCE)
        assertEquals(1.7f, dimensions.list.rowLineSpacingExtraDp, FLOAT_TOLERANCE)
        assertEquals(10.2f, dimensions.list.actionHintMarginDp, FLOAT_TOLERANCE)
        assertEquals(17f, dimensions.list.actionIconSizeDp, FLOAT_TOLERANCE)
        assertEquals(15.5f, dimensions.list.actionIconPaddingDp, FLOAT_TOLERANCE)
        assertEquals(1f, dimensions.list.dividerThicknessDp, FLOAT_TOLERANCE)
        assertEquals(1.7f, dimensions.list.progressThicknessDp, FLOAT_TOLERANCE)
        assertEquals(61.2f, dimensions.list.stateHeightDp, FLOAT_TOLERANCE)
        assertEquals(17f, dimensions.list.stateHorizontalPaddingDp, FLOAT_TOLERANCE)
        assertEquals(238f, dimensions.list.maxHeightDp, FLOAT_TOLERANCE)
        assertEquals(1.7f, dimensions.list.verticalPaddingDp, FLOAT_TOLERANCE)
        assertEquals(17f, dimensions.list.cornerRadiusDp, FLOAT_TOLERANCE)
        assertEquals(20.4f, dimensions.list.closeVisualSizeDp, FLOAT_TOLERANCE)
        assertEquals(8.5f, dimensions.list.closeIconSizeDp, FLOAT_TOLERANCE)
        assertEquals(3.4f, dimensions.list.closeVisualTopMarginDp, FLOAT_TOLERANCE)
        assertEquals(14f, dimensions.list.textSizeSp, FLOAT_TOLERANCE)
        assertEquals(12f, dimensions.list.hintTextSizeSp, FLOAT_TOLERANCE)

        assertEquals(48f, dimensions.realtime.touchMinHeightDp, FLOAT_TOLERANCE)
        assertEquals(30.6f, dimensions.realtime.visualMinHeightDp, FLOAT_TOLERANCE)
        assertEquals(221f, dimensions.realtime.textMaxWidthDp, FLOAT_TOLERANCE)
        assertEquals(11.9f, dimensions.realtime.horizontalPaddingDp, FLOAT_TOLERANCE)
        assertEquals(3.4f, dimensions.realtime.outerMarginDp, FLOAT_TOLERANCE)
        assertEquals(18.7f, dimensions.realtime.cornerRadiusDp, FLOAT_TOLERANCE)
        assertEquals(1f, dimensions.realtime.strokeWidthDp, FLOAT_TOLERANCE)
        assertEquals(6.8f, dimensions.realtime.horizontalInsetDp, FLOAT_TOLERANCE)
        assertEquals(61.2f, dimensions.realtime.minTopDp, FLOAT_TOLERANCE)
        assertEquals(113.9f, dimensions.realtime.fallbackBottomOffsetDp, FLOAT_TOLERANCE)
        assertEquals(53.1f, dimensions.realtime.inputTopOffsetDp, FLOAT_TOLERANCE)
        assertEquals(14f, dimensions.realtime.textSizeSp, FLOAT_TOLERANCE)
    }

    @Test
    fun `interactive roots and action targets never shrink below forty eight dp`() {
        listOf(280f, 320f, 360f, 411f, 430f, 600f).forEach { widthDp ->
            val dimensions = OverlaySizePolicy.resolve(widthDp)

            assertTrue(dimensions.ball.rootSizeDp >= 48f)
            assertTrue(dimensions.list.toolbarHeightDp >= 48f)
            assertTrue(dimensions.list.actionTouchSizeDp >= 48f)
            assertTrue(dimensions.list.rowMinHeightDp >= 48f)
            assertEquals(48f, dimensions.realtime.touchMinHeightDp, FLOAT_TOLERANCE)
        }
    }

    @Test
    fun `pixel conversion rounds once and falls back for invalid density`() {
        val xxhdpi = OverlaySizePolicy.resolve(411f).toPixels(density = 3f)
        val fallback = OverlaySizePolicy.resolve(360f).toPixels(density = Float.NaN)

        assertEquals(163, xxhdpi.ball.rootSizePx)
        assertEquals(932, xxhdpi.list.windowWidthPx)
        assertEquals(48, fallback.ball.rootSizePx)
        assertEquals(272, fallback.list.windowWidthPx)
    }

    @Test
    fun `invalid widths fall back to reduced reference geometry`() {
        listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY).forEach { widthDp ->
            val dimensions = OverlaySizePolicy.resolve(widthDp)

            assertEquals(1f, dimensions.scale, FLOAT_TOLERANCE)
            assertEquals(48f, dimensions.ball.rootSizeDp, FLOAT_TOLERANCE)
            assertEquals(272f, dimensions.list.windowWidthDp, FLOAT_TOLERANCE)
        }
    }

    private data class VisibleGeometryExpectation(
        val widthDp: Float,
        val screenScale: Float,
        val expandedBallDp: Float,
        val panelWidthDp: Float
    )

    private companion object {
        const val FLOAT_TOLERANCE = 0.001f
    }
}
