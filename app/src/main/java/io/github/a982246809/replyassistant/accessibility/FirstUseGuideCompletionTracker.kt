package io.github.a982246809.replyassistant.accessibility

import io.github.a982246809.replyassistant.data.datastore.FirstUseGuidePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal class FirstUseGuideCompletionTracker(
    initialCompleted: Boolean = false
) {
    private var completed = initialCompleted

    fun update(completed: Boolean) {
        this.completed = completed
    }

    fun observe(
        scope: CoroutineScope,
        preferences: Flow<FirstUseGuidePreferences>
    ): Job = scope.launch {
        preferences
            .map { preference: FirstUseGuidePreferences -> preference.completed }
            .distinctUntilChanged()
            .collect(::update)
    }

    fun tryMarkCompleted(): Boolean {
        if (completed) return false
        completed = true
        return true
    }
}
