package io.github.a982246809.replyassistant.ui.assistantchatexclusions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.a982246809.replyassistant.ASSISTANT_CHAT_TITLE_MAX_CODE_POINTS
import io.github.a982246809.replyassistant.AssistantChatExclusion
import io.github.a982246809.replyassistant.AssistantChatKey
import io.github.a982246809.replyassistant.AssistantChatPolicy
import io.github.a982246809.replyassistant.AssistantChatPolicyState
import io.github.a982246809.replyassistant.AssistantExclusionResult
import io.github.a982246809.replyassistant.WECHAT_SOURCE_PACKAGE
import io.github.a982246809.replyassistant.canonicalizeWeChatChatTitle
import io.github.a982246809.replyassistant.isWeChatChatInterfaceTitle
import io.github.a982246809.replyassistant.isWeChatGroupChatTitle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AssistantChatExclusionsLoadState { LOADING, READY, UNAVAILABLE }

data class AssistantChatExclusionUiModel(
    val title: String,
    val key: AssistantChatKey = requireNotNull(
        AssistantChatKey.from(WECHAT_SOURCE_PACKAGE, title)
    )
)

data class AssistantChatExclusionsUiState(
    val loadState: AssistantChatExclusionsLoadState = AssistantChatExclusionsLoadState.LOADING,
    val exclusions: List<AssistantChatExclusionUiModel> = emptyList(),
    val input: String = "",
    val inputError: String? = null,
    val pendingTitle: String? = null,
    val confirmationError: String? = null,
    val submitting: Boolean = false,
    val restoringTitles: Set<String> = emptySet(),
    val ruleErrors: Map<String, String> = emptyMap(),
    val retrying: Boolean = false
) {
    val operating: Boolean
        get() = submitting || restoringTitles.isNotEmpty() || retrying
}

sealed interface AssistantChatExclusionsAction {
    data class ChangeInput(val value: String) : AssistantChatExclusionsAction
    data object RequestAdd : AssistantChatExclusionsAction
    data object ConfirmAdd : AssistantChatExclusionsAction
    data object DismissConfirmation : AssistantChatExclusionsAction
    data class Restore(val title: String) : AssistantChatExclusionsAction
    data object Retry : AssistantChatExclusionsAction
}

data class AssistantChatExclusionsEvent(val message: String)

@HiltViewModel
class AssistantChatExclusionsViewModel @Inject constructor(
    private val policy: AssistantChatPolicy
) : ViewModel() {
    private val mutableState = MutableStateFlow(AssistantChatExclusionsUiState())
    val uiState: StateFlow<AssistantChatExclusionsUiState> = mutableState.asStateFlow()

    private val mutableEvents = MutableSharedFlow<AssistantChatExclusionsEvent>(
        extraBufferCapacity = 8
    )
    val events: SharedFlow<AssistantChatExclusionsEvent> = mutableEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            policy.observeState()
                .catch {
                    mutableState.update {
                        it.copy(loadState = AssistantChatExclusionsLoadState.UNAVAILABLE)
                    }
                }
                .collect(::applyPolicyState)
        }
    }

    fun onAction(action: AssistantChatExclusionsAction) {
        when (action) {
            is AssistantChatExclusionsAction.ChangeInput -> changeInput(action.value)
            AssistantChatExclusionsAction.RequestAdd -> requestAdd()
            AssistantChatExclusionsAction.ConfirmAdd -> confirmAdd()
            AssistantChatExclusionsAction.DismissConfirmation -> dismissConfirmation()
            is AssistantChatExclusionsAction.Restore -> restore(action.title)
            AssistantChatExclusionsAction.Retry -> retry()
        }
    }

    private fun applyPolicyState(state: AssistantChatPolicyState) {
        mutableState.update { current ->
            when (state) {
                AssistantChatPolicyState.Loading -> current.copy(
                    loadState = AssistantChatExclusionsLoadState.LOADING
                )
                is AssistantChatPolicyState.Ready -> current.copy(
                    loadState = AssistantChatExclusionsLoadState.READY,
                    exclusions = state.exclusions
                        .sortedByDescending(AssistantChatExclusion::createdAt)
                        .map(::toUiModel)
                )
                is AssistantChatPolicyState.Unavailable -> current.copy(
                    loadState = AssistantChatExclusionsLoadState.UNAVAILABLE
                )
            }
        }
    }

    private fun changeInput(value: String) {
        if (mutableState.value.submitting) return
        mutableState.update {
            it.copy(
                input = value,
                inputError = null,
                confirmationError = null
            )
        }
    }

    private fun requestAdd() {
        val state = mutableState.value
        if (state.loadState != AssistantChatExclusionsLoadState.READY || state.operating) return
        val error = validateForConfirmation(state.input, state.exclusions)
        if (error != null) {
            mutableState.update { it.copy(inputError = error) }
            return
        }
        mutableState.update {
            it.copy(
                pendingTitle = state.input,
                inputError = null,
                confirmationError = null
            )
        }
    }

    private fun dismissConfirmation() {
        mutableState.update {
            it.copy(
                pendingTitle = null,
                confirmationError = null
            )
        }
    }

    private fun confirmAdd() {
        val title = mutableState.value.pendingTitle ?: return
        if (mutableState.value.operating) return
        mutableState.update { it.copy(submitting = true, confirmationError = null) }
        viewModelScope.launch {
            handleExcludeResult(title, policy.exclude(title))
        }
    }

    private suspend fun handleExcludeResult(title: String, result: AssistantExclusionResult) {
        when (result) {
            is AssistantExclusionResult.Excluded -> {
                mutableState.update { current ->
                    current.copy(
                        exclusions = (listOf(toUiModel(result.exclusion)) +
                            current.exclusions).distinctBy(AssistantChatExclusionUiModel::key),
                        input = "",
                        inputError = null,
                        pendingTitle = null,
                        confirmationError = null,
                        submitting = false
                    )
                }
                mutableEvents.emit(
                    AssistantChatExclusionsEvent("已停用“${result.exclusion.rawTitle}”。")
                )
            }
            else -> {
                mutableState.update {
                    it.copy(
                        loadState = if (result is AssistantExclusionResult.Unavailable) {
                            AssistantChatExclusionsLoadState.UNAVAILABLE
                        } else {
                            it.loadState
                        },
                        confirmationError = resultMessage(result, title),
                        submitting = false
                    )
                }
            }
        }
    }

    private fun restore(title: String) {
        val model = mutableState.value.exclusions.firstOrNull { it.title == title } ?: return
        mutableState.update {
            it.copy(
                restoringTitles = it.restoringTitles + title,
                ruleErrors = it.ruleErrors - title
            )
        }
        viewModelScope.launch {
            when (val result = policy.restore(model.key)) {
                is AssistantExclusionResult.Restored -> {
                    mutableState.update {
                        it.copy(
                            exclusions = it.exclusions.filterNot { rule ->
                                rule.key == model.key
                            },
                            restoringTitles = it.restoringTitles - title
                        )
                    }
                    mutableEvents.emit(AssistantChatExclusionsEvent("已重新开启“$title”。"))
                }
                is AssistantExclusionResult.NotExcluded -> {
                    mutableState.update {
                        it.copy(
                            exclusions = it.exclusions.filterNot { rule ->
                                rule.key == model.key
                            },
                            restoringTitles = it.restoringTitles - title
                        )
                    }
                }
                else -> mutableState.update {
                    it.copy(
                        restoringTitles = it.restoringTitles - title,
                        ruleErrors = it.ruleErrors + (title to resultMessage(result, title))
                    )
                }
            }
        }
    }

    private fun retry() {
        if (mutableState.value.operating) return
        mutableState.update {
            it.copy(loadState = AssistantChatExclusionsLoadState.LOADING, retrying = true)
        }
        viewModelScope.launch {
            try {
                policy.retryUnavailableStorage()
                mutableState.update { it.copy(retrying = false) }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                mutableState.update {
                    it.copy(
                        loadState = AssistantChatExclusionsLoadState.UNAVAILABLE,
                        retrying = false
                    )
                }
            }
        }
    }
}

