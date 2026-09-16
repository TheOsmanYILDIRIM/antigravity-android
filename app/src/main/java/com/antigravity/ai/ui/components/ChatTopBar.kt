package com.antigravity.ai.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import kotlinx.coroutines.delay

@Composable
fun ChatTopBar(
    settings: ChatSettings,
    usage: UsageData?,
    isGenerating: Boolean,
    modelName: String? = null,
    projectName: String? = null,
    sessionTokens: Int = 0,
    sessionDataBytes: Long = 0L,
    onModelClick: (() -> Unit)? = null,
    onMenuClick: () -> Unit,
    onNewChatClick: () -> Unit,
    onUsageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Dynamic Subtitle state: auto-reveals on change / generating or periodic gentle ticker
    var showStatsSubtitle by remember { mutableStateOf(false) }

    // Auto-reveal on token updates or while generating
    LaunchedEffect(sessionTokens, sessionDataBytes, isGenerating) {
        if (isGenerating) {
            showStatsSubtitle = true
        } else if (sessionTokens > 0) {
            showStatsSubtitle = true
            delay(4000)
            showStatsSubtitle = false
        }
    }

    // Periodic gentle ticker (arada bir 3.5 saniyeliğine kendi kendine açılıp kapanır)
    LaunchedEffect(Unit) {
        while (true) {
            delay(20000) // Her 20 saniyede bir
            if (!isGenerating && sessionTokens > 0) {
                showStatsSubtitle = true
                delay(3500)
                showStatsSubtitle = false
            }
        }
    }

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
            // Left: Hamburger Menu
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

            // Center: Brand Title + Project Badge + (Animated Expanding Subtitle)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        // Tapping toggles stats visibility or opens model sheet if provided
                        showStatsSubtitle = !showStatsSubtitle
                    }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
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
                        if (!projectName.isNullOrBlank()) {
                            val pColor = ProjectColorUtil.getColorForProject(projectName)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = pColor.copy(alpha = 0.18f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, pColor.copy(alpha = 0.45f)),
                                modifier = Modifier.padding(start = 6.dp)
                            ) {
                                Text(
                                    text = projectName,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = pColor,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    // Smooth Auto Expanding & Collapsing Subtitle
                    AnimatedVisibility(
                        visible = showStatsSubtitle && (sessionTokens > 0 || sessionDataBytes > 0 || isGenerating),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        val tokenDisplay = if (sessionTokens >= 1_000_000) {
                            String.format("%.2fM", sessionTokens / 1_000_000f) + " bağlam"
                        } else if (sessionTokens >= 1000) {
                            String.format("%.1fk", sessionTokens / 1000f) + " bağlam"
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
                            text = if (isGenerating) "⚡ Üretiliyor • $tokenDisplay" else "📊 $tokenDisplay • 💾 $dataDisplay",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isGenerating) GeminiBlue else PrimaryIndigo,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
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

