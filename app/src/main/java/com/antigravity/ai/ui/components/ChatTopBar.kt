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
    // 1. Initial title collapse animation (Antigravity yazısı açılışta yıldızın içine katlanır)
    var showTitleText by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(2500)
        showTitleText = false
    }

    // 2. Stats Subtitle state: only shows briefly on update or generating (no aggressive periodic ticker)
    var showStatsSubtitle by remember { mutableStateOf(false) }
    LaunchedEffect(sessionTokens, isGenerating) {
        if (isGenerating) {
            showStatsSubtitle = true
        } else if (sessionTokens > 0) {
            showStatsSubtitle = true
            delay(3000)
            showStatsSubtitle = false
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
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SurfaceVariantDark)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menü",
                    tint = TextPrimary,
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Center: Sparkle Icon + (Folding Title) + Project Badge + (Auto-collapsing stats)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        // Tapping toggles title text and stats
                        showTitleText = !showTitleText
                        showStatsSubtitle = !showStatsSubtitle
                    }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                GeminiSparkleIcon(size = 20.dp)

                Column(modifier = Modifier.padding(start = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Animated Folding Title ("Antigravity" collapses into the sparkle icon)
                        AnimatedVisibility(
                            visible = showTitleText,
                            enter = expandHorizontally() + fadeIn(),
                            exit = shrinkHorizontally() + fadeOut()
                        ) {
                            Text(
                                text = "Antigravity",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = TextPrimary,
                                maxLines = 1,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        }

                        if (!projectName.isNullOrBlank()) {
                            val pColor = ProjectColorUtil.getColorForProject(projectName)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = pColor.copy(alpha = 0.18f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, pColor.copy(alpha = 0.45f))
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

                    // Smooth Auto Expanding & Collapsing Subtitle (Concise token & data format)
                    AnimatedVisibility(
                        visible = showStatsSubtitle && (sessionTokens > 0 || isGenerating),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        val tokenDisplay = if (sessionTokens >= 1_000_000) {
                            String.format("%.1fM", sessionTokens / 1_000_000f)
                        } else if (sessionTokens >= 1000) {
                            String.format("%.1fk", sessionTokens / 1000f)
                        } else {
                            "$sessionTokens"
                        }

                        val dataDisplay = if (sessionDataBytes >= 1024 * 1024) {
                            String.format("%.1fMB", sessionDataBytes / (1024f * 1024f))
                        } else if (sessionDataBytes >= 1024) {
                            String.format("%.1fKB", sessionDataBytes / 1024f)
                        } else {
                            "${sessionDataBytes}B"
                        }

                        Text(
                            text = if (isGenerating) "⚡ Üretiliyor • $tokenDisplay tok" else "$tokenDisplay tok • $dataDisplay",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isGenerating) GeminiBlue else PrimaryIndigo,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Right Group: Usage Token Meter & Scaled Down (30% smaller) New Chat Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                UsageWidget(
                    usage = usage,
                    onClick = onUsageClick
                )

                // New Chat Button (Reduced by ~30% from 38dp to 28dp)
                IconButton(
                    onClick = onNewChatClick,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(SurfaceVariantDark)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Yeni Sohbet",
                        tint = GeminiBlue,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

