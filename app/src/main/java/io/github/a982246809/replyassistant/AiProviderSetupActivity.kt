package io.github.a982246809.replyassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.github.a982246809.replyassistant.ui.provider.AiProviderSetupScreen
import io.github.a982246809.replyassistant.ui.provider.AiProviderSetupViewModel
import io.github.a982246809.replyassistant.ui.theme.ReplyAssistantTheme

@AndroidEntryPoint
class AiProviderSetupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReplyAssistantTheme {
                val viewModel: AiProviderSetupViewModel = hiltViewModel()
                AiProviderSetupScreen(
                    viewModel = viewModel,
                    onSaved = { finish() },
                    showBack = true,
                    onBack = { finish() }
                )
            }
        }
    }
}
