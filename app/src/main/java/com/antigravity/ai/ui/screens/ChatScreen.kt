package com.antigravity.ai.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.antigravity.ai.data.model.Attachment
import com.antigravity.ai.ui.components.*
import com.antigravity.ai.ui.theme.*
import com.antigravity.ai.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val showScrollToBottom by remember {
        derivedStateOf {
            val totalItems = uiState.messages.size
            if (totalItems <= 1) false
            else {
                val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisibleIndex < totalItems - 1
            }
        }
    }

    // Audio record permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening()
        }
    }

    // Notification permission launcher (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* izin verilirse bildirimler çalışır; reddedilirse NotificationHelper zaten korur */ }

    // Overlay permission launcher for Floating Keep-Alive (SYSTEM_ALERT_WINDOW)
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (com.antigravity.ai.service.FloatingKeepAliveService.canDrawOverlays(context)) {
            viewModel.toggleKeepAlive(true, uiState.keepAliveMode)
        }
    }

    // Document / Image picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            var fileName = "attachment_${System.currentTimeMillis()}"
            context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex)
                }
            }
            val mime = context.contentResolver.getType(it) ?: "application/octet-stream"
            viewModel.uploadAndAttachFile(fileName, it, mime)
        }
    }

    // Auto-scroll on new messages
    LaunchedEffect(uiState.messages.size, uiState.messages.lastOrNull()?.content) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Model & Settings Bottom Sheet
    if (uiState.showSettingsDialog) {
        ModelSettingsDialog(
            currentSettings = uiState.settings,
            availableModels = uiState.availableModels,
            availableEfforts = uiState.availableEfforts,
            onDismiss = { viewModel.setSettingsDialogVisible(false) },
            onSave = { newSettings -> viewModel.updateSettings(newSettings) },
            onOpenAuthDialog = {
                viewModel.setSettingsDialogVisible(false)
                viewModel.setAuthDialogVisible(true)
            },
            currentBackend = uiState.activeBackend,
            onBackendChange = { viewModel.setBackend(it) },
            onNotificationToggle = { enabled ->
                if (enabled && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            serverHealth = uiState.serverHealth,
            isCheckingHealth = uiState.isCheckingHealth,
            onCheckHealth = { viewModel.checkServerHealth() },
            onStartServer = { viewModel.startAgyServer() },
            onStopServer = { viewModel.stopAgyServer() },
            onRestartServer = { viewModel.restartAgyServer() },
            isKeepAliveRunning = uiState.isKeepAliveRunning,
            keepAliveMode = uiState.keepAliveMode,
            onToggleKeepAlive = { enabled, mode ->
                if (enabled && !com.antigravity.ai.service.FloatingKeepAliveService.canDrawOverlays(context)) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        val intent = android.content.Intent(
                            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            android.net.Uri.parse("package:${context.packageName}")
                        )
                        overlayPermissionLauncher.launch(intent)
                    }
                } else {
                    viewModel.toggleKeepAlive(enabled, mode)
                }
            }
        )
    }

    // opencode izin onayı dialogu
    if (uiState.pendingPermission != null) {
        val perm = uiState.pendingPermission!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissPermission() },
            title = { Text("İzin Gerekli (${perm.action})") },
            text = {
                Column {
                    Text("OpenCode bu işlemi çalıştırmak istiyor:")
                    Spacer(Modifier.height(6.dp))
                    perm.resources.forEach { Text("• $it", fontSize = 12.sp) }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.replyPermission(allow = true, always = false) }) { Text("İzin Ver") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { viewModel.replyPermission(allow = true, always = true) }) { Text("Her Zaman") }
                    TextButton(onClick = { viewModel.replyPermission(allow = false, always = false) }) { Text("Reddet") }
                }
            }
        )
    }

    // opencode kullanıcı sorusu dialogu
    if (uiState.pendingQuestion != null) {
        val q = uiState.pendingQuestion!!
        var answer by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.dismissQuestion() },
            title = { Text("OpenCode Sorusu") },
            text = {
                Column {
                    q.questions.forEach { Text("• $it", fontSize = 13.sp) }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = answer,
                        onValueChange = { answer = it },
                        label = { Text("Yanıt") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.replyQuestion(listOf(answer)) }) { Text("Gönder") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissQuestion() }) { Text("İptal") }
            }
        )
    }

    // Auth & Token Input Sheet
    if (uiState.showAuthDialog) {
        AuthTokenDialog(
            isAuthenticated = uiState.isAuthenticated,
            onDismiss = { viewModel.setAuthDialogVisible(false) }
        )
    }

    // Usage & Quota Detail Dialog
    if (uiState.showUsageDetail) {
        UsageDetailDialog(
            usage = uiState.usage,
            onDismiss = { viewModel.setUsageDetailVisible(false) }
        )
    }

    // Obsidian-Style AGY Vault Library & Note Editor Screen
    if (uiState.showVaultManager) {
        VaultManagerScreen(
            vaultFiles = uiState.vaultFiles,
            activeFileContent = uiState.activeVaultFileContent,
            activeFilePath = uiState.activeVaultFilePath,
            onDismiss = { viewModel.setVaultManagerVisible(false) },
            onLoadFileContent = { path -> viewModel.loadVaultFileContent(path) },
            onSaveNote = { relPath, title, content -> viewModel.saveVaultNote(relPath, title, content) },
            onCreateFolder = { folderPath -> viewModel.createVaultFolder(folderPath) },
            onDeleteFile = { path -> viewModel.deleteVaultFile(path) },
            onReferenceFile = { item -> viewModel.referenceFile(item) },
            onReferenceParagraph = { fileName, paragraph -> viewModel.referenceParagraph(fileName, paragraph) }
        )
    }

    // Termux File Manager & Project Explorer Screen
    if (uiState.showFileManager) {
        TermuxFileManagerScreen(
            currentDir = uiState.fsCurrentDir,
            parentDir = uiState.fsParentDir,
            homeDir = uiState.fsHomeDir,
            items = uiState.fsItems,
            projects = uiState.fsProjects,
            isLoading = uiState.isFsLoading,
            onDismiss = { viewModel.setFileManagerVisible(false) },
            onNavigateToDir = { dir -> viewModel.loadFsDirectory(dir) },
            onOpenFile = { path -> viewModel.openFileInViewer(path) },
            onOpenImage = { url, title -> viewModel.openImageInViewer(url, title) },
            onAttachToChat = { path -> viewModel.attachFsPathToChat(path) },
            onMentionInChat = { path -> viewModel.mentionFsPathInChat(path) },
            onRefresh = {
                viewModel.loadFsDirectory(uiState.fsCurrentDir)
                viewModel.loadFsProjects()
            },
            serverHealth = uiState.serverHealth,
            onStartServer = { viewModel.startAgyServer() }
        )
    }

    // In-App File Viewer & Editor Dialog
    if (uiState.activeViewerFilePath != null) {
        FileViewerDialog(
            filePath = uiState.activeViewerFilePath!!,
            contentResponse = uiState.activeViewerFileContent,
            isLoading = uiState.isViewerLoading,
            onDismiss = { viewModel.closeFileViewer() },
            onSaveFile = { path, content -> viewModel.saveFsFileContent(path, content) },
            onAttachToChat = { path -> viewModel.attachFsPathToChat(path) },
            onMentionInChat = { path -> viewModel.mentionFsPathInChat(path) }
        )
    }

    // Fullscreen In-App Image Viewer Dialog
    if (uiState.activeImageViewerUrl != null) {
        ImageViewerDialog(
            imageUrl = uiState.activeImageViewerUrl!!,
            title = uiState.activeImageViewerTitle,
            onDismiss = { viewModel.closeImageViewer() }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChatDrawer(
                conversations = uiState.conversations,
                currentSessionId = uiState.currentSessionId,
                pinnedIds = uiState.pinnedConversationIds,
                onSelectConversation = { id ->
                    viewModel.selectConversation(id)
                    scope.launch { drawerState.close() }
                },
                onNewChat = {
                    viewModel.startNewChat()
                    scope.launch { drawerState.close() }
                },
                onDeleteConversation = { id ->
                    viewModel.deleteConversation(id)
                },
                onTogglePinConversation = { id ->
                    viewModel.togglePinConversation(id)
                },
                onExportSingleConversation = { id ->
                    viewModel.exportSingleConversation(context, id)
                },
                onExportAllConversations = {
                    viewModel.exportAllConversations(context)
                },
                onOpenVault = {
                    scope.launch { drawerState.close() }
                    viewModel.setVaultManagerVisible(true)
                },
                onOpenFileManager = {
                    scope.launch { drawerState.close() }
                    viewModel.setFileManagerVisible(true)
                },
                onOpenSettings = {
                    scope.launch { drawerState.close() }
                    viewModel.setSettingsDialogVisible(true)
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                ChatTopBar(
                    settings = uiState.settings,
                    usage = uiState.usage,
                    isGenerating = uiState.isGenerating,
                    onMenuClick = {
                        scope.launch { drawerState.open() }
                    },
                    onNewChatClick = {
                        viewModel.startNewChat()
                    },
                    onSettingsClick = {
                        viewModel.setSettingsDialogVisible(true)
                    },
                    onUsageClick = {
                        viewModel.setUsageDetailVisible(true)
                    },
                    onFileManagerClick = {
                        viewModel.setFileManagerVisible(true)
                    }
                )
            },
            bottomBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Üretim başarısız olduysa / sunucu durduysa net hata banner'ı (TUI benzeri geri bildirim)
                    if (uiState.errorMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚠️ ${uiState.errorMessage}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = viewModel::clearErrorMessage) {
                                    Icon(Icons.Default.Close, contentDescription = "Kapat")
                                }
                            }
                        }
                    }

                    // Agy stderr / sistem bildirimleri (izin reddi, bekleme vb.) — sessiz donmayı önler
                    if (uiState.notice != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ℹ️ ${uiState.notice}",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = viewModel::clearNotice) {
                                    Icon(Icons.Default.Close, contentDescription = "Kapat")
                                }
                            }
                        }
                    }

                    // Slash command & Dynamic Skills autocomplete popup
                    if (uiState.showSlashCommands) {
                        SlashCommandPopup(
                            query = uiState.slashQuery,
                            installedSkills = uiState.installedSkills,
                            onSelect = { cmd -> viewModel.onSelectSlashCommand(cmd) }
                        )
                    }

                    // Mention autocomplete popup
                    if (uiState.showMentions) {
                        MentionPopup(
                            query = uiState.mentionQuery,
                            items = uiState.vaultFiles,
                            onSelect = { item -> viewModel.onSelectMention(item) }
                        )
                    }

                    val selectedModelName = uiState.availableModels.find { it.id == uiState.settings.model }?.name
                        ?: uiState.settings.model

                    MessageInputBar(
                        text = uiState.inputText,
                        onTextChange = viewModel::onInputTextChange,
                        pastedBlocks = uiState.pastedBlocks,
                        onRemovePastedBlock = viewModel::removePastedBlock,
                        attachments = uiState.attachments,
                        onRemoveAttachment = viewModel::removeAttachment,
                        selectedModelName = selectedModelName,
                        onModelPillClick = { viewModel.setSettingsDialogVisible(true) },
                        isGenerating = uiState.isGenerating,
                        isListening = uiState.isListening,
                        onSend = viewModel::sendMessage,
                        onStop = viewModel::stopExecution,
                        onMicClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasPermission) {
                                if (uiState.isListening) viewModel.stopListening() else viewModel.startListening()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onAttachClick = {
                            filePickerLauncher.launch("*/*")
                        },
                        onOpenFileManager = {
                            viewModel.setFileManagerVisible(true)
                        }
                    )
                }
            },
            containerColor = BackgroundDark,
            contentWindowInsets = WindowInsets.safeDrawing
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (uiState.messages.isEmpty()) {
                    FigmaGeminiHomeView(
                        onSuggestionClick = { prompt ->
                            viewModel.onInputTextChange(prompt)
                            viewModel.sendMessage()
                        },
                        onOpenVault = { viewModel.setVaultManagerVisible(true) }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp)
                    ) {
                        items(uiState.messages, key = { it.id }) { msg ->
                            val isLastBot = msg == uiState.messages.lastOrNull { it.role == "bot" }
                            MessageItem(
                                message = msg,
                                isLastBotMessage = isLastBot,
                                fontSizeSp = uiState.settings.fontSizeSp,
                                onOpenFile = { path -> viewModel.openFileInViewer(path) },
                                onOpenImage = { url, title -> viewModel.openImageInViewer(url, title) }
                            )
                        }
                    }
                }

                // Floating Scroll To Bottom Button (Gemini App / Figma Style)
                AnimatedVisibility(
                    visible = showScrollToBottom,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 12.dp, end = 16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = SurfaceVariantDark,
                        shadowElevation = 6.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .clickable {
                                scope.launch {
                                    listState.animateScrollToItem(uiState.messages.size - 1)
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "En alta kaydır",
                                tint = TextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Figma Gemini Home View ("Hello, Osman" & Action Cards)
@Composable
fun FigmaGeminiHomeView(
    onSuggestionClick: (String) -> Unit,
    onOpenVault: () -> Unit
) {
    var showPromo by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1.2f))

        // Figma Centered Greeting: "Hello, Osman"
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GeminiSparkleIcon(size = 44.dp)
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Hello, Osman",
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Antigravity AI is ready for coding & workflows",
                fontSize = 14.sp,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.weight(1.5f))

        // Bottom Action Block (Suggestion Cards + Promo Banner)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Figma Horizontal Scroll Suggestion Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SuggestionCard(
                    title = "Help me",
                    subtitle = "write code",
                    onClick = { onSuggestionClick("Python ile temiz bir otomasyon betiği yaz") }
                )
                SuggestionCard(
                    title = "Explore",
                    subtitle = "AGY Vault",
                    onClick = { onOpenVault() }
                )
                SuggestionCard(
                    title = "Analyze",
                    subtitle = "files & photos",
                    onClick = { onSuggestionClick("Termux ortamındaki çalışma alanını ve aktif dosyaları analiz et") }
                )
                SuggestionCard(
                    title = "Plan",
                    subtitle = "architecture",
                    onClick = { onSuggestionClick("Geliştireceğimiz yeni mobil özellik için adım adım bir mimari plan hazırla") }
                )
            }

            // Figma Feature Banner ("Vision & Vault Ready ✨")
            if (showPromo) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceVariantDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = GeminiPink,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Vision & Vault Ready ✨",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = TextPrimary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onOpenVault() }
                            ) {
                                Text(
                                    text = "Try Now",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BackgroundDark,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { showPromo = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Kapat",
                                    tint = TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable

fun SuggestionCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceVariantDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier
            .width(130.dp)
            .height(78.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextMuted
            )
        }
    }
}
