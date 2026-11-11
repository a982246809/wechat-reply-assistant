package io.github.a982246809.replyassistant.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.a982246809.replyassistant.AssistantChatPolicy
import io.github.a982246809.replyassistant.DefaultAssistantChatPolicy
import io.github.a982246809.replyassistant.data.exclusions.AssistantChatExclusionStore
import io.github.a982246809.replyassistant.data.exclusions.DataStoreAssistantChatExclusionStore
import io.github.a982246809.replyassistant.data.provider.AiSecretStore
import io.github.a982246809.replyassistant.data.provider.AndroidKeystoreAiSecretStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

private val Context.userPreferencesStore by preferencesDataStore(name = "user_preferences")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.userPreferencesStore

    @Provides
    @Singleton
    fun provideAiSecretStore(
        store: AndroidKeystoreAiSecretStore
    ): AiSecretStore = store

    @Provides
    @Singleton
    fun provideAssistantChatExclusionStore(
        store: DataStoreAssistantChatExclusionStore
    ): AssistantChatExclusionStore = store

    @Provides
    @Singleton
    fun provideAssistantChatPolicy(
        store: AssistantChatExclusionStore
    ): AssistantChatPolicy = DefaultAssistantChatPolicy(
        store = store,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    )
}
