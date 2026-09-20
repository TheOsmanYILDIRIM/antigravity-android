package com.antigravity.ai.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.antigravity.ai.data.api.CodexServerManager
import com.antigravity.ai.ui.viewmodel.ChatViewModel
import com.antigravity.ai.ui.viewmodel.ChatViewModelFactory

/**
 * Bağımsız AGY, Codex, OpenCode ve Cline sohbet istemcilerini canlı tutarak
 * hamburger menü altındaki mini yüzen geçiş balonları üzerinden yönetir.
 */
@Composable
fun ChatWorkspace(onExitApp: (() -> Unit)? = null) {
    var selectedBackend by rememberSaveable { mutableStateOf("agy") }
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application

    val agyViewModel: ChatViewModel = viewModel(
        key = "chat-agy",
        factory = ChatViewModelFactory(application, "agy")
    )
    val codexViewModel: ChatViewModel = viewModel(
        key = "chat-codex",
        factory = ChatViewModelFactory(application, "codex")
    )
    val opencodeViewModel: ChatViewModel = viewModel(
        key = "chat-opencode",
        factory = ChatViewModelFactory(application, "opencode")
    )
    val clineViewModel: ChatViewModel = viewModel(
        key = "chat-cline",
        factory = ChatViewModelFactory(application, "cline")
    )

    LaunchedEffect(selectedBackend) {
        if (selectedBackend == "codex") {
            CodexServerManager.ensureStarted(context)
        }
    }

    Box(Modifier.fillMaxSize()) {
        val activeViewModel = when (selectedBackend) {
            "codex" -> codexViewModel
            "opencode" -> opencodeViewModel
            "cline" -> clineViewModel
            else -> agyViewModel
        }

        ChatScreen(
            viewModel = activeViewModel,
            forcedBackend = selectedBackend,
            selectedBackend = selectedBackend,
            onBackendSelected = { selectedBackend = it },
            onExitApp = onExitApp,
            modifier = Modifier.fillMaxSize()
        )
    }
}
