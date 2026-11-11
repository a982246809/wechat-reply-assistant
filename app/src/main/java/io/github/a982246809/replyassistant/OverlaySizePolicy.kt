package io.github.a982246809.replyassistant

import kotlin.math.roundToInt

internal data class OverlayDimensions(
    val scale: Float,
    val ball: FloatingBallDimensions,
    val list: SuggestionListDimensions,
    val realtime: RealtimeSuggestionDimensions
)

internal data class FloatingBallDimensions(
    val rootSizeDp: Float,
    val collapsedWidthDp: Float,
    val expandedWidthDp: Float,
    val heightDp: Float,
    val iconSizeDp: Float,
    val cornerRadiusDp: Float,
    val elevationDp: Float,
    val verticalMarginDp: Float
)

internal data class SuggestionListDimensions(
    val windowWidthDp: Float,
    val toolbarHeightDp: Float,
    val actionTouchSizeDp: Float,
    val rowMinHeightDp: Float,
    val rowHorizontalPaddingDp: Float,
    val rowVerticalPaddingDp: Float,
    val rowLineSpacingExtraDp: Float,
    val actionHintMarginDp: Float,
    val actionIconSizeDp: Float,
    val actionIconPaddingDp: Float,
    val dividerThicknessDp: Float,
    val progressThicknessDp: Float,
    val stateHeightDp: Float,
    val stateHorizontalPaddingDp: Float,
    val maxHeightDp: Float,
    val verticalPaddingDp: Float,
    val cornerRadiusDp: Float,
    val closeVisualSizeDp: Float,
    val closeIconSizeDp: Float,
    val closeVisualTopMarginDp: Float,
    val textSizeSp: Float,
    val hintTextSizeSp: Float
)

internal data class RealtimeSuggestionDimensions(
    val touchMinHeightDp: Float,
    val visualMinHeightDp: Float,
    val textMaxWidthDp: Float,
    val horizontalPaddingDp: Float,
    val outerMarginDp: Float,
    val cornerRadiusDp: Float,
    val strokeWidthDp: Float,
    val horizontalInsetDp: Float,
    val minTopDp: Float,
    val fallbackBottomOffsetDp: Float,
    val inputTopOffsetDp: Float,
    val textSizeSp: Float
)

internal data class OverlayPixelDimensions(
    val scale: Float,
    val density: Float,
    val ball: FloatingBallPixelDimensions,
    val list: SuggestionListPixelDimensions,
    val realtime: RealtimeSuggestionPixelDimensions
)

internal data class FloatingBallPixelDimensions(
    val rootSizePx: Int,
    val collapsedWidthPx: Int,
    val expandedWidthPx: Int,
    val heightPx: Int,
    val iconSizePx: Int,
    val cornerRadiusPx: Int,
    val elevationPx: Int,
    val verticalMarginPx: Int
)

internal data class SuggestionListPixelDimensions(
    val windowWidthPx: Int,
    val toolbarHeightPx: Int,
    val actionTouchSizePx: Int,
    val rowMinHeightPx: Int,
    val rowHorizontalPaddingPx: Int,
    val rowVerticalPaddingPx: Int,
    val rowLineSpacingExtraPx: Int,
    val actionHintMarginPx: Int,
    val actionIconSizePx: Int,
    val actionIconPaddingPx: Int,
    val dividerThicknessPx: Int,
    val progressThicknessPx: Int,
    val stateHeightPx: Int,
    val stateHorizontalPaddingPx: Int,
    val maxHeightPx: Int,
    val verticalPaddingPx: Int,
    val cornerRadiusPx: Int,
    val closeVisualSizePx: Int,
    val closeIconSizePx: Int,
    val closeVisualTopMarginPx: Int,
    val textSizeSp: Float,
    val hintTextSizeSp: Float
)

