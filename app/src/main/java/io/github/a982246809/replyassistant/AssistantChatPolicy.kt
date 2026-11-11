package io.github.a982246809.replyassistant

import io.github.a982246809.replyassistant.data.exclusions.AssistantChatExclusionStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.Collections

const val WECHAT_SOURCE_PACKAGE: String = "com.tencent.mm"
const val ASSISTANT_CHAT_TITLE_MAX_CODE_POINTS: Int = 80

class AssistantChatKey private constructor(
    val sourcePackage: String,
    val canonicalTitle: String
) {
    override fun equals(other: Any?): Boolean =
        other is AssistantChatKey &&
            sourcePackage == other.sourcePackage &&
            canonicalTitle == other.canonicalTitle

    override fun hashCode(): Int = 31 * sourcePackage.hashCode() + canonicalTitle.hashCode()

    companion object {
        fun from(sourcePackage: String, rawTitle: String): AssistantChatKey? {
            val canonicalSourcePackage = sourcePackage.trim()
            val canonicalTitle = canonicalizeWeChatChatTitle(rawTitle)
            if (canonicalSourcePackage.isEmpty() || canonicalTitle.isEmpty()) return null
            return AssistantChatKey(canonicalSourcePackage, canonicalTitle)
        }

        internal fun fromCanonical(
            sourcePackage: String,
            canonicalTitle: String
        ): AssistantChatKey? = from(sourcePackage, canonicalTitle)
    }
}

data class AssistantChatExclusion(
    val key: AssistantChatKey,
    val rawTitle: String,
    val createdAt: Long
)

sealed interface AssistantChatPolicyState {
    data object Loading : AssistantChatPolicyState

    class Ready(source: Collection<AssistantChatExclusion>) : AssistantChatPolicyState {
        val exclusions: List<AssistantChatExclusion> =
            Collections.unmodifiableList(ArrayList(source))
        val excludedKeys: Set<AssistantChatKey> = Collections.unmodifiableSet(
            this.exclusions.mapTo(linkedSetOf()) { it.key }
        )

        operator fun contains(key: AssistantChatKey): Boolean = key in excludedKeys

        override fun equals(other: Any?): Boolean =
            other is Ready && exclusions == other.exclusions

        override fun hashCode(): Int = exclusions.hashCode()
    }

    data class Unavailable(val cause: Throwable? = null) : AssistantChatPolicyState
}

sealed interface AssistantExclusionResult {
    data class Excluded(
        val exclusion: AssistantChatExclusion,
        val newlyInserted: Boolean
    ) : AssistantExclusionResult

    data class Restored(val key: AssistantChatKey) : AssistantExclusionResult
    data class NotExcluded(val key: AssistantChatKey) : AssistantExclusionResult
    data object BlankTitle : AssistantExclusionResult
    data class TitleTooLong(
        val maxCodePoints: Int = ASSISTANT_CHAT_TITLE_MAX_CODE_POINTS
    ) : AssistantExclusionResult
    data object SystemEntry : AssistantExclusionResult
    data object GroupChat : AssistantExclusionResult
    data class Unavailable(val cause: Throwable) : AssistantExclusionResult
    data class Failed(val cause: Throwable) : AssistantExclusionResult
}

interface AssistantChatPolicy {
    val state: StateFlow<AssistantChatPolicyState>
    fun observeState(): Flow<AssistantChatPolicyState> = state
    suspend fun exclude(rawTitle: String): AssistantExclusionResult
    suspend fun restore(key: AssistantChatKey): AssistantExclusionResult
    suspend fun retryUnavailableStorage()
}

class DefaultAssistantChatPolicy(
    private val store: AssistantChatExclusionStore,
    private val scope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis
) : AssistantChatPolicy {
    private val mutableState =
        MutableStateFlow<AssistantChatPolicyState>(AssistantChatPolicyState.Loading)
    override val state: StateFlow<AssistantChatPolicyState> = mutableState.asStateFlow()
    private var observationJob: Job? = null

    init {
        startObservation()
    }

    override suspend fun exclude(rawTitle: String): AssistantExclusionResult {
        val canonicalTitle = canonicalizeWeChatChatTitle(rawTitle)
        if (canonicalTitle.isEmpty()) return AssistantExclusionResult.BlankTitle
        if (canonicalTitle.codePointCount(0, canonicalTitle.length) >
            ASSISTANT_CHAT_TITLE_MAX_CODE_POINTS
        ) {
            return AssistantExclusionResult.TitleTooLong()
        }
        if (!isWeChatChatInterfaceTitle(canonicalTitle)) {
            return AssistantExclusionResult.SystemEntry
        }
        if (isWeChatGroupChatTitle(canonicalTitle)) {
            return AssistantExclusionResult.GroupChat
        }
        val key = AssistantChatKey.from(WECHAT_SOURCE_PACKAGE, canonicalTitle)
            ?: return AssistantExclusionResult.BlankTitle
        return try {
            val exclusion = AssistantChatExclusion(key, rawTitle.trim(), now())
            val inserted = store.add(exclusion)
            AssistantExclusionResult.Excluded(exclusion, newlyInserted = inserted)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            mutableState.value = AssistantChatPolicyState.Unavailable(error)
            AssistantExclusionResult.Unavailable(error)
        }
    }

    override suspend fun restore(key: AssistantChatKey): AssistantExclusionResult = try {
        if (store.remove(key)) {
            AssistantExclusionResult.Restored(key)
        } else {
            AssistantExclusionResult.NotExcluded(key)
        }
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        mutableState.value = AssistantChatPolicyState.Unavailable(error)
        AssistantExclusionResult.Unavailable(error)
    }

    override suspend fun retryUnavailableStorage() {
        startObservation()
    }

    private fun startObservation() {
        observationJob?.cancel()
        mutableState.value = AssistantChatPolicyState.Loading
        observationJob = scope.launch {
            try {
                store.observe().collect { exclusions ->
                    mutableState.value = AssistantChatPolicyState.Ready(exclusions)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                mutableState.value = AssistantChatPolicyState.Unavailable(error)
            }
        }
    }
}
