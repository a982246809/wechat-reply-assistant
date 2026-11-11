package io.github.a982246809.replyassistant.data.exclusions

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.a982246809.replyassistant.AssistantChatExclusion
import io.github.a982246809.replyassistant.AssistantChatKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

interface AssistantChatExclusionStore {
    fun observe(): Flow<List<AssistantChatExclusion>>
    suspend fun add(exclusion: AssistantChatExclusion): Boolean
    suspend fun remove(key: AssistantChatKey): Boolean
}

@Singleton
class DataStoreAssistantChatExclusionStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : AssistantChatExclusionStore {
    private object Keys {
        val EXCLUSIONS = stringPreferencesKey("assistant_chat_exclusions")
    }

    override fun observe(): Flow<List<AssistantChatExclusion>> =
        dataStore.data.map { preferences -> decode(preferences[Keys.EXCLUSIONS]) }

    override suspend fun add(exclusion: AssistantChatExclusion): Boolean {
        var inserted = false
        dataStore.edit { preferences ->
            val current = decode(preferences[Keys.EXCLUSIONS])
            if (current.any { it.key == exclusion.key }) return@edit
            inserted = true
            preferences[Keys.EXCLUSIONS] = encode(current + exclusion)
        }
        return inserted
    }

    override suspend fun remove(key: AssistantChatKey): Boolean {
        var removed = false
        dataStore.edit { preferences ->
            val current = decode(preferences[Keys.EXCLUSIONS])
            val remaining = current.filterNot { it.key == key }
            removed = remaining.size != current.size
            if (removed) preferences[Keys.EXCLUSIONS] = encode(remaining)
        }
        return removed
    }

    private fun decode(raw: String?): List<AssistantChatExclusion> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            Json.decodeFromString<List<StoredExclusion>>(raw).mapNotNull { stored ->
                val key = AssistantChatKey.fromCanonical(
                    stored.sourcePackage,
                    stored.canonicalTitle
                ) ?: return@mapNotNull null
                AssistantChatExclusion(key, stored.rawTitle, stored.createdAt)
            }
        }.getOrDefault(emptyList())
    }

    private fun encode(exclusions: List<AssistantChatExclusion>): String =
        Json.encodeToString(
            exclusions.map {
                StoredExclusion(
                    sourcePackage = it.key.sourcePackage,
                    canonicalTitle = it.key.canonicalTitle,
                    rawTitle = it.rawTitle,
                    createdAt = it.createdAt
                )
            }
        )

    @Serializable
    private data class StoredExclusion(
        val sourcePackage: String,
        val canonicalTitle: String,
        val rawTitle: String,
        val createdAt: Long
    )
}
