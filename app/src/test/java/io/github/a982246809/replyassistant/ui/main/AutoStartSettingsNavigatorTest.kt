package io.github.a982246809.replyassistant.ui.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoStartSettingsNavigatorTest {
    @Test
    fun `only successful settings navigation records settings opened`() {
        assertTrue(shouldRecordAutoStartSettingsOpened(AutoStartNavigationResult.VENDOR_SETTINGS_OPENED))
        assertTrue(shouldRecordAutoStartSettingsOpened(AutoStartNavigationResult.APP_DETAILS_OPENED))
        assertFalse(shouldRecordAutoStartSettingsOpened(AutoStartNavigationResult.UNAVAILABLE))
    }

    @Test
    fun `小米设备优先使用自启动管理入口`() {
        val components = vendorAutoStartComponents("Xiaomi")

        assertEquals("com.miui.securitycenter", components.first().packageName)
        assertTrue(components.first().className.contains("AutoStartManagementActivity"))
    }

    @Test
    fun `常见国产厂商均提供候选入口`() {
        listOf("Huawei", "HONOR", "OPPO", "OnePlus", "realme", "vivo", "iQOO", "Meizu")
            .forEach { manufacturer ->
                assertTrue(
                    "$manufacturer 应提供自启动候选入口",
                    vendorAutoStartComponents(manufacturer).isNotEmpty()
                )
            }
    }

    @Test
    fun `未知厂商使用通用降级说明`() {
        assertTrue(vendorAutoStartComponents("Google").isEmpty())
        assertEquals(
            "请在应用详情或系统管家中允许本应用自启动。",
            autoStartManualInstruction("Google")
        )
    }
}
