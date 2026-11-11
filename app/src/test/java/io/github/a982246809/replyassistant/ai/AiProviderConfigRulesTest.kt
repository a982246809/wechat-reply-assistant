package io.github.a982246809.replyassistant.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiProviderConfigRulesTest {
    @Test
    fun `normalizes trailing slash and endpoint suffix`() {
        assertEquals(
            "https://api.example.com/v1",
            AiProviderConfigRules.normalizeBaseUrl(
                " https://api.example.com/v1/chat/completions/ "
            )
        )
        assertEquals(
            "http://192.168.1.5:11434/v1",
            AiProviderConfigRules.normalizeBaseUrl("http://192.168.1.5:11434/v1/models")
        )
    }

    @Test
    fun `rejects unsupported url forms`() {
        listOf(
            "",
            "ftp://example.com/v1",
            "https://user:pass@example.com/v1",
            "https://example.com/v1?key=secret"
        ).forEach { value ->
            assertTrue(runCatching { AiProviderConfigRules.normalizeBaseUrl(value) }.isFailure)
        }
    }

    @Test
    fun `validates required model and detects cleartext http`() {
        val errors = AiProviderConfigRules.validate(
            AiProviderConfigInput(
                baseUrl = "http://localhost:11434/v1",
                apiKey = "",
                textModel = "",
                visionModel = null
            )
        )
        assertEquals(listOf("请填写主模型名称"), errors)
        assertTrue(AiProviderConfigRules.isCleartextHttp("http://localhost:11434/v1"))
        assertFalse(AiProviderConfigRules.isCleartextHttp("https://api.example.com/v1"))
    }
}
