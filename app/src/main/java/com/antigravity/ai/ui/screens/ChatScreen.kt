package com.antigravity.ai.ui.screens

import android.Manifest
import android.content.pm.PackageManager
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

    // Auto-scroll on new messages
    LaunchedEffect(uiState.messages.size, uiState.messages.lastOrNull()?.content) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChatDrawer(
                conversations = uiState.conversations,
                currentConversationId = uiState.currentConversationId,
                onSelectConversation = {
                    scope.launch { drawerState.close() }
                },
                onNewChat = {
                    viewModel.startNewChat()
                    scope.launch { drawerState.close() }
                },
                onDeleteConversation = {
                    // delete
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                ChatTopBar(
                    isGenerating = uiState.isGenerating,
                    onMenuClick = {
                        scope.launch { drawerState.open() }
                    },
                    onNewChatClick = {
                        viewModel.startNewChat()
                    }
                )
            },
            bottomBar = {
                MessageInputBar(
                    text = uiState.inputText,
                    onTextChange = viewModel::onInputTextChange,
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
                    }
                )
            },
            containerColor = BackgroundDark
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (uiState.messages.isEmpty()) {
                    // Welcome / Empty State
                    WelcomeView(
                        onSuggestionClick = { prompt ->
                            viewModel.onInputTextChange(prompt)
                            viewModel.sendMessage()
                        }
                    )
                } else {
                    // Messages list
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

        // Suggestion Chips
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
