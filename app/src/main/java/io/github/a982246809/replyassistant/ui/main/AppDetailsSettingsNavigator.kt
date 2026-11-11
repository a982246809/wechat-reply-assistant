package io.github.a982246809.replyassistant.ui.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object AppDetailsSettingsNavigator {
    fun open(context: Context): Boolean = runCatching {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }.isSuccess
}
