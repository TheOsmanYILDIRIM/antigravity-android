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

            Spacer(modifier = Modifier.width(10.dp))

            // Center: Gemini Sparkle + Antigravity Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                GeminiSparkleIcon(size = 20.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Antigravity",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = settings.model.replace("gemini-", "Gemini ").replace("-medium", "").replace("-high", " ⚡"),
                        fontSize = 11.sp,
                        color = TextMuted,
                        maxLines = 1
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
