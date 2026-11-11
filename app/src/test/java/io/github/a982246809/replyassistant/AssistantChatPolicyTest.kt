package io.github.a982246809.replyassistant

import io.github.a982246809.replyassistant.data.exclusions.AssistantChatExclusionStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantChatPolicyTest {
    private val exclusions = MutableStateFlow<List<AssistantChatExclusion>>(emptyList())
    private val store = object : AssistantChatExclusionStore {
        override fun observe(): Flow<List<AssistantChatExclusion>> = exclusions

        override suspend fun add(exclusion: AssistantChatExclusion): Boolean {
            if (exclusions.value.any { it.key == exclusion.key }) return false
            exclusions.value = exclusions.value + exclusion
            return true
        }

        override suspend fun remove(key: AssistantChatKey): Boolean {
            val next = exclusions.value.filterNot { it.key == key }
            if (next.size == exclusions.value.size) return false
            exclusions.value = next
            return true
        }
    }

    @Test
    fun `exclude and restore a direct chat title`() = runTest {
        val policy = DefaultAssistantChatPolicy(
            store = store,
            scope = backgroundScope,
            now = { 42L }
        )

        val excluded = policy.exclude("小王")
        assertTrue(excluded is AssistantExclusionResult.Excluded)
        assertEquals(
            AssistantChatKey.from(WECHAT_SOURCE_PACKAGE, "小王"),
            (excluded as AssistantExclusionResult.Excluded).exclusion.key
        )

        val key = excluded.exclusion.key
        assertTrue(policy.restore(key) is AssistantExclusionResult.Restored)
    }

    @Test
    fun `rejects a group chat title`() = runTest {
        val policy = DefaultAssistantChatPolicy(store, backgroundScope)
        assertEquals(
            AssistantExclusionResult.GroupChat,
            policy.exclude("项目群 (3)")
        )
    }
}
