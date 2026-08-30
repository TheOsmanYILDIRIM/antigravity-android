package com.antigravity.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.ai.data.api.ServerHealth
import com.antigravity.ai.data.model.ChatSettings
import com.antigravity.ai.data.model.UsageData
import com.antigravity.ai.ui.theme.*

@Composable
fun ChatTopBar(
    settings: ChatSettings,
    usage: UsageData?,
    isGenerating: Boolean,
    onMenuClick: () -> Unit,
    onNewChatClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onUsageClick: () -> Unit,
    onFileManagerClick: () -> Unit = {},
    serverHealth: ServerHealth? = null,
    onStartServer: () -> Unit = {},
    onStopServer: () -> Unit = {},
    isKeepAliveRunning: Boolean = false,
    onToggleKeepAlive: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        color = BackgroundDark,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            // Left: Hamburger Menu
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menü",
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Center: Gemini Sparkle + Antigravity Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSettingsClick() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                GeminiSparkleIcon(size = 18.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "Antigravity",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${settings.fontSizeSp.toInt()}sp • ${if (settings.thermalMode == "eco") "❄️ %50 CPU" else "⚡ Tam Hız"}",
                        fontSize = 10.sp,
                        color = TextMuted,
                        maxLines = 1
                    )
                }
            }

            // Right Group: Usage, Settings & New Chat
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Server Quick Power / Status Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (serverHealth?.isOnline == true) SuccessGreen.copy(alpha = 0.15f) else DangerRed.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (serverHealth?.isOnline == true) SuccessGreen.copy(alpha = 0.6f) else DangerRed.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (serverHealth?.isOnline == true) {
                                onSettingsClick()
                            } else {
                                onStartServer()
                            }
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (serverHealth?.isOnline == true) SuccessGreen else DangerRed)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (serverHealth?.isOnline == true) "${serverHealth.latencyMs}ms" else "⚡ Başlat",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (serverHealth?.isOnline == true) SuccessGreen else DangerRed
                        )
                    }
                }

                UsageWidget(
                    usage = usage,
                    onClick = onUsageClick
                )

                // Termux File Manager Button
                IconButton(
                    onClick = onFileManagerClick,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(SurfaceVariantDark)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Folder,
                        contentDescription = "Termux Dosyaları",
                        tint = GeminiPurple,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Dedicated Gemini Settings Button
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(SurfaceVariantDark)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = "Ayarlar",
                        tint = GeminiBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // New Chat Button
                IconButton(
                    onClick = onNewChatClick,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(SurfaceVariantDark)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "Yeni Sohbet",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
