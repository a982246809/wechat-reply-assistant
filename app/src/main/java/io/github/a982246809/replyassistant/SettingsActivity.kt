package io.github.a982246809.replyassistant

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import io.github.a982246809.replyassistant.ui.assistantchatexclusions.AssistantChatExclusionsActivity
import io.github.a982246809.replyassistant.ui.settings.SettingsScreen
import io.github.a982246809.replyassistant.ui.theme.ReplyAssistantTheme

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReplyAssistantTheme {
                SettingsScreen(
                    onNavigateBack = { finish() },
                    onOpenProviderSetup = {
                        startActivity(Intent(this, AiProviderSetupActivity::class.java))
                    },
                    onOpenAssistantChatExclusions = {
                        startActivity(
                            Intent(this, AssistantChatExclusionsActivity::class.java)
                        )
                    }
                )
            }
        }
    }
}
