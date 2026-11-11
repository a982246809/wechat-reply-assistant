package io.github.a982246809.replyassistant.ui.assistantchatexclusions

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import io.github.a982246809.replyassistant.ui.theme.ReplyAssistantTheme

@AndroidEntryPoint
class AssistantChatExclusionsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReplyAssistantTheme {
                AssistantChatExclusionsScreen(onNavigateBack = ::finish)
            }
        }
    }
}
