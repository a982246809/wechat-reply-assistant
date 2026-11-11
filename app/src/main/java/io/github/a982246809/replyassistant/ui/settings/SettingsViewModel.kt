package io.github.a982246809.replyassistant.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.a982246809.replyassistant.AssistantChatPolicy
import io.github.a982246809.replyassistant.AssistantChatPolicyState
import io.github.a982246809.replyassistant.ai.AiProviderState
import io.github.a982246809.replyassistant.data.datastore.UserPreferences
import io.github.a982246809.replyassistant.data.repository.AiRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val aiRepository: AiRepository,
    private val assistantChatPolicy: AssistantChatPolicy
) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferences.replyCount.collect { count ->
                mutableState.update { it.copy(selectedReplyCount = count) }
            }
        }
        viewModelScope.launch {
            aiRepository.providerState.collect { provider ->
                mutableState.update { it.copy(providerState = provider) }
            }
        }
        viewModelScope.launch {
            try {
                assistantChatPolicy.observeState().collect { state ->
                    mutableState.update {
                        it.copy(
                            assistantChatExclusionSummary = when (state) {
                                AssistantChatPolicyState.Loading ->
                                    AssistantChatExclusionSummary.Loading
                                is AssistantChatPolicyState.Ready ->
                                    AssistantChatExclusionSummary.Ready(state.exclusions.size)
                                is AssistantChatPolicyState.Unavailable ->
                                    AssistantChatExclusionSummary.Unavailable
                            }
                        )
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                mutableState.update {
                    it.copy(
                        assistantChatExclusionSummary =
                            AssistantChatExclusionSummary.Unavailable
                    )
                }
            }
        }
    }

    fun setReplyCount(count: Int) {
        viewModelScope.launch { userPreferences.setReplyCount(count) }
    }

    data class SettingsUiState(
        val selectedReplyCount: Int = UserPreferences.DEFAULT_REPLY_COUNT,
        val providerState: AiProviderState = AiProviderState.Loading,
        val assistantChatExclusionSummary: AssistantChatExclusionSummary =
            AssistantChatExclusionSummary.Loading
    )
}

sealed interface AssistantChatExclusionSummary {
    data object Loading : AssistantChatExclusionSummary
    data class Ready(val count: Int) : AssistantChatExclusionSummary
    data object Unavailable : AssistantChatExclusionSummary
}
