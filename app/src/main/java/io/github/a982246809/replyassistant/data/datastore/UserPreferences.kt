package io.github.a982246809.replyassistant.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.a982246809.replyassistant.FloatingBallDockPosition
import io.github.a982246809.replyassistant.parseFloatingBallDockPosition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

data class FirstUseGuidePreferences(
    val initialized: Boolean = false,
    val collapsed: Boolean = false,
    val activationRequested: Boolean = false,
    val autoStartGuideStatus: AutoStartGuideStatus = AutoStartGuideStatus.NOT_HANDLED,
    val completed: Boolean = false
)

enum class AutoStartGuideStatus {
    NOT_HANDLED,
    SETTINGS_OPENED,
    SKIPPED
}

@Singleton
class UserPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val REPLY_COUNT = intPreferencesKey("reply_count")
        val FLOATING_BALL_ENABLED = booleanPreferencesKey("floating_ball_enabled")
        val FLOATING_BALL_DOCK_SIDE = stringPreferencesKey("floating_ball_dock_side")
        val FLOATING_BALL_Y_RATIO = floatPreferencesKey("floating_ball_y_ratio")
        val FLOATING_BALL_LONG_PRESS_HINT_SHOWN =
            booleanPreferencesKey("floating_ball_long_press_hint_shown")
        val FIRST_USE_GUIDE_INITIALIZED = booleanPreferencesKey("first_use_guide_initialized")
        val FIRST_USE_GUIDE_COLLAPSED = booleanPreferencesKey("first_use_guide_collapsed")
        val FIRST_USE_GUIDE_ACTIVATION_REQUESTED =
            booleanPreferencesKey("first_use_guide_activation_requested")
        val FIRST_USE_AUTO_START_GUIDE_STATUS =
            stringPreferencesKey("first_use_auto_start_guide_status")
        val FIRST_USE_GUIDE_COMPLETED = booleanPreferencesKey("first_use_guide_completed")
    }

    companion object {
        const val DEFAULT_REPLY_COUNT = 5
    }

    val replyCount: Flow<Int> = dataStore.data.map { preferences ->
        normalizeReplyCount(preferences[Keys.REPLY_COUNT])
    }

    val floatingBallEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[Keys.FLOATING_BALL_ENABLED] ?: false
    }

    val firstUseGuidePreferences: Flow<FirstUseGuidePreferences> =
        dataStore.data.map { preferences ->
            FirstUseGuidePreferences(
                initialized = preferences[Keys.FIRST_USE_GUIDE_INITIALIZED] ?: false,
                collapsed = preferences[Keys.FIRST_USE_GUIDE_COLLAPSED] ?: false,
                activationRequested =
                    preferences[Keys.FIRST_USE_GUIDE_ACTIVATION_REQUESTED] ?: false,
                autoStartGuideStatus = parseAutoStartGuideStatus(
                    preferences[Keys.FIRST_USE_AUTO_START_GUIDE_STATUS]
                ),
                completed = preferences[Keys.FIRST_USE_GUIDE_COMPLETED] ?: false
            )
        }

    fun getFloatingBallEnabledBlocking(): Boolean = runBlocking {
        dataStore.data.first()[Keys.FLOATING_BALL_ENABLED] ?: false
    }

    internal fun getFloatingBallDockPositionBlocking(): FloatingBallDockPosition = runBlocking {
        val preferences = dataStore.data.first()
        parseFloatingBallDockPosition(
            sideValue = preferences[Keys.FLOATING_BALL_DOCK_SIDE],
            yRatio = preferences[Keys.FLOATING_BALL_Y_RATIO]
        )
    }

    internal fun hasShownFloatingBallLongPressHintBlocking(): Boolean = runBlocking {
        dataStore.data.first()[Keys.FLOATING_BALL_LONG_PRESS_HINT_SHOWN] ?: false
    }

    internal fun hasCompletedFirstUseGuideBlocking(): Boolean = runBlocking {
        dataStore.data.first()[Keys.FIRST_USE_GUIDE_COMPLETED] ?: false
    }

    suspend fun setReplyCount(count: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.REPLY_COUNT] = normalizeReplyCount(count)
        }
    }

    suspend fun setFloatingBallEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.FLOATING_BALL_ENABLED] = enabled
        }
    }

    suspend fun initializeFirstUseGuide(completedForExistingInstall: Boolean) {
        dataStore.edit { preferences ->
            if (preferences[Keys.FIRST_USE_GUIDE_INITIALIZED] == true) return@edit
            preferences[Keys.FIRST_USE_GUIDE_INITIALIZED] = true
            preferences[Keys.FIRST_USE_GUIDE_COLLAPSED] = false
            preferences[Keys.FIRST_USE_GUIDE_ACTIVATION_REQUESTED] = false
            preferences[Keys.FIRST_USE_AUTO_START_GUIDE_STATUS] =
                AutoStartGuideStatus.NOT_HANDLED.name
            preferences[Keys.FIRST_USE_GUIDE_COMPLETED] = completedForExistingInstall
        }
    }

    suspend fun requestFirstUseGuideActivation() {
        dataStore.edit { preferences ->
            if (preferences[Keys.FIRST_USE_GUIDE_COMPLETED] == true) return@edit
            preferences[Keys.FIRST_USE_GUIDE_INITIALIZED] = true
            preferences[Keys.FIRST_USE_GUIDE_COLLAPSED] = false
            preferences[Keys.FIRST_USE_GUIDE_ACTIVATION_REQUESTED] = true
        }
    }

    suspend fun collapseFirstUseGuide() {
        dataStore.edit { preferences ->
            if (preferences[Keys.FIRST_USE_GUIDE_COMPLETED] != true) {
                preferences[Keys.FIRST_USE_GUIDE_COLLAPSED] = true
            }
        }
    }

    suspend fun expandFirstUseGuide() {
        dataStore.edit { preferences ->
            if (preferences[Keys.FIRST_USE_GUIDE_COMPLETED] != true) {
                preferences[Keys.FIRST_USE_GUIDE_COLLAPSED] = false
            }
        }
    }

    suspend fun completeFirstUseGuideActivationIfRequested(): Boolean {
        var activated = false
        dataStore.edit { preferences ->
            if (
                preferences[Keys.FIRST_USE_GUIDE_COMPLETED] != true &&
                preferences[Keys.FIRST_USE_GUIDE_ACTIVATION_REQUESTED] == true
            ) {
                preferences[Keys.FLOATING_BALL_ENABLED] = true
                preferences[Keys.FIRST_USE_GUIDE_ACTIVATION_REQUESTED] = false
                activated = true
            }
        }
        return activated
    }

    suspend fun markFirstUseGuideCompleted() {
        dataStore.edit { preferences ->
            if (preferences[Keys.FIRST_USE_GUIDE_COMPLETED] == true) return@edit
            preferences[Keys.FIRST_USE_GUIDE_INITIALIZED] = true
            preferences[Keys.FIRST_USE_GUIDE_ACTIVATION_REQUESTED] = false
            preferences[Keys.FIRST_USE_GUIDE_COMPLETED] = true
        }
    }

    suspend fun markFirstUseAutoStartSettingsOpened() {
        updateFirstUseAutoStartGuideStatus(AutoStartGuideStatus.SETTINGS_OPENED)
    }

    private suspend fun updateFirstUseAutoStartGuideStatus(status: AutoStartGuideStatus) {
        dataStore.edit { preferences ->
            if (
                preferences[Keys.FIRST_USE_GUIDE_COMPLETED] == true ||
                parseAutoStartGuideStatus(
                    preferences[Keys.FIRST_USE_AUTO_START_GUIDE_STATUS]
                ) != AutoStartGuideStatus.NOT_HANDLED
            ) {
                return@edit
            }
            preferences[Keys.FIRST_USE_AUTO_START_GUIDE_STATUS] = status.name
        }
    }

    internal suspend fun setFloatingBallDockPosition(position: FloatingBallDockPosition) {
        val normalized = position.normalized()
        dataStore.edit { preferences ->
            preferences[Keys.FLOATING_BALL_DOCK_SIDE] = normalized.side.name
            preferences[Keys.FLOATING_BALL_Y_RATIO] = normalized.yRatio
        }
    }

    internal suspend fun markFloatingBallLongPressHintShown() {
        dataStore.edit { preferences ->
            preferences[Keys.FLOATING_BALL_LONG_PRESS_HINT_SHOWN] = true
        }
    }

    private fun normalizeReplyCount(count: Int?): Int = if (count == 3) 3 else DEFAULT_REPLY_COUNT
}

internal fun parseAutoStartGuideStatus(value: String?): AutoStartGuideStatus =
    AutoStartGuideStatus.entries.firstOrNull { it.name == value }
        ?: AutoStartGuideStatus.NOT_HANDLED
