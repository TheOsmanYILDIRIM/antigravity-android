package com.antigravity.ai.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.ai.data.model.VaultItem
import com.antigravity.ai.ui.theme.*

enum class ObsidianVaultTab {
    FILES,       // File Tree / Explorer
    SEARCH,      // Quick Switcher & Full Search
    BOOKMARKS,   // Starred / Favorite Notes
    GRAPH        // Knowledge Graph / Backlinks summary
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultManagerScreen(
    vaultFiles: List<VaultItem>,
    activeFileContent: String?,
    activeFilePath: String?,
    onDismiss: () -> Unit,
    onLoadFileContent: (String) -> Unit,
    onSaveNote: (relPath: String?, title: String?, content: String) -> Unit,
    onCreateFolder: (folderPath: String) -> Unit,
    onDeleteFile: (relPath: String) -> Unit,
    onReferenceFile: (VaultItem) -> Unit,
    onReferenceParagraph: (fileName: String, paragraph: String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BackgroundDark,
        contentColor = TextPrimary,
        modifier = Modifier.fillMaxHeight(0.94f)
    ) {
        VaultManagerContent(
            vaultFiles = vaultFiles,
            activeFileContent = activeFileContent,
            activeFilePath = activeFilePath,
            onDismiss = onDismiss,
            onLoadFileContent = onLoadFileContent,
            onSaveNote = onSaveNote,
            onCreateFolder = onCreateFolder,
            onDeleteFile = onDeleteFile,
            onReferenceFile = onReferenceFile,
            onReferenceParagraph = onReferenceParagraph
        )
    }
}

@Composable
fun VaultManagerContent(
    vaultFiles: List<VaultItem>,
    activeFileContent: String?,
    activeFilePath: String?,
    onDismiss: () -> Unit,
    onLoadFileContent: (String) -> Unit,
    onSaveNote: (relPath: String?, title: String?, content: String) -> Unit,
    onCreateFolder: (folderPath: String) -> Unit,
    onDeleteFile: (relPath: String) -> Unit,
    onReferenceFile: (VaultItem) -> Unit,
    onReferenceParagraph: (fileName: String, paragraph: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(ObsidianVaultTab.FILES) }
    var isEditingNote by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf("") }
    var editContent by remember { mutableStateOf("") }
    var editRelPath by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var selectedTagFilter by remember { mutableStateOf<String?>(null) }
    val bookmarkedPaths = remember { mutableStateListOf<String>() }

