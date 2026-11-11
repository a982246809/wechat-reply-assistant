package io.github.a982246809.replyassistant.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityServiceCheckerTest {

    @Test
    fun `无障碍总开关关闭时应返回false`() {
        val enabled = isAccessibilityServiceEnabled(
            accessibilityEnabled = false,
            enabledServices = "io.github.a982246809.replyassistant/io.github.a982246809.replyassistant.accessibility.ReplyAssistantService",
            packageName = "io.github.a982246809.replyassistant",
            serviceClassName = "io.github.a982246809.replyassistant.accessibility.ReplyAssistantService"
        )

        assertFalse(enabled)
    }

    @Test
    fun `服务列表包含目标服务时应返回true`() {
        val enabled = isAccessibilityServiceEnabled(
            accessibilityEnabled = true,
            enabledServices = "io.github.a982246809.replyassistant/io.github.a982246809.replyassistant.accessibility.ReplyAssistantService:other/pkg.Service",
            packageName = "io.github.a982246809.replyassistant",
            serviceClassName = "io.github.a982246809.replyassistant.accessibility.ReplyAssistantService"
        )

        assertTrue(enabled)
    }

    @Test
    fun `目标服务类名使用相对写法时应返回true`() {
        val enabled = isAccessibilityServiceEnabled(
            accessibilityEnabled = true,
            enabledServices = "io.github.a982246809.replyassistant/.accessibility.ReplyAssistantService",
            packageName = "io.github.a982246809.replyassistant",
            serviceClassName = ".accessibility.ReplyAssistantService"
        )

        assertTrue(enabled)
    }

    @Test
    fun `服务列表不包含目标服务时应返回false`() {
        val enabled = isAccessibilityServiceEnabled(
            accessibilityEnabled = true,
            enabledServices = "com.other.app/io.github.a982246809.replyassistant.accessibility.ReplyAssistantService",
            packageName = "io.github.a982246809.replyassistant",
            serviceClassName = "io.github.a982246809.replyassistant.accessibility.ReplyAssistantService"
        )

        assertFalse(enabled)
    }
}
