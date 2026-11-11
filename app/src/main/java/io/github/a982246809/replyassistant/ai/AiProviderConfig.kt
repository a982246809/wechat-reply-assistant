package io.github.a982246809.replyassistant.ai

import java.net.URI

data class AiProviderConfig(
    val baseUrl: String,
    val textModel: String,
    val visionModel: String?,
    val hasApiKey: Boolean
) {
    val effectiveVisionModel: String get() = visionModel ?: textModel
    val chatCompletionsUrl: String
        get() = "${AiProviderConfigRules.normalizeBaseUrl(baseUrl)}/chat/completions"
    val modelsUrl: String
        get() = "${AiProviderConfigRules.normalizeBaseUrl(baseUrl)}/models"
    val usesCleartextHttp: Boolean
        get() = AiProviderConfigRules.isCleartextHttp(baseUrl)
}

data class AiProviderConfigInput(
    val baseUrl: String,
    val apiKey: String?,
    val textModel: String,
    val visionModel: String?
)

sealed interface AiProviderState {
    data object Loading : AiProviderState
    data object Missing : AiProviderState
    data class Ready(val config: AiProviderConfig) : AiProviderState
}

object AiProviderConfigRules {
    private val endpointSuffixes = listOf("/chat/completions", "/models")

    fun normalizeBaseUrl(raw: String): String {
        val trimmed = raw.trim().trimEnd('/')
        require(trimmed.isNotEmpty()) { "请填写 API Base URL" }
        val uri = runCatching { URI(trimmed) }.getOrElse {
            throw IllegalArgumentException("API Base URL 格式无效")
        }
        require(uri.scheme.equals("https", true) || uri.scheme.equals("http", true)) {
            "API Base URL 必须使用 http:// 或 https://"
        }
        require(!uri.host.isNullOrBlank()) { "API Base URL 缺少主机名" }
        require(uri.userInfo == null) { "API Base URL 不能包含用户名或密码" }
        require(uri.query == null && uri.fragment == null) {
            "API Base URL 不能包含查询参数或片段"
        }
        val withoutEndpoint = endpointSuffixes.firstOrNull {
            trimmed.endsWith(it, ignoreCase = true)
        }?.let { suffix -> trimmed.dropLast(suffix.length) } ?: trimmed
        return withoutEndpoint.trimEnd('/')
    }

    fun normalizeInput(input: AiProviderConfigInput): AiProviderConfigInput =
        input.copy(
            baseUrl = normalizeBaseUrl(input.baseUrl),
            apiKey = input.apiKey?.trim()?.takeIf(String::isNotEmpty),
            textModel = input.textModel.trim(),
            visionModel = input.visionModel?.trim()?.takeIf(String::isNotEmpty)
        )

    fun validate(input: AiProviderConfigInput): List<String> = buildList {
        runCatching { normalizeBaseUrl(input.baseUrl) }
            .onFailure { add(it.message ?: "API Base URL 格式无效") }
        if (input.textModel.isBlank()) add("请填写主模型名称")
    }

    fun isCleartextHttp(raw: String): Boolean =
        runCatching { URI(raw.trim()).scheme.equals("http", ignoreCase = true) }
            .getOrDefault(false)
}
