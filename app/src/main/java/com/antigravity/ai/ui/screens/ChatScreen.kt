package com.antigravity.ai.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
            var fileName = "attachment"
            var fileSize: Long? = null
            context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex >= 0) fileName = cursor.getString(nameIndex)
                    if (sizeIndex >= 0) fileSize = cursor.getLong(sizeIndex)
                }
            }
            val mime = context.contentResolver.getType(it) ?: ""
            val type = if (mime.startsWith("image/")) "image" else "doc"
            viewModel.addAttachment(
                Attachment(
                    name = fileName,
                    path = it.toString(),
                    type = type,
                    size = fileSize
                )
            )
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

                    MessageInputBar(
                        text = uiState.inputText,
                        onTextChange = viewModel::onInputTextChange,
                        pastedBlocks = uiState.pastedBlocks,
                        onRemovePastedBlock = viewModel::removePastedBlock,
                        attachments = uiState.attachments,
                        onRemoveAttachment = viewModel::removeAttachment,
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
                    WelcomeView(
                        onSuggestionClick = { prompt ->
                            viewModel.onInputTextChange(prompt)
                            viewModel.sendMessage()
                        }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(uiState.messages, key = { it.id }) { msg ->
                            MessageItem(message = msg)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeView(onSuggestionClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "⚡",
            fontSize = 42.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Text(
            text = "Antigravity AI Asistanı",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Termux ve Android Linux üzerinde çalışan güçlü AI asistanınız hazır. Mobilde hızlıca komut çalıştırın, kod yazdırın veya görevlerinizi yönetin.",
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        val suggestions = listOf(
            "📁 Projeleri ve durumları listele" to "Termux ortamındaki mevcut projeleri ve durumları listele",
            "🌿 Git durumunu kontrol et" to "Git durumunu kontrol et ve özetle",
            "🔍 Kod analizi yap" to "Son yapılan değişiklikleri analiz et",
            "🐍 Python otomasyonu yaz" to "Bana pratik bir Python otomasyon scripti yaz"
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { (label, prompt) ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceVariantDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSuggestionClick(prompt) }
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}
