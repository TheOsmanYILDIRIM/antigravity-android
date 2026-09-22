package com.antigravity.ai.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.keyframes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import android.provider.Settings
import androidx.lifecycle.viewmodel.compose.viewModel
import com.antigravity.ai.data.api.CodexServerManager
import com.antigravity.ai.ui.viewmodel.ChatViewModel
import com.antigravity.ai.ui.viewmodel.ChatViewModelFactory
import com.antigravity.ai.ui.components.PreviewScreen

internal fun shouldOpenHub(progress: Float): Boolean = progress >= 0.35f

private val terminalHubDragDistance = 96.dp
private val terminalHubEdgeInset = 16.dp

/**
 * Bağımsız AGY, Codex, OpenCode ve Cline sohbet istemcilerini canlı tutarak
 * hamburger menü altındaki mini yüzen geçiş balonları üzerinden yönetir.
 */
@Composable
fun ChatWorkspace(onExitApp: (() -> Unit)? = null) {
    var selectedBackend by rememberSaveable { mutableStateOf("agy") }
    var previewUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var previewSourcePath by rememberSaveable { mutableStateOf<String?>(null) }
    var showHub by rememberSaveable { mutableStateOf(false) }
    var hubDragProgress by rememberSaveable { mutableStateOf(0f) }
    val context = LocalContext.current
    val animationsEnabled = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
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
            val ready = CodexServerManager.ensureStarted(context)
            // serverHealth is the AGY status shown by shared server UI.
            codexViewModel.checkServerHealth()
            if (ready) {
                codexViewModel.refreshAll()
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .then(
                if (previewUrl == null) {
                    Modifier.pointerInput(showHub) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var previousX = down.position.x
                            var acceptedDirection = false
                            var rejectedDirection = false
                            hubDragProgress = 0f
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (change.changedToUp()) {
                                    if (acceptedDirection && shouldOpenHub(hubDragProgress)) {
                                        showHub = !showHub
                                    }
                                    hubDragProgress = 0f
                                    break
                                }

                                val amount = change.position.x - previousX
                                previousX = change.position.x
                                if (rejectedDirection) continue
                                if (!acceptedDirection && kotlin.math.abs(amount) > viewConfiguration.touchSlop) {
                                    val validDirection = (!showHub && amount < 0f) || (showHub && amount > 0f)
                                    if (!validDirection) {
                                        rejectedDirection = true
                                        hubDragProgress = 0f
                                        continue
                                    }
                                    acceptedDirection = true
                                }
                                if (acceptedDirection) {
                                    hubDragProgress = (hubDragProgress + kotlin.math.abs(amount) / terminalHubDragDistance.toPx())
                                        .coerceIn(0f, 1f)
                                    change.consume()
                                }
                            }
                        }
                    }
                } else {
                    Modifier
                }
            )
    ) {
        AnimatedVisibility(showHub, enter = slideInHorizontally { it } + fadeIn(), exit = slideOutHorizontally { it } + fadeOut()) {
            TerminalHubScreen { showHub = false }
        }
        if (!showHub) {
            val activeViewModel = when (selectedBackend) {
                "codex" -> codexViewModel
                "opencode" -> opencodeViewModel
                "cline" -> clineViewModel
                else -> agyViewModel
            }
            val activeUiState by activeViewModel.uiState.collectAsState()

            ChatScreen(
                viewModel = activeViewModel,
                forcedBackend = selectedBackend,
                selectedBackend = selectedBackend,
                onBackendSelected = { selectedBackend = it },
                onExitApp = onExitApp,
                onPreviewHtml = { path ->
                    previewSourcePath = path
                    activeViewModel.openPreview(path) { previewUrl = it }
                },
                modifier = Modifier.fillMaxSize()
            )
            if (previewUrl != null && !activeUiState.showImageMarkupDialog) {
                PreviewScreen(
                    previewUrl!!,
                    previewSourcePath.orEmpty(),
                    { previewUrl = null },
                    activeViewModel
                ) { bitmap ->
                    activeViewModel.openImageMarkup(bitmap, "Preview ekran görüntüsü")
                }
            }
        }
        if (!showHub && previewUrl == null) {
            val pulse = rememberInfiniteTransition(label = "edge-pulse").animateFloat(
                initialValue = 0.18f, targetValue = 0.38f,
                animationSpec = infiniteRepeatable(keyframes {
                    durationMillis = 8000
                    0.18f at 0
                    0.38f at 350
                    0.18f at 900
                    0.18f at 8000
                }, RepeatMode.Restart), label = "edge-alpha"
            )
            Box(Modifier.align(androidx.compose.ui.Alignment.CenterEnd)
                .offset(x = -terminalHubEdgeInset)
                .width(2.dp)
                .fillMaxHeight()
                .semantics { contentDescription = "Terminal Hub açma alanı" }
                .drawBehind {
                    val alpha = if (animationsEnabled) pulse.value else 0.18f
                    drawLine(Color.White.copy(alpha = alpha),
                        androidx.compose.ui.geometry.Offset(size.width - 1.dp.toPx(), size.height * .42f),
                        androidx.compose.ui.geometry.Offset(size.width - 1.dp.toPx(), size.height * .58f),
                        strokeWidth = 1.dp.toPx() + hubDragProgress * 2.dp.toPx())
                })
        }
    }
}
