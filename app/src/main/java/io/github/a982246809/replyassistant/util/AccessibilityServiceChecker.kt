package io.github.a982246809.replyassistant.util

import android.content.Context
import android.provider.Settings

private const val ACCESSIBILITY_SERVICE_CLASS_NAME =
    "io.github.a982246809.replyassistant.accessibility.ReplyAssistantService"

/**
 * 读取系统设置，判断当前应用的无障碍服务是否已启用。
 */
fun isOurAccessibilityServiceEnabled(context: Context): Boolean {
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    )
    val accessibilityEnabled = Settings.Secure.getInt(
        context.contentResolver,
        Settings.Secure.ACCESSIBILITY_ENABLED,
        0
    ) == 1

    return isAccessibilityServiceEnabled(
        accessibilityEnabled = accessibilityEnabled,
        enabledServices = enabledServices,
        packageName = context.packageName,
        serviceClassName = ACCESSIBILITY_SERVICE_CLASS_NAME
    )
}

internal fun isAccessibilityServiceEnabled(
    accessibilityEnabled: Boolean,
    enabledServices: String?,
    packageName: String,
    serviceClassName: String
): Boolean {
    if (!accessibilityEnabled || enabledServices.isNullOrBlank()) {
        return false
    }

    val targetClassName = normalizeClassName(packageName, serviceClassName)
    val targetPackageName = packageName.lowercase()

    return enabledServices.split(':')
        .map { it.trim() }
        .any { component ->
            val slashIndex = component.indexOf('/')
            if (slashIndex <= 0 || slashIndex == component.lastIndex) {
                return@any false
            }

            val componentPackage = component.substring(0, slashIndex)
            val componentClass = component.substring(slashIndex + 1)
            val normalizedClass = normalizeClassName(componentPackage, componentClass)

            componentPackage.lowercase() == targetPackageName &&
                normalizedClass.equals(targetClassName, ignoreCase = true)
        }
}

private fun normalizeClassName(packageName: String, className: String): String {
    return if (className.startsWith(".")) {
        "$packageName$className"
    } else {
        className
    }
}
