package com.antigravity.ai.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.LeadingIconTab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.antigravity.ai.data.api.CodexServerManager
import com.antigravity.ai.ui.viewmodel.ChatViewModel
import com.antigravity.ai.ui.viewmodel.ChatViewModelFactory

/** Keeps independent AGY and Codex chat clients alive while switching tabs. */
@Composable
fun ChatWorkspace(onExitApp: (() -> Unit)? = null) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
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

    LaunchedEffect(Unit) {
        CodexServerManager.ensureStarted(context)
    }

    Column(Modifier.fillMaxSize()) {
        WorkspaceTabRow(selectedTab = selectedTab, onTabSelected = { selectedTab = it })

        when (selectedTab) {
            0 -> ChatScreen(
                viewModel = agyViewModel,
                forcedBackend = "agy",
                onExitApp = onExitApp,
                modifier = Modifier.weight(1f)
            )
            else -> ChatScreen(
                viewModel = codexViewModel,
                forcedBackend = "codex",
                onExitApp = onExitApp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun WorkspaceTabRow(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    TabRow(selectedTabIndex = selectedTab) {
        LeadingIconTab(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            icon = { Icon(Icons.Outlined.SmartToy, contentDescription = null) },
            text = { Text("AGY") }
        )
        LeadingIconTab(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            icon = { Icon(Icons.Outlined.Code, contentDescription = null) },
            text = { Text("Codex") }
        )
    }
}
