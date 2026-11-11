package io.github.a982246809.replyassistant.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.a982246809.replyassistant.ai.AiProviderState
import io.github.a982246809.replyassistant.data.datastore.AutoStartGuideStatus
import io.github.a982246809.replyassistant.data.datastore.FirstUseGuidePreferences
import io.github.a982246809.replyassistant.data.datastore.UserPreferences
import io.github.a982246809.replyassistant.data.repository.AiRepository
import io.github.a982246809.replyassistant.util.isOurAccessibilityServiceEnabled
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    aiRepository: AiRepository,
    private val userPreferences: UserPreferences
) : AndroidViewModel(application) {
    private val mutableState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferences.floatingBallEnabled.collect { enabled ->
                mutableState.update { current ->
                    current.copy(
                        isFloatingBallEnabled = enabled,
                        firstUseGuide = deriveFirstUseGuideUiState(
                            preferences = current.firstUseGuidePreferences,
                            isAccessibilityEnabled = current.isAccessibilityEnabled,
                            isFloatingBallEnabled = enabled
                        )
                    )
                }
            }
        }
        viewModelScope.launch {
            userPreferences.initializeFirstUseGuide(
                completedForExistingInstall = isExistingInstallUpgrade(application)
            )
            userPreferences.firstUseGuidePreferences.collect { preferences ->
                mutableState.update { current ->
                    current.copy(
                        firstUseGuidePreferences = preferences,
                        firstUseGuide = deriveFirstUseGuideUiState(
                            preferences = preferences,
                            isAccessibilityEnabled = current.isAccessibilityEnabled,
                            isFloatingBallEnabled = current.isFloatingBallEnabled
                        )
                    )
                }
            }
        }
        viewModelScope.launch {
            aiRepository.providerState.collect { provider ->
                mutableState.update { it.copy(providerState = provider) }
            }
        }
        checkAccessibilityStatus()
    }

    fun checkAccessibilityStatus() {
        val isEnabled = isOurAccessibilityServiceEnabled(getApplication())
        mutableState.update { current ->
            current.copy(
                isAccessibilityEnabled = isEnabled,
                firstUseGuide = deriveFirstUseGuideUiState(
                    preferences = current.firstUseGuidePreferences,
                    isAccessibilityEnabled = isEnabled,
                    isFloatingBallEnabled = current.isFloatingBallEnabled
                )
            )
        }
        if (isEnabled) {
            viewModelScope.launch {
                userPreferences.completeFirstUseGuideActivationIfRequested()
            }
        }
    }

    suspend fun requestFirstUseGuideActivation() =
        userPreferences.requestFirstUseGuideActivation()

    fun collapseFirstUseGuide() {
        viewModelScope.launch { userPreferences.collapseFirstUseGuide() }
    }

    fun expandFirstUseGuide() {
        viewModelScope.launch { userPreferences.expandFirstUseGuide() }
    }

    fun markFirstUseAutoStartSettingsOpened() {
        viewModelScope.launch { userPreferences.markFirstUseAutoStartSettingsOpened() }
    }

    fun setFloatingBallEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setFloatingBallEnabled(enabled) }
    }

    data class MainUiState(
        val isAccessibilityEnabled: Boolean = false,
        val isFloatingBallEnabled: Boolean = false,
        val providerState: AiProviderState = AiProviderState.Loading,
        val firstUseGuidePreferences: FirstUseGuidePreferences = FirstUseGuidePreferences(),
        val firstUseGuide: FirstUseGuideUiState = FirstUseGuideUiState()
    )
}

internal fun deriveFirstUseGuideUiState(
    preferences: FirstUseGuidePreferences,
    isAccessibilityEnabled: Boolean,
    isFloatingBallEnabled: Boolean
): FirstUseGuideUiState {
    if (!preferences.initialized || preferences.completed) return FirstUseGuideUiState()
    val stage = when {
        !isAccessibilityEnabled -> FirstUseGuideStage.ENABLE_ACCESSIBILITY
        !isFloatingBallEnabled -> FirstUseGuideStage.ENABLE_FLOATING_BALL
        else -> FirstUseGuideStage.TRY_WECHAT
    }
    return FirstUseGuideUiState(
        stage = stage,
        collapsed = preferences.collapsed,
        activationRequested = preferences.activationRequested,
        showAccessibilityTroubleshooting =
            preferences.activationRequested && !isAccessibilityEnabled,
        autoStartGuideStatus = preferences.autoStartGuideStatus,
        isAutoStartRequired =
            preferences.autoStartGuideStatus != AutoStartGuideStatus.SETTINGS_OPENED
    )
}

@Suppress("DEPRECATION")
internal fun isExistingInstallUpgrade(application: Application): Boolean = runCatching {
    val packageInfo = application.packageManager.getPackageInfo(application.packageName, 0)
    packageInfo.firstInstallTime > 0L && packageInfo.lastUpdateTime > packageInfo.firstInstallTime
}.getOrDefault(false)

enum class FirstUseGuideStage {
    HIDDEN,
    ENABLE_ACCESSIBILITY,
    ENABLE_FLOATING_BALL,
    TRY_WECHAT
}

data class FirstUseGuideUiState(
    val stage: FirstUseGuideStage = FirstUseGuideStage.HIDDEN,
    val collapsed: Boolean = false,
    val activationRequested: Boolean = false,
    val showAccessibilityTroubleshooting: Boolean = false,
    val autoStartGuideStatus: AutoStartGuideStatus = AutoStartGuideStatus.NOT_HANDLED,
    val isAutoStartRequired: Boolean = false
)

enum class MainContentDestination {
    LOADING,
    PROVIDER_SETUP,
    FIRST_USE_GUIDE,
    HOME
}

internal fun deriveMainContentDestination(
    preferences: FirstUseGuidePreferences,
    guide: FirstUseGuideUiState,
    providerState: AiProviderState
): MainContentDestination = when {
    !preferences.initialized || providerState is AiProviderState.Loading ->
        MainContentDestination.LOADING
    providerState is AiProviderState.Missing -> MainContentDestination.PROVIDER_SETUP
    guide.stage != FirstUseGuideStage.HIDDEN && !guide.collapsed ->
        MainContentDestination.FIRST_USE_GUIDE
    else -> MainContentDestination.HOME
}
