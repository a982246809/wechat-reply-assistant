package io.github.a982246809.replyassistant.ui.main

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.a982246809.replyassistant.R

internal enum class AccessibilityGuideIllustration {
    SERVICE_ENTRY,
    APP_ENTRY,
    ENABLE_SWITCH
}

internal data class AccessibilityGuideStep(
    val number: Int,
    val title: String,
    val description: String,
    val illustration: AccessibilityGuideIllustration,
    val illustrationDescription: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FirstUseGuideScreen(
    state: FirstUseGuideUiState,
    onPrimaryAction: () -> Unit,
    onOpenAppDetails: () -> Unit,
    onLater: () -> Unit,
    supportsRestrictedSettings: Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "首次设置",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        bottomBar = {
            FirstUseGuideBottomActions(
                state = state,
                onPrimaryAction = onPrimaryAction,
                onLater = onLater
            )
        }
    ) { paddingValues ->
        when (state.stage) {
            FirstUseGuideStage.ENABLE_ACCESSIBILITY -> AccessibilityGuideContent(
                state = state,
                paddingValues = paddingValues,
                onOpenAppDetails = onOpenAppDetails,
                supportsRestrictedSettings = supportsRestrictedSettings
            )

            FirstUseGuideStage.ENABLE_FLOATING_BALL,
            FirstUseGuideStage.TRY_WECHAT -> ReadyGuideContent(
                state = state,
                paddingValues = paddingValues
            )

            FirstUseGuideStage.HIDDEN -> Unit
        }
    }
}

@Composable
private fun FirstUseGuideBottomActions(
    state: FirstUseGuideUiState,
    onPrimaryAction: () -> Unit,
    onLater: () -> Unit
) {
    Surface(shadowElevation = 8.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, top = 12.dp, end = 24.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = onPrimaryAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text(firstUseGuidePrimaryActionLabel(state))
            }
            TextButton(
                onClick = onLater,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text("稍后设置")
            }
        }
    }
}

@Composable
private fun AccessibilityGuideContent(
    state: FirstUseGuideUiState,
    paddingValues: PaddingValues,
    onOpenAppDetails: () -> Unit,
    supportsRestrictedSettings: Boolean
) {
    val appName = stringResource(R.string.app_name_label)
    val steps = listOf(
        AccessibilityGuideStep(
            number = 1,
            title = "找到应用服务",
            description = "进入“已下载的应用”或“已安装的服务”。部分手机会直接显示应用列表。",
            illustration = AccessibilityGuideIllustration.SERVICE_ENTRY,
            illustrationDescription = "步骤 1 示意图：在无障碍设置中找到应用服务入口"
        ),
        AccessibilityGuideStep(
            number = 2,
            title = "选择 $appName",
            description = "认准彩虹图标。测试版或开发版的名称可能带有对应后缀。",
            illustration = AccessibilityGuideIllustration.APP_ENTRY,
            illustrationDescription = "步骤 2 示意图：在应用服务列表中选择 $appName"
        ),
        AccessibilityGuideStep(
            number = 3,
            title = "打开“使用 $appName”",
            description = "打开“使用 $appName”开关，在系统确认框中选择允许，然后返回应用。",
            illustration = AccessibilityGuideIllustration.ENABLE_SWITCH,
            illustrationDescription = "步骤 3 示意图：打开使用 $appName 开关并确认允许"
        )
    )
    var showManualTroubleshooting by rememberSaveable { mutableStateOf(false) }
    val showLegacyTroubleshooting =
        showManualTroubleshooting || state.showAccessibilityTroubleshooting

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 16.dp,
            end = 20.dp,
            bottom = 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "照着图开启无障碍服务",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        item { AccessibilityDisclosure() }
        if (supportsRestrictedSettings) {
            item {
                RestrictedSettingsHelpCard(
                    showErrorState = state.showAccessibilityTroubleshooting,
                    appName = appName,
                    supportsRestrictedSettings = true,
                    onOpenAppDetails = onOpenAppDetails
                )
            }
        }
        items(steps.size, key = { steps[it].number }) { index ->
            AccessibilityGuideStepCard(
                step = steps[index],
                appName = appName
            )
        }
        if (!supportsRestrictedSettings) {
            if (!showLegacyTroubleshooting) {
                item {
                    TextButton(
                        onClick = { showManualTroubleshooting = true },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("开关点不了？")
                    }
                }
            } else {
                item {
                    RestrictedSettingsHelpCard(
                        showErrorState = state.showAccessibilityTroubleshooting,
                        appName = appName,
                        supportsRestrictedSettings = false,
                        onOpenAppDetails = onOpenAppDetails
                    )
                }
            }
        }
    }
}

