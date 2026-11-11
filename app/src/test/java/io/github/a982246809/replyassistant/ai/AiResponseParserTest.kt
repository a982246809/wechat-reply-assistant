package io.github.a982246809.replyassistant.ai

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AiResponseParserTest {
    private val parser = AiResponseParser(Json { ignoreUnknownKeys = true })

    @Test
    fun `parses fenced json array and removes exclamation marks`() {
        val result = parser.parse(
            """
            ```json
            ["好呀！", " 我在路上了 ", ""]
            ```
            """.trimIndent(),
            replyCount = 3
        )
        assertEquals(listOf("好呀", "我在路上了"), result)
    }

    @Test
    fun `parses object choices and limits count`() {
        val result = parser.parse(
            """{"choices":["一","二","三"]}""",
            replyCount = 2
        )
        assertEquals(listOf("一", "二"), result)
    }

    @Test
    fun `parses choices with message objects`() {
        val result = parser.parse(
            """{"choices":[{"message":{"content":"在的"}}]}""",
            replyCount = 3
        )
        assertEquals(listOf("在的"), result)
    }

    @Test
    fun `maps unreadable vision response`() {
        assertThrows(AiProviderFailure.VisionUnreadable::class.java) {
            parser.parse("""{"error":"VISION_CONTENT_UNREADABLE"}""", 3)
        }
    }

    @Test
    fun `rejects empty or invalid response`() {
        assertThrows(AiProviderFailure.InvalidResponse::class.java) {
            parser.parse("不是 JSON", 3)
        }
        assertThrows(AiProviderFailure.InvalidResponse::class.java) {
            parser.parse("[]", 3)
        }
    }
}