    // When active file changes
    LaunchedEffect(activeFilePath, activeFileContent) {
        if (activeFilePath != null && activeFileContent != null) {
            editRelPath = activeFilePath
            editTitle = activeFilePath.substringAfterLast("/")
            editContent = activeFileContent
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
            .padding(bottom = 16.dp)
    ) {
        // 1. Obsidian Top Header (Title + Breadcrumbs + Actions)
        ObsidianHeader(
            activeFilePath = activeFilePath,
            isEditingNote = isEditingNote || activeFilePath != null,
            onBack = {
                isEditingNote = false
                editRelPath = null
                onLoadFileContent("")
            },
            onDismiss = onDismiss
        )

        Spacer(modifier = Modifier.height(8.dp))

        // If a note is currently opened (Reader / Editor View)
        if (activeFilePath != null || isEditingNote) {
            ObsidianNoteEditorView(
                title = editTitle,
                content = if (editContent.isNotBlank()) editContent else (activeFileContent ?: ""),
                relPath = editRelPath,
                isEditing = isEditingNote,
                onTitleChange = { editTitle = it },
                onContentChange = { editContent = it },
                onToggleEdit = { isEditingNote = !isEditingNote },
                onSave = {
                    onSaveNote(editRelPath, editTitle, editContent)
                    isEditingNote = false
                },
                onReferenceFile = {
                    activeFilePath?.let { path ->
                        onReferenceFile(VaultItem(name = path.substringAfterLast("/"), path = path))
                        onDismiss()
                    }
                },
                onReferenceParagraph = { paragraph ->
                    activeFilePath?.let { path ->
                        onReferenceParagraph(path.substringAfterLast("/"), paragraph)
                        onDismiss()
                    }
                }
            )
        } else {
            // Main Obsidian Vault Explorer with Ribbon Tabs
            Column(modifier = Modifier.fillMaxSize()) {
                // 2. Obsidian Ribbon Tab Bar
                ObsidianRibbonTabBar(
                    activeTab = activeTab,
                    onTabSelect = { activeTab = it }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Tab Contents
                when (activeTab) {
                    ObsidianVaultTab.FILES -> {
                        ObsidianFileExplorerView(
                            vaultFiles = vaultFiles,
                            onSelectFile = { item ->
                                if (!item.isDirectory) {
                                    onLoadFileContent(item.path)
                                }
                            },
                            onNewNote = {
                                editRelPath = null
                                editTitle = "Yeni-Not-${System.currentTimeMillis() % 10000}.md"
                                editContent = "---\ntags: [not]\ncreated: 2026-08-26\n---\n\n# Yeni Not\n\n"
                                isEditingNote = true
                            },
                            onNewFolder = { showCreateFolderDialog = true },
                            onReferenceFile = {
                                onReferenceFile(it)
                                onDismiss()
                            },
                            onDeleteFile = onDeleteFile,
                            onToggleBookmark = { path ->
                                if (bookmarkedPaths.contains(path)) bookmarkedPaths.remove(path)
                                else bookmarkedPaths.add(path)
                            },
                            bookmarkedPaths = bookmarkedPaths
                        )
                    }

                    ObsidianVaultTab.SEARCH -> {
                        ObsidianQuickSwitcherView(
                            vaultFiles = vaultFiles,
                            searchQuery = searchQuery,
                            onQueryChange = { searchQuery = it },
                            selectedTag = selectedTagFilter,
                            onTagSelect = { selectedTagFilter = if (selectedTagFilter == it) null else it },
                            onSelectFile = { item ->
                                onLoadFileContent(item.path)
                            }
                        )
                    }

                    ObsidianVaultTab.BOOKMARKS -> {
                        ObsidianBookmarksView(
                            vaultFiles = vaultFiles.filter { bookmarkedPaths.contains(it.path) },
                            onSelectFile = { onLoadFileContent(it.path) },
                            onRemoveBookmark = { bookmarkedPaths.remove(it.path) }
                        )
                    }

                    ObsidianVaultTab.GRAPH -> {
                        ObsidianGraphSummaryView(
                            vaultFiles = vaultFiles,
                            onSelectFile = { onLoadFileContent(it.path) }
                        )
                    }
                }
            }
        }
    }

    // New Folder Dialog
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Yeni Klasör Oluştur", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    placeholder = { Text("örn: 10-Mimari-Notlar", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryIndigo, unfocusedBorderColor = BorderSubtle)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            onCreateFolder(newFolderName.trim())
                            newFolderName = ""
                            showCreateFolderDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Text("Oluştur")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("İptal", color = TextMuted)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

// 1. Obsidian Top Header & Breadcrumbs
@Composable
fun ObsidianHeader(
    activeFilePath: String?,
    isEditingNote: Boolean,
    onBack: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (isEditingNote) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Geri", tint = TextPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
            }


            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.linearGradient(listOf(SecondaryPurple, PrimaryIndigo))),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isEditingNote) "Obsidian Not Görünümü" else "AGY Vault",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
                Text(
                    text = activeFilePath ?: "Vault / root",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Kapat", tint = TextMuted, modifier = Modifier.size(18.dp))
        }
    }
}

// 2. Obsidian Ribbon Tab Bar
@Composable
fun ObsidianRibbonTabBar(
    activeTab: ObsidianVaultTab,
    onTabSelect: (ObsidianVaultTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceVariantDark)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        RibbonTabItem(
            label = "Dosyalar",
            icon = Icons.Outlined.Folder,
            isSelected = activeTab == ObsidianVaultTab.FILES,
            onClick = { onTabSelect(ObsidianVaultTab.FILES) },
            modifier = Modifier.weight(1f)
        )
        RibbonTabItem(
            label = "Arama",
            icon = Icons.Outlined.Search,
            isSelected = activeTab == ObsidianVaultTab.SEARCH,
            onClick = { onTabSelect(ObsidianVaultTab.SEARCH) },
            modifier = Modifier.weight(1f)
        )
        RibbonTabItem(
            label = "Yıldızlı",
            icon = Icons.Outlined.BookmarkBorder,
            isSelected = activeTab == ObsidianVaultTab.BOOKMARKS,
            onClick = { onTabSelect(ObsidianVaultTab.BOOKMARKS) },
            modifier = Modifier.weight(1f)
        )
        RibbonTabItem(
            label = "Ağ Haritası",
            icon = Icons.Outlined.Hub,
            isSelected = activeTab == ObsidianVaultTab.GRAPH,
            onClick = { onTabSelect(ObsidianVaultTab.GRAPH) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun RibbonTabItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) SurfaceDark else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, BorderSubtle) else null,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) PrimaryIndigo else TextMuted,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) TextPrimary else TextMuted,
                maxLines = 1
            )
        }
    }
}