internal data class RealtimeSuggestionPixelDimensions(
    val touchMinHeightPx: Int,
    val visualMinHeightPx: Int,
    val textMaxWidthPx: Int,
    val horizontalPaddingPx: Int,
    val outerMarginPx: Int,
    val cornerRadiusPx: Int,
    val strokeWidthPx: Int,
    val horizontalInsetPx: Int,
    val minTopPx: Int,
    val fallbackBottomOffsetPx: Int,
    val inputTopOffsetPx: Int,
    val textSizeSp: Float
)

internal fun OverlayDimensions.toPixels(density: Float): OverlayPixelDimensions {
    val resolvedDensity = density.takeIf { it.isFinite() && it > 0f } ?: 1f
    fun Float.toPx(): Int = (this * resolvedDensity).roundToInt()

    return OverlayPixelDimensions(
        scale = scale,
        density = resolvedDensity,
        ball = FloatingBallPixelDimensions(
            rootSizePx = ball.rootSizeDp.toPx(),
            collapsedWidthPx = ball.collapsedWidthDp.toPx(),
            expandedWidthPx = ball.expandedWidthDp.toPx(),
            heightPx = ball.heightDp.toPx(),
            iconSizePx = ball.iconSizeDp.toPx(),
            cornerRadiusPx = ball.cornerRadiusDp.toPx(),
            elevationPx = ball.elevationDp.toPx(),
            verticalMarginPx = ball.verticalMarginDp.toPx()
        ),
        list = SuggestionListPixelDimensions(
            windowWidthPx = list.windowWidthDp.toPx(),
            toolbarHeightPx = list.toolbarHeightDp.toPx(),
            actionTouchSizePx = list.actionTouchSizeDp.toPx(),
            rowMinHeightPx = list.rowMinHeightDp.toPx(),
            rowHorizontalPaddingPx = list.rowHorizontalPaddingDp.toPx(),
            rowVerticalPaddingPx = list.rowVerticalPaddingDp.toPx(),
            rowLineSpacingExtraPx = list.rowLineSpacingExtraDp.toPx(),
            actionHintMarginPx = list.actionHintMarginDp.toPx(),
            actionIconSizePx = list.actionIconSizeDp.toPx(),
            actionIconPaddingPx = list.actionIconPaddingDp.toPx(),
            dividerThicknessPx = list.dividerThicknessDp.toPx().coerceAtLeast(1),
            progressThicknessPx = list.progressThicknessDp.toPx().coerceAtLeast(1),
            stateHeightPx = list.stateHeightDp.toPx(),
            stateHorizontalPaddingPx = list.stateHorizontalPaddingDp.toPx(),
            maxHeightPx = list.maxHeightDp.toPx(),
            verticalPaddingPx = list.verticalPaddingDp.toPx(),
            cornerRadiusPx = list.cornerRadiusDp.toPx(),
            closeVisualSizePx = list.closeVisualSizeDp.toPx(),
            closeIconSizePx = list.closeIconSizeDp.toPx(),
            closeVisualTopMarginPx = list.closeVisualTopMarginDp.toPx(),
            textSizeSp = list.textSizeSp,
            hintTextSizeSp = list.hintTextSizeSp
        ),
        realtime = RealtimeSuggestionPixelDimensions(
            touchMinHeightPx = realtime.touchMinHeightDp.toPx(),
            visualMinHeightPx = realtime.visualMinHeightDp.toPx(),
            textMaxWidthPx = realtime.textMaxWidthDp.toPx(),
            horizontalPaddingPx = realtime.horizontalPaddingDp.toPx(),
            outerMarginPx = realtime.outerMarginDp.toPx(),
            cornerRadiusPx = realtime.cornerRadiusDp.toPx(),
            strokeWidthPx = realtime.strokeWidthDp.toPx().coerceAtLeast(1),
            horizontalInsetPx = realtime.horizontalInsetDp.toPx(),
            minTopPx = realtime.minTopDp.toPx(),
            fallbackBottomOffsetPx = realtime.fallbackBottomOffsetDp.toPx(),
            inputTopOffsetPx = realtime.inputTopOffsetDp.toPx(),
            textSizeSp = realtime.textSizeSp
        )
    )
}

