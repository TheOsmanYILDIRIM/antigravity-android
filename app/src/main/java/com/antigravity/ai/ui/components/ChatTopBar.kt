package com.antigravity.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Tune
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    settings: ChatSettings,
    usage: UsageData?,
    isGenerating: Boolean,
    onMenuClick: () -> Unit,
    onNewChatClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onUsageClick: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Antigravity AI",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isGenerating) WarningAmber else SuccessGreen)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isGenerating) "İşlem yürütülüyor…" else "${settings.model.replace("gemini-", "").replace("default", "CLI")} • ${settings.effort}",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(imageVector = Icons.Default.Menu, contentDescription = "Menü", tint = TextSecondary)
            }
        },
        actions = {
            // Real-Time Quota & Usage Widget
            UsageWidget(
                usage = usage,
                onClick = onUsageClick,
                modifier = Modifier.padding(end = 4.dp)
            )

            // Model & Settings button
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SurfaceVariantDark,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSettingsClick() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                ) {
                    Icon(imageVector = Icons.Default.Tune, contentDescription = "Ayarlar", tint = PrimaryIndigo, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = settings.effort.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryIndigo
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // New chat
            IconButton(
                onClick = onNewChatClick,
                modifier = Modifier
                    .padding(end = 2.dp)
                    .clip(CircleShape)
                    .background(SurfaceVariantDark)
                    .size(34.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Yeni Sohbet", tint = PrimaryIndigo, modifier = Modifier.size(18.dp))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
    )
}
