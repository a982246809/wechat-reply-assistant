package io.github.a982246809.replyassistant.ui.main

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import java.util.Locale

enum class AutoStartNavigationResult {
    VENDOR_SETTINGS_OPENED,
    APP_DETAILS_OPENED,
    UNAVAILABLE
}

data class AutoStartNavigationOutcome(
    val result: AutoStartNavigationResult,
    val message: String
)

data class AutoStartComponentSpec(
    val packageName: String,
    val className: String
)

object AutoStartSettingsNavigator {
    fun open(context: Context): AutoStartNavigationOutcome {
        vendorAutoStartComponents(android.os.Build.MANUFACTURER).forEach { candidate ->
            val intent = Intent().apply {
                component = ComponentName(candidate.packageName, candidate.className)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (canOpen(context, intent) && start(context, intent)) {
                return AutoStartNavigationOutcome(
                    result = AutoStartNavigationResult.VENDOR_SETTINGS_OPENED,
                    message = autoStartManualInstruction(android.os.Build.MANUFACTURER)
                )
            }
        }

        val appDetailsIntent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (canOpen(context, appDetailsIntent) && start(context, appDetailsIntent)) {
            return AutoStartNavigationOutcome(
                result = AutoStartNavigationResult.APP_DETAILS_OPENED,
                message = "未找到专用入口。请在应用详情或系统管家中允许本应用自启动。"
            )
        }

        return AutoStartNavigationOutcome(
            result = AutoStartNavigationResult.UNAVAILABLE,
            message = "暂时无法打开系统设置，请在系统管家中手动允许本应用自启动。"
        )
    }

    private fun canOpen(context: Context, intent: Intent): Boolean = runCatching {
        intent.resolveActivity(context.packageManager) != null
    }.getOrDefault(false)

    private fun start(context: Context, intent: Intent): Boolean = runCatching {
        context.startActivity(intent)
    }.isSuccess
}

internal fun vendorAutoStartComponents(manufacturer: String): List<AutoStartComponentSpec> {
    return when (normalizeManufacturer(manufacturer)) {
        "xiaomi", "redmi" -> listOf(
            AutoStartComponentSpec(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
        )

        "huawei" -> listOf(
            AutoStartComponentSpec(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            ),
            AutoStartComponentSpec(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.optimize.process.ProtectActivity"
            )
        )

        "honor" -> listOf(
            AutoStartComponentSpec(
                "com.hihonor.systemmanager",
                "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            )
        )

        "oppo", "oneplus", "realme" -> listOf(
            AutoStartComponentSpec(
                "com.oplus.safecenter",
                "com.oplus.safecenter.permission.startupapp.StartupAppListActivity"
            ),
            AutoStartComponentSpec(
                "com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.StartupAppListActivity"
            )
        )

        "vivo", "iqoo" -> listOf(
            AutoStartComponentSpec(
                "com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
            ),
            AutoStartComponentSpec(
                "com.iqoo.secure",
                "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"
            )
        )

        "meizu" -> listOf(
            AutoStartComponentSpec(
                "com.meizu.safe",
                "com.meizu.safe.permission.SmartBGActivity"
            )
        )

        else -> emptyList()
    }
}

internal fun autoStartManualInstruction(manufacturer: String): String {
    return when (normalizeManufacturer(manufacturer)) {
        "xiaomi", "redmi" -> "请在自启动管理中允许本应用自启动。"
        "huawei", "honor" -> "请在应用启动管理中将本应用设为允许自动管理或手动允许自启动。"
        "oppo", "oneplus", "realme" -> "请在自启动管理中允许本应用自启动和后台运行。"
        "vivo", "iqoo" -> "请在权限管理的自启动设置中允许本应用。"
        "meizu" -> "请在后台管理中允许本应用保持运行。"
        else -> "请在应用详情或系统管家中允许本应用自启动。"
    }
}

private fun normalizeManufacturer(manufacturer: String): String =
    manufacturer.trim().lowercase(Locale.ROOT)
