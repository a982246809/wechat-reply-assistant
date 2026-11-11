package io.github.a982246809.replyassistant.ui.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.a982246809.replyassistant.ai.AiProviderConfig
import io.github.a982246809.replyassistant.ai.AiProviderConfigInput
import io.github.a982246809.replyassistant.ai.AiProviderConfigRules
import io.github.a982246809.replyassistant.data.provider.AiProviderConfigStore
import io.github.a982246809.replyassistant.data.provider.AiProviderService
import io.github.a982246809.replyassistant.data.provider.AiSecretStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiProviderSetupUiState(
    val loading: Boolean = true,
    val baseUrl: String = "",
    val apiKey: String = "",
    val textModel: String = "",
    val visionModel: String = "",
    val hasStoredKey: Boolean = false,
    val models: List<String> = emptyList(),
    val message: String? = null,
    val messageIsError: Boolean = false,
    val fetchingModels: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false
) {
    val isHttp: Boolean
        get() = AiProviderConfigRules.isCleartextHttp(baseUrl)
}

@HiltViewModel
class AiProviderSetupViewModel @Inject constructor(
    private val configStore: AiProviderConfigStore,
    private val providerService: AiProviderService,
    private val secretStore: AiSecretStore
) : ViewModel() {
    private val mutableState = MutableStateFlow(AiProviderSetupUiState())
    val state: StateFlow<AiProviderSetupUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val config = configStore.load()
            mutableState.update {
                it.copy(
                    loading = false,
                    baseUrl = config?.baseUrl.orEmpty(),
                    textModel = config?.textModel.orEmpty(),
                    visionModel = config?.visionModel.orEmpty(),
                    hasStoredKey = config?.hasApiKey == true
                )
            }
        }
    }

    fun setBaseUrl(value: String) = update { copy(baseUrl = value, message = null) }
    fun setApiKey(value: String) = update { copy(apiKey = value, message = null) }
    fun setTextModel(value: String) = update { copy(textModel = value, message = null) }
    fun setVisionModel(value: String) = update { copy(visionModel = value, message = null) }

    fun fetchModels() {
        val state = mutableState.value
        if (state.fetchingModels) return
        val errors = validate(state)
        if (errors.isNotEmpty()) {
            showErrors(errors)
            return
        }
        mutableState.update { it.copy(fetchingModels = true, message = null) }
        viewModelScope.launch {
            val config = state.toConfig()
            val key = state.apiKey.trim().takeIf(String::isNotEmpty) ?: secretStore.readApiKey()
            providerService.listModels(config, key)
                .onSuccess { models ->
                    mutableState.update {
                        it.copy(
                            fetchingModels = false,
                            models = models,
                            textModel = it.textModel.ifBlank { models.firstOrNull().orEmpty() },
                            message = if (models.isEmpty()) {
                                "服务未返回模型，请手动填写模型名"
                            } else {
                                "已获取 ${models.size} 个模型"
                            },
                            messageIsError = models.isEmpty()
                        )
                    }
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(
                            fetchingModels = false,
                            message = error.message ?: "获取模型失败",
                            messageIsError = true
                        )
                    }
                }
        }
    }

    fun save() {
        val state = mutableState.value
        if (state.saving) return
        val errors = validate(state)
        if (errors.isNotEmpty()) {
            showErrors(errors)
            return
        }
        mutableState.update { it.copy(saving = true, message = null) }
        viewModelScope.launch {
            val apiKey = when {
                state.apiKey.isNotBlank() -> state.apiKey.trim()
                state.hasStoredKey -> null
                else -> ""
            }
            runCatching {
                configStore.save(
                    AiProviderConfigInput(
                        baseUrl = state.baseUrl,
                        apiKey = apiKey,
                        textModel = state.textModel,
                        visionModel = state.visionModel
                    )
                )
            }.onSuccess {
                mutableState.update {
                    it.copy(
                        saving = false,
                        saved = true,
                        hasStoredKey = it.apiKey.isNotBlank() || it.hasStoredKey,
                        apiKey = "",
                        message = "配置已保存",
                        messageIsError = false
                    )
                }
            }.onFailure { error ->
                mutableState.update {
                    it.copy(
                        saving = false,
                        message = error.message ?: "保存失败",
                        messageIsError = true
                    )
                }
            }
        }
    }

    fun clearApiKey() {
        viewModelScope.launch {
            configStore.clearApiKey()
            mutableState.update {
                it.copy(
                    apiKey = "",
                    hasStoredKey = false,
                    message = "API Key 已清除",
                    messageIsError = false
                )
            }
        }
    }

    fun consumeSaved(): Boolean {
        if (!mutableState.value.saved) return false
        mutableState.update { it.copy(saved = false) }
        return true
    }

    private fun validate(state: AiProviderSetupUiState): List<String> =
        AiProviderConfigRules.validate(
            AiProviderConfigInput(
                baseUrl = state.baseUrl,
                apiKey = state.apiKey,
                textModel = state.textModel,
                visionModel = state.visionModel
            )
        )

    private fun AiProviderSetupUiState.toConfig(): AiProviderConfig = AiProviderConfig(
        baseUrl = AiProviderConfigRules.normalizeBaseUrl(baseUrl),
        textModel = textModel.trim(),
        visionModel = visionModel.trim().takeIf(String::isNotEmpty),
        hasApiKey = hasStoredKey || apiKey.isNotBlank()
    )

    private fun showErrors(errors: List<String>) {
        mutableState.update {
            it.copy(message = errors.joinToString("\n"), messageIsError = true)
        }
    }

    private fun update(block: AiProviderSetupUiState.() -> AiProviderSetupUiState) {
        mutableState.update { it.block() }
    }
}
