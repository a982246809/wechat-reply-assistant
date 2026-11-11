package io.github.a982246809.replyassistant.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.OutputStream
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal fun isCompatibleWindowBitmap(
    windowBounds: WeChatWindowBounds,
    bitmapWidth: Int,
    bitmapHeight: Int,
    tolerancePx: Int = 2
): Boolean = !windowBounds.isEmpty &&
    bitmapWidth > 0 &&
    bitmapHeight > 0 &&
    abs((windowBounds.right - windowBounds.left) - bitmapWidth) <= tolerancePx &&
    abs((windowBounds.bottom - windowBounds.top) - bitmapHeight) <= tolerancePx

/** Reads only the small set of WeChat nodes needed to classify the visible frame. */
internal class AccessibilityWeChatVoiceFrameReader : WeChatVoiceFrameReader {
    override fun read(
        generation: Long,
        chatTitle: String?,
        root: AccessibilityNodeInfo
    ): WeChatFrameRead {
        val records = root.findAccessibilityNodeInfosByViewId(MESSAGE_CONTAINER_VIEW_ID)
            .filter(AccessibilityNodeInfo::isVisibleToUser)
            .map(::snapshotRecord)
        return WeChatVoiceRecordParser.parse(
            generation = generation,
            chatTitle = chatTitle,
            windowId = root.windowId,
            records = records
        )
    }

    private fun snapshotRecord(record: AccessibilityNodeInfo): WeChatRecordNodeSnapshot {
        val timeLabel = record.findAccessibilityNodeInfosByViewId(TIME_LABEL_VIEW_ID)
            .firstOrNull(AccessibilityNodeInfo::isVisibleToUser)
            ?.text
            ?.toString()
        val rawMessageNodes = record.findAccessibilityNodeInfosByViewId(MESSAGE_TEXT_VIEW_ID)
        if (rawMessageNodes.isEmpty()) {
            return WeChatRecordNodeSnapshot(
                timeLabel = timeLabel,
                rawMessageText = null,
                directionHasSecondChild = false,
                firstDirectionChildClassName = null,
                visibleVoiceContainers = emptyList()
            )
        }
        val rawMessageText = rawMessageNodes
            .firstOrNull(AccessibilityNodeInfo::isVisibleToUser)
            ?.text
            ?.toString()
        val direction = record.findAccessibilityNodeInfosByViewId(DIRECTION_VIEW_ID)
            .firstOrNull(AccessibilityNodeInfo::isVisibleToUser)
        val voiceContainers = record.findAccessibilityNodeInfosByViewId(VOICE_CONTAINER_VIEW_ID)
            .filter(AccessibilityNodeInfo::isVisibleToUser)
            .map(::snapshotVoiceContainer)
        return WeChatRecordNodeSnapshot(
            timeLabel = timeLabel,
            rawMessageText = rawMessageText,
            directionHasSecondChild = direction?.getChild(1) != null,
            firstDirectionChildClassName = direction?.getChild(0)?.className?.toString(),
            visibleVoiceContainers = voiceContainers
        )
    }

    private fun snapshotVoiceContainer(node: AccessibilityNodeInfo): WeChatVoiceContainerSnapshot {
        val transcriptNodes = node.findAccessibilityNodeInfosByViewId(TRANSCRIPT_TEXT_VIEW_ID)
        return WeChatVoiceContainerSnapshot(
            transcriptNodeTexts = transcriptNodes.flatMap { transcriptNode ->
                listOfNotNull(
                    transcriptNode.text?.toString(),
                    transcriptNode.contentDescription?.toString()
                )
            },
            visibleTranscriptNodeCount = transcriptNodes.size,
            boundsInWindow = readNodeBounds(node)
        )
    }

    private fun readNodeBounds(node: AccessibilityNodeInfo): WeChatWindowBounds {
        val bounds = Rect()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            readBoundsInWindowApi34(node, bounds)
        } else {
            node.getBoundsInScreen(bounds)
        }
        return WeChatWindowBounds(bounds.left, bounds.top, bounds.right, bounds.bottom)
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun readBoundsInWindowApi34(node: AccessibilityNodeInfo, bounds: Rect) {
        node.getBoundsInWindow(bounds)
    }

