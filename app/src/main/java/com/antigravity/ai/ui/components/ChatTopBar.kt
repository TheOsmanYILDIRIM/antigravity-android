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
import com.antigravity.ai.data.model.ChatSettings
import com.antigravity.ai.data.model.UsageData
import com.antigravity.ai.ui.theme.*

@Composable
fun ChatTopBar(
    settings: ChatSettings,
    usage: UsageData?,
    isGenerating: Boolean,
    modelName: String? = null,
    sessionTokens: Int = 0,
    sessionDataBytes: Long = 0L,
    onModelClick: (() -> Unit)? = null,
    onMenuClick: () -> Unit,
    onNewChatClick: () -> Unit,
    onUsageClick: () -> Unit,
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
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // Left: Hamburger Menu (Opens Chats & Settings)
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(SurfaceVariantDark)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menü",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Center: Gemini Sparkle + Antigravity Title & Live Token/Session Data Size Stats
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (onModelClick != null) {
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onModelClick() }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        } else Modifier
                    )
            ) {
                GeminiSparkleIcon(size = 20.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Antigravity",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        if (onModelClick != null) {
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Ayarlar",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Format live active tokens & total session context data size
                    val tokenDisplay = if (sessionTokens >= 1000) {
                        String.format("%.1fk", sessionTokens / 1000f) + " tok"
                    } else {
                        "$sessionTokens tok"
                    }

                    val dataDisplay = if (sessionDataBytes >= 1024 * 1024) {
                        String.format("%.1f MB", sessionDataBytes / (1024f * 1024f))
                    } else if (sessionDataBytes >= 1024) {
                        String.format("%.1f KB", sessionDataBytes / 1024f)
                    } else {
                        "${sessionDataBytes} B"
                    }

                    Text(
                        text = "📊 $tokenDisplay • 💾 $dataDisplay",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryIndigo,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            // Right Group: Usage Token Meter & New Chat
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                UsageWidget(
                    usage = usage,
                    onClick = onUsageClick
                )

                // New Chat Button
                IconButton(
                    onClick = onNewChatClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(SurfaceVariantDark)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Yeni Sohbet",
                        tint = GeminiBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