@Composable
private fun AccessibilityDisclosure() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "为什么需要开启？",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "用于读取微信聊天内容并提供回复建议，可随时关闭。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun AccessibilityGuideStepCard(
    step: AccessibilityGuideStep,
    appName: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = step.number.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = step.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            AccessibilityGuideIllustration(
                type = step.illustration,
                appName = appName,
                contentDescription = step.illustrationDescription
            )
        }
    }
}

@Composable
private fun AccessibilityGuideIllustration(
    type: AccessibilityGuideIllustration,
    appName: String,
    contentDescription: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { this.contentDescription = contentDescription },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        when (type) {
            AccessibilityGuideIllustration.SERVICE_ENTRY -> ServiceEntryIllustration()
            AccessibilityGuideIllustration.APP_ENTRY -> AppEntryIllustration(appName)
            AccessibilityGuideIllustration.ENABLE_SWITCH -> EnableSwitchIllustration(appName)
        }
    }
}

@Composable
private fun SettingsIllustrationHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "‹",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ServiceEntryIllustration() {
    Column {
        SettingsIllustrationHeader("无障碍")
        IllustrationSettingRow("屏幕阅读器")
        IllustrationSettingRow(
            title = "已下载的应用",
            supportingText = "已安装的服务",
            highlighted = true
        )
        IllustrationSettingRow("更多无障碍选项")
    }
}

