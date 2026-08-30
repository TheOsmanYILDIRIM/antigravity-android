package com.antigravity.ai.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.ai.data.api.ServerHealth
import com.antigravity.ai.data.model.FsItem
import com.antigravity.ai.data.model.ProjectItem
import com.antigravity.ai.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TermuxFileManagerScreen(
    currentDir: String,
    parentDir: String?,
    homeDir: String,
    items: List<FsItem>,
    projects: List<ProjectItem>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onNavigateToDir: (String) -> Unit,
    onOpenFile: (String) -> Unit,
    onOpenImage: (String, String) -> Unit,
    onAttachToChat: (String) -> Unit,
    onMentionInChat: (String) -> Unit,
    onRefresh: () -> Unit,
    serverHealth: ServerHealth? = null,
    onStartServer: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: Dosya Gezgini, 1: Projeler
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var selectedContextItem by remember { mutableStateOf<FsItem?>(null) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        color = BackgroundDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // 1. Top Bar
            Surface(
                color = SurfaceDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceVariantDark)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Kapat",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = "Termux Dosyaları & Projeler",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${projects.size} Proje • ${items.size} Öğe",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        // Right icons (Search & Refresh)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = { isSearching = !isSearching; if (!isSearching) searchQuery = "" },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceVariantDark)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = "Ara",
                                    tint = if (isSearching) GeminiBlue else TextPrimary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            IconButton(
                                onClick = onRefresh,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceVariantDark)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = "Yenile",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }

                    // Search input field
                    AnimatedVisibility(visible = isSearching) {
                        Surface(
                            color = SurfaceVariantDark,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                androidx.compose.foundation.text.BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        color = TextPrimary,
                                        fontSize = 13.5.sp
                                    ),
                                    cursorBrush = androidx.compose.ui.graphics.SolidColor(GeminiBlue),
                                    modifier = Modifier.weight(1f)
                                )
                                if (searchQuery.isNotEmpty()) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Temizle",
                                        tint = TextMuted,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { searchQuery = "" }
                                    )
                                }
                            }
                        }
                    }

                    // Tab Selector (Dosya Gezgini / Projeler)
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = SurfaceDark,
                        contentColor = TextPrimary,
                        divider = { Divider(color = BorderSubtle) }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Dosya Gezgini (${items.size})", fontSize = 13.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                                }
                            },
                            selectedContentColor = GeminiBlue,
                            unselectedContentColor = TextMuted
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Projelerim (${projects.size})", fontSize = 13.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                                }
                            },
                            selectedContentColor = GeminiPurple,
                            unselectedContentColor = TextMuted
                        )
                    }
                }
            }

            // 2. Breadcrumbs & Quick Shortcuts Bar (Only when in File Explorer tab)
            if (selectedTab == 0) {
                Surface(
                    color = SurfaceVariantDark.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        // Quick shortcuts
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            QuickLocationChip(
                                label = "🏠 Ev (~)",
                                isSelected = currentDir == homeDir,
                                onClick = { onNavigateToDir(homeDir) }
                            )
                            QuickLocationChip(
                                label = "📱 Android Projesi",
                                isSelected = currentDir.contains("antigravity-android"),
                                onClick = { onNavigateToDir("$homeDir/antigravity-android") }
                            )
                            QuickLocationChip(
                                label = "📚 AGY Vault",
                                isSelected = currentDir.contains("agy-vault"),
                                onClick = { onNavigateToDir("$homeDir/agy-vault") }
                            )
                            QuickLocationChip(
                                label = "💾 Yüklenenler",
                                isSelected = currentDir.contains("uploads"),
                                onClick = { onNavigateToDir("$homeDir/uploads") }
                            )
                            QuickLocationChip(
                                label = "🧠 Brain",
                                isSelected = currentDir.contains("brain"),
                                onClick = { onNavigateToDir("$homeDir/.gemini/antigravity-cli/brain") }
                            )
                            QuickLocationChip(
                                label = "📂 SDCard / Depolama",
                                isSelected = currentDir.startsWith("/storage") || currentDir.startsWith("/sdcard"),
                                onClick = { onNavigateToDir("/storage/emulated/0") }
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Path Breadcrumbs
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (parentDir != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SurfaceVariantDark,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onNavigateToDir(parentDir) }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowBack,
                                            contentDescription = "Üst Dizin",
                                            tint = GeminiBlue,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Üst Klasör", fontSize = 11.5.sp, color = TextPrimary)
                                    }
                                }
                                Text("/", color = TextMuted, fontSize = 12.sp)
                            }

                            val displayPath = currentDir.replace(homeDir, "~")
                            val parts = displayPath.split("/").filter { it.isNotEmpty() }

                            parts.forEachIndexed { index, part ->
                                val subPath = if (part == "~") homeDir else {
                                    homeDir + "/" + parts.subList(1, index + 1).joinToString("/")
                                }
                                val isCurrent = index == parts.size - 1

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isCurrent) GeminiBlue.copy(alpha = 0.15f) else Color.Transparent,
                                    border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.dp, GeminiBlue.copy(alpha = 0.5f)) else null,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { onNavigateToDir(subPath) }
                                ) {
                                    Text(
                                        text = part,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCurrent) GeminiBlue else TextSecondary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                if (index < parts.size - 1) {
                                    Text("/", color = TextMuted, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 3. Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (isLoading) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = GeminiBlue, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Termux dizini taranıyor…", fontSize = 13.sp, color = TextMuted)
                    }
                } else if (selectedTab == 1) {
                    // Projects View
                    val filteredProjects = if (searchQuery.isBlank()) projects else {
                        projects.filter {
                            it.name.contains(searchQuery, ignoreCase = true) ||
                            it.type.contains(searchQuery, ignoreCase = true) ||
                            (it.description?.contains(searchQuery, ignoreCase = true) == true)
                        }
                    }

                    if (filteredProjects.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isNotEmpty()) "Aramaya uygun proje bulunamadı" else "Termux ana dizininde proje tespit edilmedi",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredProjects, key = { it.path }) { proj ->
                                ProjectCardItem(
                                    project = proj,
                                    onOpenDir = { onNavigateToDir(proj.path); selectedTab = 0 },
                                    onAttach = { onAttachToChat(proj.path) },
                                    onMention = { onMentionInChat(proj.path) }
                                )
                            }
                        }
                    }
                } else {
                    // File Explorer View
                    val filteredItems = if (searchQuery.isBlank()) items else {
                        items.filter { it.name.contains(searchQuery, ignoreCase = true) }
                    }

                    if (filteredItems.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (serverHealth?.isOnline == false && searchQuery.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = SurfaceDark,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PowerSettingsNew,
                                            contentDescription = null,
                                            tint = DangerRed,
                                            modifier = Modifier.size(44.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Termux agy-web Sunucusu Kapalı",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Termux dosya ve projelerine erişebilmek için yerel sunucunun çalışması gerekir.",
                                            fontSize = 12.sp,
                                            color = TextSecondary,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = onStartServer,
                                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("⚡ Sunucuyu Başlat", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = if (searchQuery.isNotEmpty()) "Eşleşen dosya bulunamadı" else "Bu klasör boş",
                                    color = TextMuted,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            items(filteredItems, key = { it.path }) { item ->
                                FsRowItem(
                                    item = item,
                                    onClick = {
                                        if (item.isDirectory) {
                                            onNavigateToDir(item.path)
                                        } else {
                                            val ext = item.extension ?: ""
                                            val isImg = ext in listOf(".png", ".jpg", ".jpeg", ".webp", ".gif", ".svg")
                                            if (isImg) {
                                                onOpenImage(item.path, item.name)
                                            } else {
                                                onOpenFile(item.path)
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        selectedContextItem = item
                                    },
                                    onMenuClick = {
                                        selectedContextItem = item
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Context Menu Dialog on Long Press / Three dots
    selectedContextItem?.let { item ->
        AlertDialog(
            onDismissRequest = { selectedContextItem = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                        contentDescription = null,
                        tint = if (item.isDirectory) GeminiBlue else PrimaryIndigo,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.path,
                        fontSize = 11.5.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    // 1. Sohbete Ekle
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onAttachToChat(item.path)
                                Toast.makeText(context, "Sohbete eklendi", Toast.LENGTH_SHORT).show()
                                selectedContextItem = null
                                onDismiss()
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.AddComment, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Sohbete Dosya Olarak Ekle", fontSize = 13.5.sp, color = TextPrimary)
                        }
                    }

                    // 2. Sohbette @Bahset
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onMentionInChat(item.path)
                                Toast.makeText(context, "Yol mesaja eklendi", Toast.LENGTH_SHORT).show()
                                selectedContextItem = null
                                onDismiss()
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = GeminiBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Sohbette @ ile Bahset", fontSize = 13.5.sp, color = TextPrimary)
                        }
                    }

                    // 3. Dosyayı Aç / Görüntüle
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (item.isDirectory) {
                                    onNavigateToDir(item.path)
                                } else {
                                    val ext = item.extension ?: ""
                                    if (ext in listOf(".png", ".jpg", ".jpeg", ".webp", ".gif", ".svg")) {
                                        onOpenImage(item.path, item.name)
                                    } else {
                                        onOpenFile(item.path)
                                    }
                                }
                                selectedContextItem = null
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
                        ) {
                            Icon(Icons.Outlined.Visibility, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(if (item.isDirectory) "Klasörü Aç" else "Görüntüle / Düzenle", fontSize = 13.5.sp, color = TextPrimary)
                        }
                    }

                    // 4. Yolu Kopyala
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Path", item.path))
                                Toast.makeText(context, "Yol kopyalandı", Toast.LENGTH_SHORT).show()
                                selectedContextItem = null
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
                        ) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Tam Yolu Kopyala", fontSize = 13.5.sp, color = TextPrimary)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedContextItem = null }) {
                    Text("Kapat", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark,
            titleContentColor = TextPrimary,
            textContentColor = TextPrimary
        )
    }
}

@Composable
fun QuickLocationChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) GeminiBlue.copy(alpha = 0.2f) else SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) GeminiBlue else BorderSubtle),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) GeminiBlue else TextPrimary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun ProjectCardItem(
    project: ProjectItem,
    onOpenDir: () -> Unit,
    onAttach: () -> Unit,
    onMention: () -> Unit
) {
    val badgeColor = when (project.type) {
        "Android" -> SuccessGreen
        "Node.js", "Next.js", "React" -> GeminiBlue
        "Python" -> GeminiAmber
        "Rust" -> DangerRed
        "Go" -> Color(0xFF00ADD8)
        else -> GeminiPurple
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onOpenDir() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.FolderSpecial,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = project.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Project Type Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = project.type,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (!project.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = project.description,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (project.gitBranch != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.AltRoute,
                            contentDescription = null,
                            tint = GeminiPurple,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = project.gitBranch,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = GeminiPurple
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceVariantDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onMention() }
                    ) {
                        Text(
                            text = "@Bahset",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GeminiBlue,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceVariantDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onAttach() }
                    ) {
                        Text(
                            text = "Sohbete Ekle",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryIndigo,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FsRowItem(
    item: FsItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    val isImg = item.extension in listOf(".png", ".jpg", ".jpeg", ".webp", ".gif", ".svg")
    val isCode = item.extension in listOf(".kt", ".java", ".js", ".ts", ".py", ".rs", ".go", ".c", ".cpp", ".html", ".css", ".json", ".sh", ".gradle", ".xml", ".yaml", ".toml")
    val isMd = item.extension == ".md"

    val icon = when {
        item.isDirectory && item.isProject -> Icons.Default.FolderSpecial
        item.isDirectory -> Icons.Default.Folder
        isImg -> Icons.Default.Image
        isCode -> Icons.Default.Code
        isMd -> Icons.Default.Description
        else -> Icons.Default.InsertDriveFile
    }

    val iconColor = when {
        item.isDirectory && item.isProject -> GeminiPurple
        item.isDirectory -> GeminiBlue
        isImg -> Color(0xFF4CAF50)
        isCode -> Color(0xFF29B6F6)
        isMd -> Color(0xFFAB47BC)
        else -> TextMuted
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = item.name,
                        fontSize = 13.5.sp,
                        fontWeight = if (item.isDirectory) FontWeight.SemiBold else FontWeight.Normal,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val subtitle = if (item.isDirectory) {
                        if (item.projectType != null) "📦 ${item.projectType} Projesi" else "${item.itemCount ?: 0} öğe"
                    } else {
                        formatBytes(item.size ?: 0)
                    }

                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = if (item.projectType != null) GeminiPurple else TextMuted
                    )
                }
            }

            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Seçenekler",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
    return String.format(java.util.Locale.US, "%.1f %s", value, units[digitGroups])
}
