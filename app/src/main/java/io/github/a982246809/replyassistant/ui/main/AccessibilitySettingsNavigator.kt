package io.github.a982246809.replyassistant.ui.main

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import io.github.a982246809.replyassistant.accessibility.ReplyAssistantService

internal const val ACCESSIBILITY_DETAILS_SETTINGS_ACTION =
    "android.settings.ACCESSIBILITY_DETAILS_SETTINGS"

internal enum class AccessibilitySettingsNavigationResult {
    SERVICE_DETAILS_OPENED,
    GENERAL_SETTINGS_OPENED,
    UNAVAILABLE
}

internal data class AccessibilitySettingsNavigationOutcome(
    val result: AccessibilitySettingsNavigationResult,
    val message: String? = null
)

internal object AccessibilitySettingsNavigator {
    fun open(context: Context): AccessibilitySettingsNavigationOutcome {
        val serviceDetailsIntent = Intent(ACCESSIBILITY_DETAILS_SETTINGS_ACTION).apply {
            putExtra(
                Intent.EXTRA_COMPONENT_NAME,
                ComponentName(context.packageName, ReplyAssistantService::class.java.name)
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (start(context, serviceDetailsIntent)) {
            return AccessibilitySettingsNavigationOutcome(
                AccessibilitySettingsNavigationResult.SERVICE_DETAILS_OPENED
            )
        }

        val generalSettingsIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (start(context, generalSettingsIntent)) {
            return AccessibilitySettingsNavigationOutcome(
                result = AccessibilitySettingsNavigationResult.GENERAL_SETTINGS_OPENED,
                message = "未能直达，请按页面提示找到本应用并开启。"
            )
        }

        return AccessibilitySettingsNavigationOutcome(
            result = AccessibilitySettingsNavigationResult.UNAVAILABLE,
            message = "暂时无法打开无障碍设置，请在系统设置中手动开启。"
        )
    }

    private fun start(context: Context, intent: Intent): Boolean = runCatching {
        context.startActivity(intent)
    }.isSuccess
}
