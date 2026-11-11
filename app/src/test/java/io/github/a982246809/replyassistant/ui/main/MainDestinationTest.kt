package io.github.a982246809.replyassistant.ui.main

import io.github.a982246809.replyassistant.ai.AiProviderState
import io.github.a982246809.replyassistant.data.datastore.FirstUseGuidePreferences
import org.junit.Assert.assertEquals
import org.junit.Test

class MainDestinationTest {
    @Test
    fun `provider setup precedes onboarding when not configured`() {
        val destination = deriveMainContentDestination(
            preferences = FirstUseGuidePreferences(initialized = true),
            guide = FirstUseGuideUiState(stage = FirstUseGuideStage.ENABLE_ACCESSIBILITY),
            providerState = AiProviderState.Missing
        )
        assertEquals(MainContentDestination.PROVIDER_SETUP, destination)
    }

    @Test
    fun `loading is shown until both preferences and provider are known`() {
        assertEquals(
            MainContentDestination.LOADING,
            deriveMainContentDestination(
                preferences = FirstUseGuidePreferences(initialized = true),
                guide = FirstUseGuideUiState(),
                providerState = AiProviderState.Loading
            )
        )
    }

    @Test
    fun `home follows completed onboarding`() {
        assertEquals(
            MainContentDestination.HOME,
            deriveMainContentDestination(
                preferences = FirstUseGuidePreferences(initialized = true, completed = true),
                guide = FirstUseGuideUiState(),
                providerState = AiProviderState.Ready(
                    io.github.a982246809.replyassistant.ai.AiProviderConfig(
                        baseUrl = "https://example.com/v1",
                        textModel = "model",
                        visionModel = null,
                        hasApiKey = true
                    )
                )
            )
        )
    }
}
