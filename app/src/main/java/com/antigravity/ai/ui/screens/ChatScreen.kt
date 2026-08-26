package com.antigravity.ai.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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

    // Audio record permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening()
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
            onSave = { newSettings -> viewModel.updateSettings(newSettings) }
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChatDrawer(
                conversations = uiState.conversations,
                currentSessionId = uiState.currentSessionId,
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
                onOpenVault = {
                    scope.launch { drawerState.close() }
                    viewModel.setVaultManagerVisible(true)
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
                    }
                )
            },
            bottomBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
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
                            MessageItem(message = msg, isLastBotMessage = isLastBot)
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
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // Figma Centered Greeting: "Hello, Osman"
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GeminiSparkleIcon(size = 40.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Hello, Osman",
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Antigravity AI is ready for coding & workflows",
                fontSize = 14.sp,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.weight(1f))

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

        Spacer(modifier = Modifier.height(14.dp))

        // Figma Feature Banner ("New! Edit images 🍌")
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

        Spacer(modifier = Modifier.height(12.dp))
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
