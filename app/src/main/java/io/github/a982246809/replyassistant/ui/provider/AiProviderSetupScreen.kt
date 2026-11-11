package io.github.a982246809.replyassistant.ui.provider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiProviderSetupScreen(
    viewModel: AiProviderSetupViewModel,
    onSaved: () -> Unit,
    showBack: Boolean,
    onBack: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 服务配置") },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "使用你自己的模型服务",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "聊天内容会直接发送到下面配置的服务。App 不设中转服务器，也不会把 API Key 上传到本项目。",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = state.baseUrl,
                    onValueChange = viewModel::setBaseUrl,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API Base URL") },
                    placeholder = { Text("https://api.example.com/v1") },
                    singleLine = true,
                    enabled = !state.saving
                )
            }
            item {
                OutlinedTextField(
                    value = state.apiKey,
                    onValueChange = viewModel::setApiKey,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API Key（本地无鉴权服务可留空）") },
                    placeholder = {
                        Text(
                            if (state.hasStoredKey) "已保存，留空则保持不变" else "sk-..."
                        )
                    },
                    singleLine = true,
                    enabled = !state.saving,
                    visualTransformation = PasswordVisualTransformation()
                )
            }
            if (state.hasStoredKey) {
                item {
                    OutlinedButton(
                        onClick = viewModel::clearApiKey,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.saving
                    ) {
                        Text("清除已保存的 API Key")
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = state.textModel,
                    onValueChange = viewModel::setTextModel,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("主模型") },
                    placeholder = { Text("例如 gpt-4o-mini") },
                    singleLine = true,
                    enabled = !state.saving
                )
            }
            item {
                OutlinedTextField(
                    value = state.visionModel,
                    onValueChange = viewModel::setVisionModel,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("视觉模型（可选）") },
                    placeholder = { Text("留空时使用主模型") },
                    singleLine = true,
                    enabled = !state.saving
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = viewModel::fetchModels,
                        modifier = Modifier.weight(1f),
                        enabled = !state.saving && !state.fetchingModels
                    ) {
                        Text(if (state.fetchingModels) "获取中…" else "获取模型列表")
                    }
                    Button(
                        onClick = viewModel::save,
                        modifier = Modifier.weight(1f),
                        enabled = !state.saving && !state.loading
                    ) {
                        Text(if (state.saving) "保存中…" else "保存配置")
                    }
                }
            }
            if (state.isHttp) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            "当前使用 HTTP 明文连接，API Key 和聊天内容可能被同一网络中的其他人读取。仅建议用于本机或可信局域网服务。",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            if (state.models.isNotEmpty()) {
                item {
                    Text("可用模型", style = MaterialTheme.typography.titleMedium)
                    Text(
                        state.models.joinToString("、"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            state.message?.let { message ->
                item {
                    Text(
                        message,
                        color = if (state.messageIsError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }
        }
    }
}