    private companion object {
        const val MESSAGE_CONTAINER_VIEW_ID = "com.tencent.mm:id/bn1"
        const val TIME_LABEL_VIEW_ID = "com.tencent.mm:id/br1"
        const val MESSAGE_TEXT_VIEW_ID = "com.tencent.mm:id/bkl"
        const val DIRECTION_VIEW_ID = "com.tencent.mm:id/bkj"
        const val TRANSCRIPT_TEXT_VIEW_ID = "com.tencent.mm:id/brv"
        const val VOICE_CONTAINER_VIEW_ID = "com.tencent.mm:id/bru"
    }
}

/** A callback success arriving after cancellation is released immediately. */
internal suspend fun <T> awaitOwnedCallback(
    release: (T) -> Unit,
    register: (onSuccess: (T) -> Unit, onFailure: (Throwable) -> Unit) -> Unit
): T = suspendCancellableCoroutine { continuation ->
    val callbackCompleted = AtomicBoolean(false)
    val onSuccess: (T) -> Unit = { value ->
        if (callbackCompleted.compareAndSet(false, true)) {
            continuation.resume(value) {
                runCatching { release(value) }
            }
        } else {
            runCatching { release(value) }
        }
    }
    val onFailure: (Throwable) -> Unit = { error ->
        if (callbackCompleted.compareAndSet(false, true)) {
            runCatching { continuation.resumeWithException(error) }
        }
    }
    try {
        register(onSuccess, onFailure)
    } catch (error: Throwable) {
        onFailure(error)
    }
}

internal data class WeChatWindowImage(
    val jpegBytes: ByteArray,
    val width: Int,
    val height: Int
)

internal fun interface WeChatWindowImageCapture : Closeable {
    suspend fun capture(windowId: Int): WeChatWindowImage

    override fun close() = Unit
}

internal fun interface AndroidBitmapJpegCompressor {
    fun compress(bitmap: Bitmap, quality: Int, output: OutputStream): Boolean
}

/** Encodes an owned Bitmap and always releases it before returning or throwing. */
internal class WeChatWindowJpegEncoder(
    private val compressor: AndroidBitmapJpegCompressor = AndroidBitmapJpegCompressor {
            bitmap,
            quality,
            output ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
    }
) {
    fun encodeAndRecycle(source: Bitmap): WeChatWindowImage {
        var working = source
        try {
            val pixels = source.width.toLong() * source.height.toLong()
            if (pixels > MAX_PIXELS) {
                val scale = sqrt(MAX_PIXELS.toDouble() / pixels.toDouble())
                val targetWidth = floor(source.width * scale).toInt().coerceAtLeast(1)
                val targetHeight = floor(source.height * scale).toInt().coerceAtLeast(1)
                working = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
            }

            val quality85 = encode(working, PRIMARY_QUALITY)
            val bytes = if (quality85.size <= MAX_BYTES) {
                quality85
            } else {
                encode(working, RETRY_QUALITY).also { retry ->
                    check(retry.size <= MAX_BYTES) { "Window screenshot exceeds 4 MiB" }
                }
            }
            check(bytes.isNotEmpty()) { "Window screenshot compression returned no data" }
            return WeChatWindowImage(bytes, working.width, working.height)
        } finally {
            if (working !== source && !working.isRecycled) working.recycle()
            if (!source.isRecycled) source.recycle()
        }
    }

    private fun encode(bitmap: Bitmap, quality: Int): ByteArray {
        val output = ByteArrayOutputStream()
        check(compressor.compress(bitmap, quality, output)) {
            "Window screenshot JPEG compression failed"
        }
        return output.toByteArray()
    }

    companion object {
        const val MAX_PIXELS = 4_000_000
        const val MAX_BYTES = 4 * 1024 * 1024
        private const val PRIMARY_QUALITY = 85
        private const val RETRY_QUALITY = 70
    }
}

