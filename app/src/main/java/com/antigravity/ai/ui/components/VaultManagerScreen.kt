package com.antigravity.ai.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.ai.data.model.VaultItem
import com.antigravity.ai.ui.theme.*

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
    var isEditingNote by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf("") }
    var editContent by remember { mutableStateOf("") }
    var editRelPath by remember { mutableStateOf<String?>(null) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    // When active file changes
    LaunchedEffect(activeFilePath, activeFileContent) {
        if (activeFilePath != null && activeFileContent != null) {
            editRelPath = activeFilePath
            editTitle = activeFilePath.substringAfterLast("/")
            editContent = activeFileContent
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        contentColor = TextPrimary,
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(listOf(WarningAmber, PrimaryIndigo))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Storage, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isEditingNote || activeFilePath != null) "Not Okuyucu & Düzenleyici" else "AGY Vault Kütüphanesi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = if (activeFilePath != null) activeFilePath!! else "/storage/emulated/0/Documents/AGY-Vault",
                            fontSize = 11.sp,
                            color = TextMuted,
                            maxLines = 1
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (activeFilePath != null || isEditingNote) {
                        IconButton(onClick = {
                            isEditingNote = false
                            editRelPath = null
                            onLoadFileContent("")
                        }) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Geri", tint = TextPrimary)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Kapat", tint = TextMuted)
                    }
                }
            }

            Divider(color = BorderSubtle, modifier = Modifier.padding(vertical = 10.dp))

            // VIEW 1: Note Reader & Paragraph Referencer / Editor
            if (activeFilePath != null || isEditingNote) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    // Actions Bar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Reference whole note button
                            Button(
                                onClick = {
                                    activeFilePath?.let { path ->
                                        onReferenceFile(VaultItem(name = path.substringAfterLast("/"), path = path))
                                        onDismiss()
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.AddLink, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Tüm Notu Referans Ver", fontSize = 11.sp, color = TextPrimary)
                            }

                            // Edit / View toggle
                            IconButton(
                                onClick = { isEditingNote = !isEditingNote },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isEditingNote) PrimaryIndigo else SurfaceVariantDark)
                            ) {
                                Icon(
                                    imageVector = if (isEditingNote) Icons.Default.Visibility else Icons.Default.Edit,
                                    contentDescription = "Düzenle",
                                    tint = if (isEditingNote) Color.White else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (isEditingNote) {
                            Button(
                                onClick = {
                                    onSaveNote(editRelPath, editTitle, editContent)
                                    isEditingNote = false
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Kaydet", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }

                    if (isEditingNote) {
                        // Markdown Editor
                        OutlinedTextField(
                            value = editContent,
                            onValueChange = { editContent = it },
                            placeholder = { Text("Markdown notunuzu yazın… (# Başlık, - Madde)", color = TextMuted, fontSize = 13.sp) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryIndigo,
                                unfocusedBorderColor = BorderSubtle,
                                focusedContainerColor = Color(0xFF090E1A),
                                unfocusedContainerColor = Color(0xFF090E1A)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    } else {
                        // Note Reader with Paragraph Referencer
                        val paragraphs = (activeFileContent ?: editContent).split("\n\n").filter { it.isNotBlank() }

                        if (paragraphs.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(text = "Bu not henüz boş.", color = TextMuted, fontSize = 13.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(paragraphs) { paragraph ->
                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = paragraph.trim(),
                                                fontSize = 13.sp,
                                                lineHeight = 19.sp,
                                                color = TextPrimary
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.End,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFF1E293B),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryIndigo),
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .clickable {
                                                            activeFilePath?.let { path ->
                                                                onReferenceParagraph(path.substringAfterLast("/"), paragraph.trim())
                                                                onDismiss()
                                                            }
                                                        }
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Icon(imageVector = Icons.Default.FormatQuote, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(13.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
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
                }
            } else {
                // VIEW 2: Vault File Explorer & Library
                Column(modifier = Modifier.fillMaxSize()) {
                    // Quick Action Buttons (New Note, New Folder)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                editRelPath = null
                                editTitle = "Yeni-Not-${System.currentTimeMillis() % 10000}.md"
                                editContent = "# Yeni Not\n\n"
                                isEditingNote = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                        ) {
                            Icon(imageVector = Icons.Default.NoteAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Yeni Not", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showCreateFolderDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CreateNewFolder, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Yeni Klasör", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }

                    // Files & Folders List
                    if (vaultFiles.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(text = "Vault dizininde henüz dosya yok.", fontSize = 12.sp, color = TextMuted)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(vaultFiles) { item ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SurfaceVariantDark)
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                                        contentDescription = null,
                                        tint = if (item.isDirectory) WarningAmber else PrimaryIndigo,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                if (!item.isDirectory) {
                                                    onLoadFileContent(item.path)
                                                }
                                            }
                                    ) {
                                        Text(text = item.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                                        Text(text = item.path, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextMuted, maxLines = 1)
                                    }

                                    // Action buttons per item
                                    if (!item.isDirectory) {
                                        // Quick Reference Button
                                        IconButton(
                                            onClick = {
                                                onReferenceFile(item)
                                                onDismiss()
                                            },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.AddLink, contentDescription = "Referans Ver", tint = PrimaryIndigo, modifier = Modifier.size(17.dp))
                                        }

                                        // Open / Read Button
                                        IconButton(
                                            onClick = { onLoadFileContent(item.path) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Visibility, contentDescription = "Oku", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    // Delete Button
                                    IconButton(
                                        onClick = { onDeleteFile(item.path) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Sil", tint = TextMuted, modifier = Modifier.size(15.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Folder Dialog
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Yeni Vault Klasörü", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    placeholder = { Text("Klasör adı (örn: 50-Notlar)", color = TextMuted) },
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
