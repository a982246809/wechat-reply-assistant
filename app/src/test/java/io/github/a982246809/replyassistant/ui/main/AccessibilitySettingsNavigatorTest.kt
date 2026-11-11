package io.github.a982246809.replyassistant.ui.main

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import io.github.a982246809.replyassistant.accessibility.ReplyAssistantService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AccessibilitySettingsNavigatorTest {
    @Test
    fun `专属入口可用时直达当前构建的无障碍服务详情`() {
        val context = RecordingContext(RuntimeEnvironment.getApplication())

        val outcome = AccessibilitySettingsNavigator.open(context)

        assertEquals(
            AccessibilitySettingsNavigationResult.SERVICE_DETAILS_OPENED,
            outcome.result
        )
        assertNull(outcome.message)
        assertEquals(1, context.startedIntents.size)
        val intent = context.startedIntents.single()
        assertEquals(ACCESSIBILITY_DETAILS_SETTINGS_ACTION, intent.action)
        assertEquals(
            ComponentName(context.packageName, ReplyAssistantService::class.java.name),
            intent.getParcelableExtra(Intent.EXTRA_COMPONENT_NAME)
        )
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun `专属入口启动失败时回退普通无障碍设置`() {
        val context = RecordingContext(
            RuntimeEnvironment.getApplication(),
            rejectedActions = setOf(ACCESSIBILITY_DETAILS_SETTINGS_ACTION)
        )

        val outcome = AccessibilitySettingsNavigator.open(context)

        assertEquals(
            AccessibilitySettingsNavigationResult.GENERAL_SETTINGS_OPENED,
            outcome.result
        )
        assertEquals(
            "未能直达，请按页面提示找到本应用并开启。",
            outcome.message
        )
        assertEquals(
            listOf(ACCESSIBILITY_DETAILS_SETTINGS_ACTION, Settings.ACTION_ACCESSIBILITY_SETTINGS),
            context.attemptedActions
        )
        assertEquals(Settings.ACTION_ACCESSIBILITY_SETTINGS, context.startedIntents.single().action)
    }

    @Test
    fun `两级设置入口都失败时返回不可用`() {
        val context = RecordingContext(
            RuntimeEnvironment.getApplication(),
            rejectedActions = setOf(
                ACCESSIBILITY_DETAILS_SETTINGS_ACTION,
                Settings.ACTION_ACCESSIBILITY_SETTINGS
            )
        )

        val outcome = AccessibilitySettingsNavigator.open(context)

        assertEquals(AccessibilitySettingsNavigationResult.UNAVAILABLE, outcome.result)
        assertEquals(
            "暂时无法打开无障碍设置，请在系统设置中手动开启。",
            outcome.message
        )
        assertTrue(context.startedIntents.isEmpty())
    }
}

private class RecordingContext(
    base: Context,
    private val rejectedActions: Set<String> = emptySet()
) : ContextWrapper(base) {
    val attemptedActions = mutableListOf<String?>()
    val startedIntents = mutableListOf<Intent>()

    override fun startActivity(intent: Intent?) {
        requireNotNull(intent)
        attemptedActions += intent.action
        if (intent.action in rejectedActions) {
            throw IllegalStateException("settings unavailable")
        }
        startedIntents += intent
    }
}
