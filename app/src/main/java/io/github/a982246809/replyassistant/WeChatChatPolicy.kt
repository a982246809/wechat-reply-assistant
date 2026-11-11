package io.github.a982246809.replyassistant

import java.text.Normalizer

private val weChatGroupTitlePattern = Regex(""".*\(\d+\)$""")
private val unicodeFormattingPattern = Regex("""\p{Cf}""")
private val weChatNonChatTitles = setOf(
    "折叠的聊天",
    "文件传输助手",
    "flomo 浮墨笔记"
)

/**
 * 微信聊天标题的唯一规范化入口。
 *
 * 这里把兼容字符和无显示效果的 Unicode 格式字符视作同一个标题，
 * 避免同一个会话绕过用户设置。
 */
internal fun canonicalizeWeChatChatTitle(title: String): String =
    Normalizer.normalize(title, Normalizer.Form.NFKC)
        .replace(unicodeFormattingPattern, "")
        .trim()

internal fun isWeChatChatInterfaceTitle(title: String?): Boolean {
    val normalizedTitle = title
        ?.let(::canonicalizeWeChatChatTitle)
        ?.takeIf(String::isNotEmpty)
        ?: return false
    return normalizedTitle !in weChatNonChatTitles
}

internal fun isWeChatDirectChatTitle(title: String?): Boolean {
    val normalizedTitle = title
        ?.let(::canonicalizeWeChatChatTitle)
        ?.takeIf(String::isNotEmpty)
        ?: return false
    return normalizedTitle !in weChatNonChatTitles &&
        !weChatGroupTitlePattern.matches(normalizedTitle)
}

internal fun isWeChatGroupChatTitle(title: String): Boolean =
    weChatGroupTitlePattern.matches(canonicalizeWeChatChatTitle(title))
