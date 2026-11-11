package io.github.a982246809.replyassistant

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeChatChatPolicyTest {

    @Test
    fun `系统入口标题标准化后完整匹配时不应识别为单聊`() {
        listOf(
            "折叠的聊天",
            " 文件传输助手 ",
            "flomo\u200E 浮墨笔记"
        ).forEach { title ->
            assertFalse(isWeChatDirectChatTitle(title))
            assertFalse(isWeChatChatInterfaceTitle(title))
        }
    }

    @Test
    fun `仅包含系统入口文字的标题仍应识别为单聊`() {
        assertTrue(isWeChatDirectChatTitle("折叠的聊天备份"))
        assertTrue(isWeChatDirectChatTitle("文件传输助手小王"))
        assertTrue(isWeChatDirectChatTitle("flomo 浮墨笔记（工作）"))
        assertTrue(isWeChatDirectChatTitle("张三"))
        assertTrue(isWeChatChatInterfaceTitle("文件传输助手小王"))
        assertTrue(isWeChatChatInterfaceTitle("张三"))
    }

    @Test
    fun `群聊仍属于聊天界面但不属于单聊`() {
        assertTrue(isWeChatChatInterfaceTitle("项目群(12)"))
        assertFalse(isWeChatDirectChatTitle("项目群(12)"))
    }

    @Test
    fun `人数后缀及无障碍字符变体应识别为群聊`() {
        assertTrue(isWeChatGroupChatTitle("项目群(12)"))
        assertTrue(isWeChatGroupChatTitle("  项目群(12)  "))
        assertTrue(isWeChatGroupChatTitle("民治羽毛球大队(434)"))
        assertTrue(isWeChatGroupChatTitle("民治羽毛球大队（４３４）"))
        assertTrue(isWeChatGroupChatTitle("民治羽毛球大队(434)\u200E"))
    }

    @Test
    fun `非纯数字人数后缀不应识别为群聊`() {
        assertFalse(isWeChatGroupChatTitle("张三"))
        assertFalse(isWeChatGroupChatTitle("项目群(成员12)"))
        assertFalse(isWeChatGroupChatTitle("项目群(12)备注"))
    }
}
