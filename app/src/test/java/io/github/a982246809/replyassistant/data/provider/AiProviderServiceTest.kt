package io.github.a982246809.replyassistant.data.provider

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.github.a982246809.replyassistant.ai.AiProviderConfig
import io.github.a982246809.replyassistant.ai.AiProviderFailure
import io.github.a982246809.replyassistant.api.OpenAiCompatibleApi
import io.github.a982246809.replyassistant.dto.ChatCompletionMessage
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class AiProviderServiceTest {
    private lateinit var server: MockWebServer
    private lateinit var service: AiProviderService

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenAiCompatibleApi::class.java)
        service = AiProviderService(api, json)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `sends chat completion with bearer key`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"choices":[{"message":{"role":"assistant","content":"你好"}}]}"""
            )
        )
        val config = config()
        val result = service.complete(
            config = config,
            model = "test-model",
            messages = listOf(ChatCompletionMessage("user", JsonPrimitive("hi"))),
            apiKey = "secret-key"
        )

        assertTrue(result.isSuccess)
        assertEquals("你好", result.getOrThrow())
        val request = server.takeRequest()
        assertEquals("/v1/chat/completions", request.path)
        assertEquals("Bearer secret-key", request.getHeader("Authorization"))
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"model\":\"test-model\""))
        assertFalse(body.contains("\"stream\""))
    }

    @Test
    fun `omits authorization for unauthenticated local endpoint`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"data":[{"id":"local-model"}]}"""))
        val result = service.listModels(config(), apiKey = null)

        assertEquals(listOf("local-model"), result.getOrThrow())
        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `maps unauthorized response`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401))
        val result = service.complete(
            config = config(),
            model = "test-model",
            messages = listOf(ChatCompletionMessage("user", JsonPrimitive("hi"))),
            apiKey = "bad-key"
        )
        assertEquals(AiProviderFailure.Unauthorized, result.exceptionOrNull())
    }

    @Test
    fun `maps not found and rate limit responses`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404))
        server.enqueue(MockResponse().setResponseCode(429))

        val notFound = service.complete(
            config = config(),
            model = "test-model",
            messages = listOf(ChatCompletionMessage("user", JsonPrimitive("hi"))),
            apiKey = null
        )
        val rateLimited = service.complete(
            config = config(),
            model = "test-model",
            messages = listOf(ChatCompletionMessage("user", JsonPrimitive("hi"))),
            apiKey = null
        )

        assertEquals(AiProviderFailure.NotFound, notFound.exceptionOrNull())
        assertEquals(AiProviderFailure.RateLimited, rateLimited.exceptionOrNull())
    }

    @Test
    fun `maps vision unsupported response`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody("""{"error":{"message":"model does not support image input"}}""")
        )
        val result = service.complete(
            config = config(),
            model = "test-model",
            messages = listOf(ChatCompletionMessage("user", JsonPrimitive("hi"))),
            apiKey = null
        )

        assertEquals(AiProviderFailure.VisionUnsupported, result.exceptionOrNull())
    }

    @Test
    fun `redacts api key from provider error`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody("""{"error":{"message":"bad request for secret-key"}}""")
        )
        val result = service.complete(
            config = config(),
            model = "test-model",
            messages = listOf(ChatCompletionMessage("user", JsonPrimitive("hi"))),
            apiKey = "secret-key"
        )

        val failure = result.exceptionOrNull() as AiProviderFailure.InvalidConfiguration
        assertFalse(failure.message.orEmpty().contains("secret-key"))
        assertTrue(failure.message.orEmpty().contains("[REDACTED]"))
    }

    private fun config() = AiProviderConfig(
        baseUrl = server.url("/v1").toString().trimEnd('/'),
        textModel = "test-model",
        visionModel = null,
        hasApiKey = true
    )
}