// 3. Tab 1: File Explorer View
@Composable
fun ObsidianFileExplorerView(
    vaultFiles: List<VaultItem>,
    onSelectFile: (VaultItem) -> Unit,
    onNewNote: () -> Unit,
    onNewFolder: () -> Unit,
    onReferenceFile: (VaultItem) -> Unit,
    onDeleteFile: (String) -> Unit,
    onToggleBookmark: (String) -> Unit,
    bookmarkedPaths: List<String>
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Action Bar (New Note / New Folder)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onNewNote,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f).height(38.dp)
            ) {
                Icon(imageVector = Icons.Default.NoteAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Yeni Not", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }


            Button(
                onClick = onNewFolder,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark),
                border = BorderStroke(1.dp, BorderSubtle),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f).height(38.dp)
            ) {
                Icon(imageVector = Icons.Default.CreateNewFolder, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Yeni Klasör", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (vaultFiles.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Outlined.FolderOpen, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Vault dizininde henüz not bulunmuyor.", fontSize = 12.sp, color = TextMuted)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(vaultFiles) { item ->
                    val isBookmarked = bookmarkedPaths.contains(item.path)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceDark,
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectFile(item) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (item.isDirectory) Icons.Filled.Folder else Icons.Outlined.Description,
                                contentDescription = null,
                                tint = if (item.isDirectory) WarningAmber else PrimaryIndigo,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.path,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (!item.isDirectory) {
                                // Star / Bookmark
                                IconButton(
                                    onClick = { onToggleBookmark(item.path) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                        contentDescription = "Yıldızla",
                                        tint = if (isBookmarked) WarningAmber else TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Quick Reference
                                IconButton(
                                    onClick = { onReferenceFile(item) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AddLink,
                                        contentDescription = "Referans Ver",
                                        tint = PrimaryIndigo,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Delete
                            IconButton(
                                onClick = { onDeleteFile(item.path) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Sil",
                                    tint = TextMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 4. Tab 2: Quick Switcher & Search
@Composable
fun ObsidianQuickSwitcherView(
    vaultFiles: List<VaultItem>,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    selectedTag: String?,
    onTagSelect: (String) -> Unit,
    onSelectFile: (VaultItem) -> Unit
) {
    val commonTags = listOf("kod", "mimari", "gemini", "termux", "android", "proje", "fikir")

    Column(modifier = Modifier.fillMaxSize()) {
        // Search TextField
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            placeholder = { Text("Quick Switcher (Dosya veya not ara…)", color = TextMuted, fontSize = 12.sp) },
            leadingIcon = { Icon(imageVector = Icons.Outlined.Search, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Temizle", tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryIndigo,
                unfocusedBorderColor = BorderSubtle,
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Tag Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            commonTags.forEach { tag ->
                val isSelected = selectedTag == tag
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) PrimaryIndigo else SurfaceVariantDark,
                    border = BorderStroke(1.dp, if (isSelected) PrimaryIndigo else BorderSubtle),
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onTagSelect(tag) }
                ) {
                    Text(
                        text = "#$tag",
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else TextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        val filtered = vaultFiles.filter {
            !it.isDirectory &&
            (searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.path.contains(searchQuery, ignoreCase = true))
        }

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(text = "Eşleşen not bulunamadı.", fontSize = 12.sp, color = TextMuted)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filtered) { item ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceDark,
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectFile(item) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                Text(text = item.path, fontSize = 10.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 5. Tab 3: Starred / Bookmarks View
@Composable
fun ObsidianBookmarksView(
    vaultFiles: List<VaultItem>,
    onSelectFile: (VaultItem) -> Unit,
    onRemoveBookmark: (VaultItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "YILDIZLI & ÖNEMLİ NOTLAR",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            modifier = Modifier.padding(vertical = 6.dp)
        )

        if (vaultFiles.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Outlined.BookmarkBorder, contentDescription = null, tint = TextMuted, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Henüz yıldızlanmış not yok. Dosya listesinden yıldızlayabilirsiniz.", fontSize = 11.sp, color = TextMuted)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(vaultFiles) { item ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceDark,
                        border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectFile(item) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Star, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = item.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onRemoveBookmark(item) }, modifier = Modifier.size(26.dp)) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Kaldır", tint = TextMuted, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// 6. Tab 4: Graph & Knowledge Summary View
@Composable
fun ObsidianGraphSummaryView(
    vaultFiles: List<VaultItem>,
    onSelectFile: (VaultItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Outlined.Hub, contentDescription = null, tint = SecondaryPurple, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Obsidian Bilgi Grafiği & İstatistikler", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(shape = RoundedCornerShape(8.dp), color = SurfaceVariantDark, modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(text = "Toplam Not", fontSize = 10.sp, color = TextMuted)
                            Text(text = "${vaultFiles.filter { !it.isDirectory }.size}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryIndigo)
                        }
                    }
                    Surface(shape = RoundedCornerShape(8.dp), color = SurfaceVariantDark, modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(text = "Klasörler", fontSize = 10.sp, color = TextMuted)
                            Text(text = "${vaultFiles.filter { it.isDirectory }.size}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = WarningAmber)
                        }
                    }
                    Surface(shape = RoundedCornerShape(8.dp), color = SurfaceVariantDark, modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(text = "WikiLinks", fontSize = 10.sp, color = TextMuted)
                            Text(text = "14", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SuccessGreen)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(text = "SON GÜNCELLENEN NOTLAR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
        Spacer(modifier = Modifier.height(6.dp))

        vaultFiles.take(5).forEach { item ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SurfaceVariantDark,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clickable { onSelectFile(item) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.Link, contentDescription = null, tint = GeminiPink, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = item.name, fontSize = 12.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                    Text(text = "[[WikiLink]]", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = PrimaryIndigo)
                }
            }
        }
    }
}

// 7. Obsidian Note Reader & Editor
@Composable
fun ObsidianNoteEditorView(
    title: String,
    content: String,
    relPath: String?,
    isEditing: Boolean,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onToggleEdit: () -> Unit,
    onSave: () -> Unit,
    onReferenceFile: () -> Unit,
    onReferenceParagraph: (String) -> Unit
) {
    val wordCount = if (content.isBlank()) 0 else content.split(Regex("\\s+")).size
    val charCount = content.length
    val readingTimeMin = maxOf(1, wordCount / 180)

    Column(modifier = Modifier.fillMaxSize()) {
        // Toolbar (Edit Toggle, Save, All Note Reference)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Refer All Button
                Button(
                    onClick = onReferenceFile,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark),
                    border = BorderStroke(1.dp, BorderSubtle),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.AddLink, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Chat'e Referans", fontSize = 11.sp, color = TextPrimary)
                }

                // Edit / View Toggle
                IconButton(
                    onClick = onToggleEdit,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isEditing) PrimaryIndigo else SurfaceVariantDark)
                ) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.Visibility else Icons.Default.Edit,
                        contentDescription = null,
                        tint = if (isEditing) Color.White else TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            if (isEditing) {
                Button(
                    onClick = onSave,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Kaydet", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isEditing) {
            // Markdown Editing Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MarkdownToolChip(label = "H1", onClick = { onContentChange("$content\n# ") })
                MarkdownToolChip(label = "H2", onClick = { onContentChange("$content\n## ") })
                MarkdownToolChip(label = "B", onClick = { onContentChange("$content **metin** ") })
                MarkdownToolChip(label = "I", onClick = { onContentChange("$content *italik* ") })
                MarkdownToolChip(label = "[[Link]]", onClick = { onContentChange("$content [[NotAdı]] ") })
                MarkdownToolChip(label = "#tag", onClick = { onContentChange("$content #etiket ") })
                MarkdownToolChip(label = "[ ] Liste", onClick = { onContentChange("$content\n- [ ] ") })
                MarkdownToolChip(label = "Kod", onClick = { onContentChange("$content\n```bash\n\n```") })
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Editor Field
            OutlinedTextField(
                value = content,
                onValueChange = onContentChange,
                placeholder = { Text("Obsidian Markdown notunuzu yazın…", color = TextMuted, fontSize = 13.sp) },
                textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace, lineHeight = 19.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryIndigo,
                    unfocusedBorderColor = BorderSubtle,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else {
            // Live Markdown Reader with Interactive Paragraphs
            val paragraphs = content.split("\n\n").filter { it.isNotBlank() }

            if (paragraphs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text(text = "Bu not henüz boş.", color = TextMuted, fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(paragraphs) { paragraph ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SurfaceDark,
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = paragraph.trim(),
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = SurfaceVariantDark,
                                        border = BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.6f)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { onReferenceParagraph(paragraph.trim()) }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.FormatQuote, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(text = "Bu Paragrafı Referans Ver", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Obsidian Telemetry Footer (Word count, Char count, Reading time)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(SurfaceVariantDark)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(text = "$wordCount kelime • $charCount karakter", fontSize = 10.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
            Text(text = "~$readingTimeMin dk okuma", fontSize = 10.sp, color = TextSecondary)
        }
    }
}

@Composable
fun MarkdownToolChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryIndigo,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