private fun toUiModel(exclusion: AssistantChatExclusion): AssistantChatExclusionUiModel =
    AssistantChatExclusionUiModel(
        title = exclusion.rawTitle.ifBlank { exclusion.key.canonicalTitle },
        key = exclusion.key
    )

internal fun validateForConfirmation(
    rawTitle: String,
    exclusions: List<AssistantChatExclusionUiModel>
): String? {
    val title = canonicalizeWeChatChatTitle(rawTitle)
    if (title.isEmpty()) return "请输入微信单聊的精确聊天标题。"
    if (title.codePointCount(0, title.length) > ASSISTANT_CHAT_TITLE_MAX_CODE_POINTS) {
        return "标题过长，最多 $ASSISTANT_CHAT_TITLE_MAX_CODE_POINTS 个 Unicode 字符。"
    }
    if (!isWeChatChatInterfaceTitle(title)) {
        return "这不是可停用的微信单聊标题，请输入聊天页顶部显示的标题。"
    }
    if (isWeChatGroupChatTitle(title)) {
        return "不支持微信群聊；不启用名单仅适用于微信单聊。"
    }
    val key = AssistantChatKey.from(WECHAT_SOURCE_PACKAGE, title)
    if (exclusions.any { it.key == key }) {
        return "“$title”已在不启用名单中，无需重复添加。"
    }
    return null
}

private fun resultMessage(result: AssistantExclusionResult, title: String): String = when (result) {
    AssistantExclusionResult.BlankTitle -> "请输入微信单聊的精确聊天标题。"
    is AssistantExclusionResult.TitleTooLong ->
        "标题过长，最多 ${result.maxCodePoints} 个 Unicode 字符。"
    AssistantExclusionResult.SystemEntry ->
        "这不是可停用的微信单聊标题，请输入聊天页顶部显示的标题。"
    AssistantExclusionResult.GroupChat -> "不支持微信群聊；不启用名单仅适用于微信单聊。"
    is AssistantExclusionResult.Unavailable -> "本地规则存储不可用，请重试。"
    is AssistantExclusionResult.Failed -> "操作失败，请稍后重试。"
    is AssistantExclusionResult.NotExcluded -> "“$title”的停用规则已不存在。"
    is AssistantExclusionResult.Excluded -> "已停用“$title”。"
    is AssistantExclusionResult.Restored -> "已重新开启“$title”。"
}
