package io.github.a982246809.replyassistant.ui.main

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import io.github.a982246809.replyassistant.AiProviderSetupActivity
import io.github.a982246809.replyassistant.SettingsActivity
import io.github.a982246809.replyassistant.ai.AiProviderState
import io.github.a982246809.replyassistant.ui.provider.AiProviderSetupScreen
import io.github.a982246809.replyassistant.ui.provider.AiProviderSetupViewModel
import io.github.a982246809.replyassistant.util.isOurAccessibilityServiceEnabled
import kotlinx.coroutines.launch

enum class FloatAction {
    None,
    OpenProviderSetup
}

fun mapper(bizCode: String?): FloatAction = when (bizCode) {
    "PROVIDER_NOT_CONFIGURED" -> FloatAction.OpenProviderSetup
    else -> FloatAction.None
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.checkAccessibilityStatus()
        }
    }

    when (
        deriveMainContentDestination(
            preferences = uiState.firstUseGuidePreferences,
            guide = uiState.firstUseGuide,
            providerState = uiState.providerState
        )
    ) {
        MainContentDestination.LOADING -> {
            LoadingScreen()
            return
        }

        MainContentDestination.PROVIDER_SETUP -> {
            val setupViewModel: AiProviderSetupViewModel = hiltViewModel()
            AiProviderSetupScreen(
                viewModel = setupViewModel,
                onSaved = {},
                showBack = false
            )
            return
        }

        MainContentDestination.FIRST_USE_GUIDE -> {
            BackHandler(onBack = viewModel::collapseFirstUseGuide)
            FirstUseGuideScreen(
                state = uiState.firstUseGuide,
                onPrimaryAction = {
                    when (uiState.firstUseGuide.stage) {
                        FirstUseGuideStage.ENABLE_ACCESSIBILITY -> {
                            coroutineScope.launch {
                                viewModel.requestFirstUseGuideActivation()
                                val outcome = AccessibilitySettingsNavigator.open(context)
                                outcome.message?.let {
                                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        FirstUseGuideStage.ENABLE_FLOATING_BALL -> {
                            if (!isOurAccessibilityServiceEnabled(context)) {
                                Toast.makeText(context, "请先开启无障碍服务", Toast.LENGTH_SHORT)
                                    .show()
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            } else {
                                viewModel.setFloatingBallEnabled(true)
                            }
                        }

                        FirstUseGuideStage.TRY_WECHAT -> {
                            if (uiState.firstUseGuide.isAutoStartRequired) {
                                val outcome = AutoStartSettingsNavigator.open(context)
                                Toast.makeText(context, outcome.message, Toast.LENGTH_LONG).show()
                                if (shouldRecordAutoStartSettingsOpened(outcome.result)) {
                                    viewModel.markFirstUseAutoStartSettingsOpened()
                                }
                            } else {
                                openWeChat(context)
                            }
                        }

                        FirstUseGuideStage.HIDDEN -> Unit
                    }
                },
                onOpenAppDetails = {
                    if (!AppDetailsSettingsNavigator.open(context)) {
                        Toast.makeText(
                            context,
                            "暂时无法打开应用信息，请在系统设置中手动进入",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onLater = viewModel::collapseFirstUseGuide
            )
            return
        }

        MainContentDestination.HOME -> Unit
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 微信回复助手", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
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
            if (
                uiState.firstUseGuide.stage != FirstUseGuideStage.HIDDEN &&
                uiState.firstUseGuide.collapsed
            ) {
                item {
                    FirstUseGuideCompactCard(
                        state = uiState.firstUseGuide,
                        onExpand = viewModel::expandFirstUseGuide
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("AI 服务", style = MaterialTheme.typography.titleMedium)
                        when (val provider = uiState.providerState) {
                            is AiProviderState.Ready -> {
                                Text(
                                    provider.config.baseUrl,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "主模型：${provider.config.textModel}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "视觉模型：${provider.config.effectiveVisionModel}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (provider.config.usesCleartextHttp) {
                                    Text(
                                        "当前使用 HTTP 明文连接，请注意 API Key 与聊天内容风险。",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            else -> Text("尚未配置 AI 服务")
                        }
                        Button(
                            onClick = {
                                context.startActivity(
                                    Intent(context, AiProviderSetupActivity::class.java)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("修改 AI 服务配置")
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("微信回复助手", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (uiState.isAccessibilityEnabled) {
                                "无障碍服务已开启"
                            } else {
                                "需要开启无障碍服务才能读取微信单聊"
                            },
                            color = if (uiState.isAccessibilityEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("启用悬浮球")
                            Switch(
                                checked = uiState.isAccessibilityEnabled &&
                                    uiState.isFloatingBallEnabled,
                                onCheckedChange = { checked ->
                                    handleSwitchChange(
                                        context = context,
                                        checked = checked,
                                        onSetEnabled = viewModel::setFloatingBallEnabled
                                    )
                                }
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("使用说明", fontWeight = FontWeight.Bold)
                        Text(
                            "进入微信单聊后，收到新文本消息会生成建议。点按建议直接发送，长按只填入输入框；短按悬浮球可生成更多回复。",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "所有请求都会直接发送到你配置的模型服务，并可能产生该服务的费用。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text("正在加载设置…")
        }
    }
}

private fun handleSwitchChange(
    context: Context,
    checked: Boolean,
    onSetEnabled: (Boolean) -> Unit
) {
    if (checked) {
        if (!isOurAccessibilityServiceEnabled(context)) {
            Toast.makeText(context, "请先开启无障碍服务", Toast.LENGTH_SHORT).show()
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return
        }
        onSetEnabled(true)
    } else {
        onSetEnabled(false)
        context.sendBroadcast(
            Intent("com.example.SWITCH_BALL").putExtra("isShowBall", false)
        )
    }
}

private fun openWeChat(context: Context) {
    val launchIntent = context.packageManager.getLaunchIntentForPackage(WECHAT_PACKAGE_NAME)
    if (launchIntent == null) {
        Toast.makeText(context, "未找到微信，请安装后再试", Toast.LENGTH_SHORT).show()
        return
    }
    runCatching {
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
    }.onFailure {
        Toast.makeText(context, "暂时无法打开微信，请手动打开", Toast.LENGTH_SHORT).show()
    }
}

internal fun shouldRecordAutoStartSettingsOpened(
    result: AutoStartNavigationResult
): Boolean = result == AutoStartNavigationResult.VENDOR_SETTINGS_OPENED ||
    result == AutoStartNavigationResult.APP_DETAILS_OPENED

private const val WECHAT_PACKAGE_NAME = "com.tencent.mm"