internal object OverlaySizePolicy {
    private const val REFERENCE_WIDTH_DP = 360f
    private const val MIN_SCALE = 0.90f
    private const val MAX_SCALE = 1.15f
    private const val MIN_TOUCH_TARGET_DP = 48f
    private const val VISUAL_SCALE_FACTOR = 0.85f

    fun resolve(screenWidthDp: Float): OverlayDimensions {
        val resolvedWidthDp = screenWidthDp
            .takeIf { it.isFinite() && it > 0f }
            ?: REFERENCE_WIDTH_DP
        val scale = (resolvedWidthDp / REFERENCE_WIDTH_DP).coerceIn(MIN_SCALE, MAX_SCALE)
        val visualScale = scale * VISUAL_SCALE_FACTOR
        val touchTargetDp = maxOf(MIN_TOUCH_TARGET_DP, 44f * scale)
        val actionIconSizeDp = 20f * visualScale
        val realtimeOuterMarginDp = 4f * visualScale
        val realtimeTouchHeightDp = maxOf(
            MIN_TOUCH_TARGET_DP,
            36f * visualScale + realtimeOuterMarginDp * 2f
        )
        val panelWidthDp = minOf(
            (resolvedWidthDp - 48f * scale).coerceAtLeast(1f),
            320f * visualScale
        )

        return OverlayDimensions(
            scale = scale,
            ball = FloatingBallDimensions(
                rootSizeDp = maxOf(MIN_TOUCH_TARGET_DP, 56f * visualScale),
                collapsedWidthDp = 32f * visualScale,
                expandedWidthDp = 48f * visualScale,
                heightDp = 48f * visualScale,
                iconSizeDp = 24f * visualScale,
                cornerRadiusDp = 24f * visualScale,
                elevationDp = 8f * visualScale,
                verticalMarginDp = 8f * visualScale
            ),
            list = SuggestionListDimensions(
                windowWidthDp = panelWidthDp,
                toolbarHeightDp = touchTargetDp,
                actionTouchSizeDp = touchTargetDp,
                rowMinHeightDp = touchTargetDp,
                rowHorizontalPaddingDp = 14f * visualScale,
                rowVerticalPaddingDp = 8f * visualScale,
                rowLineSpacingExtraDp = 2f * visualScale,
                actionHintMarginDp = 12f * visualScale,
                actionIconSizeDp = actionIconSizeDp,
                actionIconPaddingDp = (touchTargetDp - actionIconSizeDp) / 2f,
                dividerThicknessDp = 1f,
                progressThicknessDp = 2f * visualScale,
                stateHeightDp = 72f * visualScale,
                stateHorizontalPaddingDp = 20f * visualScale,
                maxHeightDp = 280f * visualScale,
                verticalPaddingDp = 2f * visualScale,
                cornerRadiusDp = 20f * visualScale,
                closeVisualSizeDp = 24f * visualScale,
                closeIconSizeDp = 10f * visualScale,
                closeVisualTopMarginDp = 4f * visualScale,
                textSizeSp = 14f,
                hintTextSizeSp = 12f
            ),
            realtime = RealtimeSuggestionDimensions(
                touchMinHeightDp = realtimeTouchHeightDp,
                visualMinHeightDp = 36f * visualScale,
                textMaxWidthDp = 260f * visualScale,
                horizontalPaddingDp = 14f * visualScale,
                outerMarginDp = realtimeOuterMarginDp,
                cornerRadiusDp = 22f * visualScale,
                strokeWidthDp = 1f,
                horizontalInsetDp = 8f * visualScale,
                minTopDp = 72f * visualScale,
                fallbackBottomOffsetDp = 134f * visualScale,
                inputTopOffsetDp = realtimeTouchHeightDp + 6f * visualScale,
                textSizeSp = 14f
            )
        )
    }
}
