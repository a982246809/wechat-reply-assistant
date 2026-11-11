package io.github.a982246809.replyassistant.data.provider

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.a982246809.replyassistant.ai.AiProviderConfig
import io.github.a982246809.replyassistant.ai.AiProviderConfigInput
import io.github.a982246809.replyassistant.ai.AiProviderConfigRules
import io.github.a982246809.replyassistant.ai.AiProviderState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiProviderConfigStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val secretStore: AiSecretStore
) {
    private object Keys {
        val BASE_URL = stringPreferencesKey("ai_provider_base_url")
        val TEXT_MODEL = stringPreferencesKey("ai_provider_text_model")
        val VISION_MODEL = stringPreferencesKey("ai_provider_vision_model")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableState = MutableStateFlow<AiProviderState>(AiProviderState.Loading)
    val state: StateFlow<AiProviderState> = mutableState.asStateFlow()

    init {
        scope.launch { refresh() }
    }

    suspend fun refresh() {
        mutableState.value = load()?.let(AiProviderState::Ready) ?: AiProviderState.Missing
    }

    suspend fun load(): AiProviderConfig? {
        return withContext(Dispatchers.IO) {
            val preferences = dataStore.data.first()
            val baseUrl = preferences[Keys.BASE_URL]?.takeIf(String::isNotBlank) ?: return@withContext null
            val textModel =
                preferences[Keys.TEXT_MODEL]?.takeIf(String::isNotBlank) ?: return@withContext null
            AiProviderConfig(
                baseUrl = baseUrl,
                textModel = textModel,
                visionModel = preferences[Keys.VISION_MODEL]?.takeIf(String::isNotBlank),
                hasApiKey = !secretStore.readApiKey().isNullOrBlank()
            )
        }
    }

    suspend fun save(input: AiProviderConfigInput) {
        val normalized = AiProviderConfigRules.normalizeInput(input)
        val errors = AiProviderConfigRules.validate(normalized)
        require(errors.isEmpty()) { errors.joinToString("；") }
        withContext(Dispatchers.IO) {
            dataStore.edit { preferences ->
                preferences[Keys.BASE_URL] = normalized.baseUrl
                preferences[Keys.TEXT_MODEL] = normalized.textModel
                if (normalized.visionModel == null) {
                    preferences.remove(Keys.VISION_MODEL)
                } else {
                    preferences[Keys.VISION_MODEL] = normalized.visionModel
                }
            }
            when (input.apiKey) {
                null -> Unit
                "" -> secretStore.clearApiKey()
                else -> secretStore.writeApiKey(input.apiKey.trim())
            }
        }
        refresh()
    }

    suspend fun clearApiKey() {
        withContext(Dispatchers.IO) { secretStore.clearApiKey() }
        refresh()
    }
}
