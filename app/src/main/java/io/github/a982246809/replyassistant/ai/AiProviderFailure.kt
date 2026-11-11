package io.github.a982246809.replyassistant.ai

sealed class AiProviderFailure(message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause) {
    data object NotConfigured : AiProviderFailure("请先配置 AI 服务")
    class InvalidConfiguration(message: String) : AiProviderFailure(message)
    data object Unauthorized : AiProviderFailure("API Key 无效或无权限")
    data object NotFound : AiProviderFailure("API 地址或模型不存在")
    data object RateLimited : AiProviderFailure("模型请求过于频繁，请稍后再试")
    data object VisionUnsupported : AiProviderFailure("当前模型不支持图片输入")
    data object VisionUnreadable : AiProviderFailure("聊天截图无法清晰识别，请重试")
    data object InvalidResponse : AiProviderFailure("模型返回格式异常，请更换模型或检查提示词支持")
    class Network(message: String, override val cause: Throwable? = null) :
        AiProviderFailure(message, cause)
    class Server(message: String) : AiProviderFailure(message)
}