/** Android 14 window-only screenshot adapter. The image stays in memory and is never cached. */
internal class AndroidWeChatWindowImageCapture(
    private val service: AccessibilityService,
    private val callbackExecutor: Executor = BackgroundExecutor,
    private val elapsedRealtimeMillis: () -> Long = SystemClock::elapsedRealtime,
    private val encoder: WeChatWindowJpegEncoder = WeChatWindowJpegEncoder()
) : WeChatWindowImageCapture {
    private val screenshotMutex = Mutex()
    private var lastScreenshotAttemptMillis: Long? = null

    override suspend fun capture(windowId: Int): WeChatWindowImage {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            throw UnsupportedOperationException("Window screenshots require Android 14 or newer")
        }
        return captureApi34(windowId)
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private suspend fun captureApi34(windowId: Int): WeChatWindowImage = screenshotMutex.withLock {
        check(
            ((service.serviceInfo?.capabilities ?: 0) and
                AccessibilityServiceInfo.CAPABILITY_CAN_TAKE_SCREENSHOT) != 0
        ) { "Accessibility screenshot capability is unavailable" }
        require(windowId >= 0)
        val targetWindowBounds = service.windows
            .firstOrNull { window -> window.id == windowId }
            ?.let { window ->
                val bounds = Rect()
                window.getBoundsInScreen(bounds)
                WeChatWindowBounds(bounds.left, bounds.top, bounds.right, bounds.bottom)
            }
            ?: error("Target accessibility window is unavailable")
        val bitmap = captureWindowWithOneRateLimitRetry(windowId)
        try {
            check(isCompatibleWindowBitmap(targetWindowBounds, bitmap.width, bitmap.height)) {
                "Window screenshot dimensions changed"
            }
            withContext(Dispatchers.Default) { encoder.encodeAndRecycle(bitmap) }
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private suspend fun captureWindowWithOneRateLimitRetry(windowId: Int): Bitmap {
        awaitMinimumInterval()
        return try {
            captureWindowOnce(windowId)
        } catch (failure: ScreenshotFailure) {
            if (failure.errorCode != AccessibilityService.ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT) {
                throw failure
            }
            awaitMinimumInterval()
            captureWindowOnce(windowId)
        }
    }

    private suspend fun awaitMinimumInterval() {
        val previousAttempt = lastScreenshotAttemptMillis
        if (previousAttempt != null) {
            val remaining = MIN_SCREENSHOT_INTERVAL_MILLIS -
                (elapsedRealtimeMillis() - previousAttempt)
            if (remaining > 0) delay(remaining)
        }
        lastScreenshotAttemptMillis = elapsedRealtimeMillis()
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private suspend fun captureWindowOnce(windowId: Int): Bitmap = awaitOwnedCallback(
        release = Bitmap::recycle,
        register = { onSuccess, onFailure ->
            service.takeScreenshotOfWindow(
                windowId,
                callbackExecutor,
                object : AccessibilityService.TakeScreenshotCallback {
                    override fun onSuccess(
                        screenshotResult: AccessibilityService.ScreenshotResult
                    ) {
                        val bitmap = try {
                            copyScreenshotBitmap(screenshotResult)
                        } catch (error: Throwable) {
                            onFailure(error)
                            return
                        }
                        onSuccess(bitmap)
                    }

                    override fun onFailure(errorCode: Int) {
                        onFailure(ScreenshotFailure(errorCode))
                    }
                }
            )
        }
    )

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun copyScreenshotBitmap(
        screenshotResult: AccessibilityService.ScreenshotResult
    ): Bitmap {
        val hardwareBuffer = screenshotResult.hardwareBuffer
        return try {
            val hardwareBitmap = checkNotNull(
                Bitmap.wrapHardwareBuffer(hardwareBuffer, screenshotResult.colorSpace)
            )
            try {
                checkNotNull(hardwareBitmap.copy(Bitmap.Config.ARGB_8888, false))
            } finally {
                hardwareBitmap.recycle()
            }
        } finally {
            hardwareBuffer.close()
        }
    }

    private class ScreenshotFailure(val errorCode: Int) :
        IllegalStateException("Window screenshot failed: $errorCode")

    private object BackgroundExecutor : Executor {
        override fun execute(command: Runnable) {
            Dispatchers.Default.dispatch(EmptyCoroutineContext, command)
        }
    }

    private companion object {
        const val MIN_SCREENSHOT_INTERVAL_MILLIS = 500L
    }
}