@Composable
private fun AppEntryIllustration(appName: String) {
    Column {
        SettingsIllustrationHeader("已下载的应用")
        IllustrationSettingRow("其他应用服务")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 5.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(34.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "关闭",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "›",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        IllustrationSettingRow("更多服务")
    }
}

@Composable
private fun EnableSwitchIllustration(appName: String) {
    Column {
        SettingsIllustrationHeader(appName)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "使用 $appName",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Switch(checked = true, onCheckedChange = null)
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 4.dp,
            shadowElevation = 3.dp
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "允许 $appName 使用无障碍服务？",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "确认系统提示后返回应用",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "允许",
                    modifier = Modifier.align(Alignment.End),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun IllustrationSettingRow(
    title: String,
    supportingText: String? = null,
    highlighted: Boolean = false
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .then(
                if (highlighted) {
                    Modifier
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(2.dp, MaterialTheme.colorScheme.primary, shape)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal
            )
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = "›",
            style = MaterialTheme.typography.titleMedium,
            color = if (highlighted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun RestrictedSettingsHelpCard(
    showErrorState: Boolean,
    appName: String,
    supportsRestrictedSettings: Boolean,
    onOpenAppDetails: () -> Unit
) {
    val containerColor = if (showErrorState) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = if (showErrorState) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }
    val accentColor = if (showErrorState) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }
    val onAccentColor = if (showErrorState) {
        MaterialTheme.colorScheme.onError
    } else {
        MaterialTheme.colorScheme.onPrimary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (showErrorState) {
                    "还没有检测到无障碍服务"
                } else if (supportsRestrictedSettings) {
                    "先允许受限制的设置"
                } else {
                    "仍然找不到开关？"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (supportsRestrictedSettings) {
                Text(
                    text = if (showErrorState) {
                        "请确认已完成以下设置，再重新前往无障碍设置："
                    } else {
                        "侧载安装后，通常需要先完成这一步，否则无障碍开关可能无法打开："
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                NumberedInstruction(1, "进入 $appName 的应用信息页面", accentColor, onAccentColor)
                NumberedInstruction(2, "点击右上角更多", accentColor, onAccentColor)
                NumberedInstruction(3, "选择“允许受限制的设置”", accentColor, onAccentColor)
                NumberedInstruction(4, "返回无障碍页面，再打开 $appName", accentColor, onAccentColor)
                OutlinedButton(
                    onClick = onOpenAppDetails,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = accentColor
                    ),
                    border = BorderStroke(1.dp, accentColor)
                ) {
                    Text("打开 $appName 应用信息")
                }
            } else {
                Text(
                    text = "请确认已进入系统无障碍的“已下载的应用”或“已安装的服务”，找到 $appName 后打开开关。不同厂商的菜单名称可能不同。",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun NumberedInstruction(
    number: Int,
    text: String,
    accentColor: Color,
    onAccentColor: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(24.dp),
            shape = CircleShape,
            color = accentColor
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = onAccentColor
                )
            }
        }
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ReadyGuideContent(
    state: FirstUseGuideUiState,
    paddingValues: PaddingValues
) {
    val isReadyToTry = state.stage == FirstUseGuideStage.TRY_WECHAT
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(
            start = 24.dp,
            top = 20.dp,
            end = 24.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "应用图标",
                    modifier = Modifier.size(72.dp)
                )
                Text(
                    text = if (isReadyToTry && state.isAutoStartRequired) {
                        "开启本应用自启动"
                    } else if (isReadyToTry) {
                        "设置完成，去微信试试看"
                    } else {
                        "无障碍服务已开启"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = firstUseGuideDescription(state),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            FirstUseGuideProgressCard(
                stage = state.stage,
                isAutoStartRequired = state.isAutoStartRequired
            )
        }
        if (isReadyToTry) {
            item {
                AutoStartGuideCard(isAutoStartRequired = state.isAutoStartRequired)
            }
        }
    }
}

@Composable
private fun FirstUseGuideProgressCard(
    stage: FirstUseGuideStage,
    isAutoStartRequired: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FirstUseGuideProgressRow(
                completed = true,
                marker = "1",
                text = "开启本应用无障碍服务"
            )
            FirstUseGuideProgressRow(
                completed = stage == FirstUseGuideStage.TRY_WECHAT,
                marker = "2",
                text = "启用应用内悬浮球"
            )
            FirstUseGuideProgressRow(
                completed = stage == FirstUseGuideStage.TRY_WECHAT && !isAutoStartRequired,
                marker = "3",
                text = "开启本应用自启动"
            )
            FirstUseGuideProgressRow(
                completed = false,
                marker = "4",
                text = "在微信单聊中看到第一条回复建议"
            )
        }
    }
}

@Composable
private fun FirstUseGuideProgressRow(
    completed: Boolean,
    marker: String,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    color = if (completed) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (completed) "✓" else marker,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (completed) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AutoStartGuideCard(isAutoStartRequired: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isAutoStartRequired) {
                    "开启本应用自启动"
                } else {
                    "自启动设置已前往"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isAutoStartRequired) {
                    "部分手机重启或清理后台后可能停止服务。请前往系统设置开启本应用自启动；我们无法检测该开关是否已开启。"
                } else {
                    "自启动设置已前往，请确认开关已开启。我们无法检测该开关状态。"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun FirstUseGuideCompactCard(
    state: FirstUseGuideUiState,
    onExpand: () -> Unit
) {
    Card(
        onClick = onExpand,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "继续完成首次设置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = firstUseGuideCollapsedDescription(state),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "›",
                modifier = Modifier.clearAndSetSemantics { },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

internal fun firstUseGuideDescription(state: FirstUseGuideUiState): String = when (state.stage) {
    FirstUseGuideStage.ENABLE_ACCESSIBILITY -> if (state.activationRequested) {
        "尚未检测到无障碍服务。完成排障后请重新前往无障碍设置。"
    } else {
        "按照页面中的三步提示开启本应用无障碍服务。"
    }

    FirstUseGuideStage.ENABLE_FLOATING_BALL ->
        "无障碍服务已开启。再启用应用内悬浮球，即可在微信单聊中使用。"

    FirstUseGuideStage.TRY_WECHAT -> if (state.isAutoStartRequired) {
        "请在系统设置中开启本应用自启动，避免重启或清理后后台服务停止。"
    } else {
        "悬浮球已启用。收到新消息时会自动显示建议，也可以点击悬浮球主动生成。"
    }

    FirstUseGuideStage.HIDDEN -> ""
}

internal fun firstUseGuidePrimaryActionLabel(state: FirstUseGuideUiState): String = when (state.stage) {
    FirstUseGuideStage.ENABLE_ACCESSIBILITY -> if (state.activationRequested) {
        "重新前往无障碍设置"
    } else {
        "去无障碍设置"
    }

    FirstUseGuideStage.ENABLE_FLOATING_BALL -> "启用悬浮球"
    FirstUseGuideStage.TRY_WECHAT -> if (state.isAutoStartRequired) {
        "去开启自启动"
    } else {
        "打开微信"
    }
    FirstUseGuideStage.HIDDEN -> ""
}

internal fun firstUseGuideCollapsedDescription(state: FirstUseGuideUiState): String = when (state.stage) {
    FirstUseGuideStage.ENABLE_ACCESSIBILITY -> "还需开启无障碍服务"
    FirstUseGuideStage.ENABLE_FLOATING_BALL -> "还需启用悬浮球"
    FirstUseGuideStage.TRY_WECHAT -> if (state.isAutoStartRequired) {
        "还需前往自启动设置"
    } else {
        "去微信查看第一条回复建议"
    }
    FirstUseGuideStage.HIDDEN -> ""
}
