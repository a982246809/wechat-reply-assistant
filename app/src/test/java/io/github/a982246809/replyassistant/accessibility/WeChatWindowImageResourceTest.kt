package io.github.a982246809.replyassistant.accessibility

import android.app.Application
import android.graphics.Bitmap
import java.io.OutputStream
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class WeChatWindowImageResourceTest {
    @Test
    fun `截图尺寸必须匹配目标微信窗口而不是整个显示屏`() {
        val targetWindow = WeChatWindowBounds(left = 0, top = 120, right = 1080, bottom = 2280)

        assertTrue(isCompatibleWindowBitmap(targetWindow, bitmapWidth = 1080, bitmapHeight = 2160))
        assertTrue(isCompatibleWindowBitmap(targetWindow, bitmapWidth = 1081, bitmapHeight = 2159))
        assertTrue(!isCompatibleWindowBitmap(targetWindow, bitmapWidth = 1080, bitmapHeight = 2400))
    }

    @Test
    fun `截图回调晚于协程取消时立即释放 Bitmap`() = runTest {
        lateinit var succeed: (FakeResource) -> Unit
        val waiter = launch {
            awaitOwnedCallback(
                release = FakeResource::close,
                register = { onSuccess, _ -> succeed = onSuccess }
            )
        }
        runCurrent()

        waiter.cancelAndJoin()
        val lateResource = FakeResource()
        succeed(lateResource)

        assertEquals(1, lateResource.closeCount)
    }

    @Test
    fun `超过四百万像素会等比缩放且原图与缩放图最终释放`() {
        val compressedBitmaps = mutableListOf<Bitmap>()
        val source = Bitmap.createBitmap(2500, 2000, Bitmap.Config.ARGB_8888)
        val encoder = WeChatWindowJpegEncoder(
            compressor = RecordingCompressor { _, output ->
                output.write(byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 0xd9.toByte()))
                true
            }.also { it.bitmaps = compressedBitmaps }
        )

        val image = encoder.encodeAndRecycle(source)

        assertTrue(image.width * image.height <= WeChatWindowJpegEncoder.MAX_PIXELS)
        assertTrue(source.isRecycled)
        assertTrue(compressedBitmaps.single().isRecycled)
    }

    @Test
    fun `质量 85 超限时只以质量 70 重试一次`() {
        val qualities = mutableListOf<Int>()
        val source = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888)
        val encoder = WeChatWindowJpegEncoder(
            compressor = AndroidBitmapJpegCompressor { _, quality, output ->
                qualities += quality
                val size = if (quality == 85) {
                    WeChatWindowJpegEncoder.MAX_BYTES + 1
                } else {
                    4
                }
                output.write(ByteArray(size))
                true
            }
        )

        val image = encoder.encodeAndRecycle(source)

        assertEquals(listOf(85, 70), qualities)
        assertEquals(4, image.jpegBytes.size)
        assertTrue(source.isRecycled)
    }

    @Test
    fun `质量 70 仍超限时失败并释放 Bitmap`() {
        val source = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888)
        val encoder = WeChatWindowJpegEncoder(
            compressor = AndroidBitmapJpegCompressor { _, _, output ->
                output.write(ByteArray(WeChatWindowJpegEncoder.MAX_BYTES + 1))
                true
            }
        )

        try {
            encoder.encodeAndRecycle(source)
            fail("超过 4 MiB 应失败")
        } catch (_: IllegalStateException) {
            // Expected.
        }

        assertTrue(source.isRecycled)
    }

    @Test
    fun `Bitmap 压缩失败时不返回空图且最终释放`() {
        val source = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888)
        val encoder = WeChatWindowJpegEncoder(
            compressor = AndroidBitmapJpegCompressor { _, _, _ -> false }
        )

        try {
            encoder.encodeAndRecycle(source)
            fail("压缩失败应抛出异常")
        } catch (_: IllegalStateException) {
            // Expected.
        }

        assertTrue(source.isRecycled)
    }

    private class RecordingCompressor(
        private val compress: (quality: Int, output: OutputStream) -> Boolean
    ) : AndroidBitmapJpegCompressor {
        lateinit var bitmaps: MutableList<Bitmap>

        override fun compress(bitmap: Bitmap, quality: Int, output: OutputStream): Boolean {
            bitmaps += bitmap
            return compress(quality, output)
        }
    }

    private class FakeResource : AutoCloseable {
        var closeCount = 0
            private set

        override fun close() {
            closeCount += 1
        }
    }
}
