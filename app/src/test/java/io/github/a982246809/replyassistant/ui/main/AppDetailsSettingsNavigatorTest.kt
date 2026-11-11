package io.github.a982246809.replyassistant.ui.main

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AppDetailsSettingsNavigatorTest {
    @Test
    fun `打开当前应用详情并附带新任务标记`() {
        val context: Context = RuntimeEnvironment.getApplication()

        assertTrue(AppDetailsSettingsNavigator.open(context))

        val intent = shadowOf(context.applicationContext as android.app.Application)
            .nextStartedActivity
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
        assertEquals("package:${context.packageName}", intent.dataString)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun `系统拒绝打开应用详情时返回失败`() {
        val base: Context = RuntimeEnvironment.getApplication()
        val rejectingContext = object : ContextWrapper(base) {
            override fun startActivity(intent: Intent?) {
                throw IllegalStateException("settings unavailable")
            }
        }

        assertFalse(AppDetailsSettingsNavigator.open(rejectingContext))
    }
}
