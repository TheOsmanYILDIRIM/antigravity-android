package com.antigravity.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.ai.data.model.ConversationMeta
import com.antigravity.ai.ui.theme.*

@Composable
fun ChatDrawer(
    conversations: List<ConversationMeta>,
    currentSessionId: String?,
    onSelectConversation: (String) -> Unit,
    onNewChat: () -> Unit,
    onDeleteConversation: (String) -> Unit,
    onOpenVault: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = BackgroundDark,
        drawerContentColor = TextPrimary,
        modifier = Modifier.width(310.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.linearGradient(listOf(PrimaryIndigo, SecondaryPurple))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Antigravity AI",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Termux Workspaces",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            // New Chat Button
            Button(
                onClick = onNewChat,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Yeni Sohbet Başlat",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // AGY Vault Quick Access Button
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceVariantDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpenVault() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Storage, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = "AGY Vault Hafızası", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = TextPrimary)
                        Text(text = "Obsidian dökümanları & notlar", fontSize = 10.sp, color = TextMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "GEÇMİŞ SOHBETLER (${conversations.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            if (conversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Henüz kayıtlı sohbet yok",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(conversations, key = { it.id }) { conv ->
                        val isSelected = conv.id == currentSessionId
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) SurfaceVariantDark else Color.Transparent)
                                .clickable { onSelectConversation(conv.id) }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = if (isSelected) PrimaryIndigo else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = conv.title.ifEmpty { "Sohbet #${conv.id.takeLast(6)}" },
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${conv.messageCount} mesaj",
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }
                            IconButton(
                                onClick = { onDeleteConversation(conv.id) },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Sil",
                                    tint = TextMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }

            Divider(color = BorderSubtle, modifier = Modifier.padding(vertical = 12.dp))

            // Footer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Yerel Daemon: 127.0.0.1:8080",
                    fontSize = 11.sp,
                    color = TextMuted
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(SuccessGreen)
                )
            }
        }
    }
}
