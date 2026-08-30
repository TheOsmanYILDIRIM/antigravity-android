package com.antigravity.ai.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.ai.data.api.ServerHealth
import com.antigravity.ai.data.model.ConversationMeta
import com.antigravity.ai.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatDrawer(
    conversations: List<ConversationMeta>,
    currentSessionId: String?,
    pinnedIds: Set<String> = emptySet(),
    onSelectConversation: (String) -> Unit,
    onNewChat: () -> Unit,
    onDeleteConversation: (String) -> Unit,
    onTogglePinConversation: (String) -> Unit,
    onExportSingleConversation: (String) -> Unit,
    onExportAllConversations: () -> Unit,
    onOpenVault: () -> Unit,
    onOpenFileManager: () -> Unit = {},
    onOpenSettings: () -> Unit,
    serverHealth: ServerHealth? = null,
    onStartServer: () -> Unit = {},
    onStopServer: () -> Unit = {},
    onExitApp: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var contextMenuConv by remember { mutableStateOf<ConversationMeta?>(null) }

    // Split and filter conversations
    val (pinnedList, unpinnedList) = remember(conversations, pinnedIds, searchQuery) {
        val filtered = if (searchQuery.isBlank()) {
            conversations
        } else {
            conversations.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }
        val pinned = filtered.filter { pinnedIds.contains(it.id) }
        val unpinned = filtered.filter { !pinnedIds.contains(it.id) }
        Pair(pinned, unpinned)
    }

    ModalDrawerSheet(
        drawerContainerColor = SurfaceDark,
        drawerContentColor = TextPrimary,
        modifier = modifier
            .width(320.dp)
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // 1. Search Bar (Figma: "Search for chats")
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = SurfaceVariantDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Ara",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(text = "Search for chats", color = TextMuted, fontSize = 14.sp)
                        }
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = TextPrimary,
                                fontSize = 14.sp
                            ),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(GeminiBlue),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Outlined.Clear,
                            contentDescription = "Temizle",
                            tint = TextMuted,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. New Chat Action Button (Figma: [+ New Chat])
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNewChat() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Yeni Sohbet",
                        tint = TextPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "New Chat",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.5.sp,
                        color = TextPrimary
                    )
                }
            }

            // 3. My Stuff (AGY Vault Library & Notes)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenVault() }
                    .padding(vertical = 8.dp, horizontal = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Folder,
                        contentDescription = null,
                        tint = GeminiBlue,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "My Stuff (Vault)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.5.sp,
                        color = TextPrimary
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            // 3.5 Termux File Manager & Projects
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenFileManager() }
                    .padding(vertical = 8.dp, horizontal = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FolderSpecial,
                        contentDescription = null,
                        tint = GeminiPurple,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Termux Dosyaları & Projeler",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.5.sp,
                        color = TextPrimary
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            Divider(color = BorderSubtle, modifier = Modifier.padding(vertical = 8.dp))

            // 4. Conversations List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Pinned Section
                if (pinnedList.isNotEmpty()) {
                    item {
                        Text(
                            text = "Sabitlenenler (Pinned)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GeminiBlue,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }

                    items(pinnedList, key = { "pinned_${it.id}" }) { conv ->
                        val isSelected = conv.id == currentSessionId
                        ConversationRowItem(
                            conv = conv,
                            isSelected = isSelected,
                            isPinned = true,
                            onSelect = { onSelectConversation(conv.id) },
                            onLongPress = { contextMenuConv = conv }
                        )
                    }

                    item {
                        Divider(color = BorderSubtle.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 6.dp))
                    }
                }

                // Recent Chats Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Arama Sonuçları (${pinnedList.size + unpinnedList.size})" else "Chats",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                    }
                }

                if (unpinnedList.isEmpty() && pinnedList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isNotEmpty()) "Eşleşen sohbet bulunamadı" else "Henüz sohbet yok",
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        }
                    }
                }

                items(unpinnedList, key = { it.id }) { conv ->
                    val isSelected = conv.id == currentSessionId
                    ConversationRowItem(
                        conv = conv,
                        isSelected = isSelected,
                        isPinned = false,
                        onSelect = { onSelectConversation(conv.id) },
                        onLongPress = { contextMenuConv = conv }
                    )
                }
            }

            Divider(color = BorderSubtle, modifier = Modifier.padding(vertical = 8.dp))

            // Termux Server Quick Status & Toggle Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceVariantDark,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (serverHealth?.isOnline == true) SuccessGreen.copy(alpha = 0.5f) else DangerRed.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (serverHealth?.isOnline == true) SuccessGreen else DangerRed)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Termux agy-web",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = if (serverHealth?.isOnline == true) "🟢 Online (${serverHealth.latencyMs}ms)" else "🔴 Offline",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (serverHealth?.isOnline == true) SuccessGreen else DangerRed
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = onStartServer,
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f).height(30.dp)
                        ) {
                            Text("⚡ Başlat", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        OutlinedButton(
                            onClick = onStopServer,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f).height(30.dp)
                        ) {
                            Text("🛑 Durdur", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom Actions: Export All & Settings Access
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Export All Sessions Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceVariantDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onExportAllConversations() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FileDownload,
                            contentDescription = "Dışa Aktar",
                            tint = GeminiPurple,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tümünü Aktar",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }

                // Ayarlar (Settings) Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceVariantDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenSettings() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Ayarlar",
                            tint = GeminiBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ayarlar",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }
            }

            if (onExitApp != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DangerRed.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExitApp() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Çıkış",
                            tint = DangerRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Çıkış & Sunucuyu Kapat",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DangerRed
                        )
                    }
                }
            }
        }
    }

    // Long Press Context Menu Dialog for Chat Item
    contextMenuConv?.let { conv ->
        val isPinned = pinnedIds.contains(conv.id)
        AlertDialog(
            onDismissRequest = { contextMenuConv = null },
            title = {
                Text(
                    text = conv.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Sabitle / Sabitlemeyi Kaldır
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onTogglePinConversation(conv.id)
                                contextMenuConv = null
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = null,
                                tint = GeminiBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isPinned) "Sabitlemeyi Kaldır" else "En Üste Sabitle",
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                        }
                    }

                    // Bu Sohbeti Dışa Aktar
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onExportSingleConversation(conv.id)
                                contextMenuConv = null
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = null,
                                tint = GeminiPurple,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Sohbeti Dışa Aktar (Markdown)",
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                        }
                    }

                    // Sohbeti Sil
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDeleteConversation(conv.id)
                                contextMenuConv = null
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = null,
                                tint = DangerRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Sohbeti Sil",
                                fontSize = 14.sp,
                                color = DangerRed
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { contextMenuConv = null }) {
                    Text("Kapat", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark,
            titleContentColor = TextPrimary,
            textContentColor = TextPrimary
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationRowItem(
    conv: ConversationMeta,
    isSelected: Boolean,
    isPinned: Boolean,
    onSelect: () -> Unit,
    onLongPress: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) SurfaceSelected else Color.Transparent)
            .combinedClickable(
                onClick = onSelect,
                onLongClick = onLongPress
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        if (isPinned) {
            Icon(
                imageVector = Icons.Filled.PushPin,
                contentDescription = "Sabitlendi",
                tint = GeminiBlue,
                modifier = Modifier
                    .size(14.dp)
                    .padding(end = 4.dp)
            )
        }

        Text(
            text = conv.title,
            fontSize = 13.5.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) TextPrimary else TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
