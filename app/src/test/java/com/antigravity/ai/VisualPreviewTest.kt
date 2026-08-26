package com.antigravity.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.antigravity.ai.data.model.ChatSettings
import com.antigravity.ai.data.model.Message
import com.antigravity.ai.data.model.UsageData
import com.antigravity.ai.data.model.UsageMetrics
import com.antigravity.ai.data.model.UsageStats
import com.antigravity.ai.data.model.VaultItem
import com.antigravity.ai.ui.components.ChatTopBar
import com.antigravity.ai.ui.components.MessageInputBar
import com.antigravity.ai.ui.components.MessageItem
import com.antigravity.ai.ui.components.VaultManagerScreen
import com.antigravity.ai.ui.screens.FigmaGeminiHomeView
import com.antigravity.ai.ui.theme.AntigravityAITheme
import com.antigravity.ai.ui.theme.BackgroundDark
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class VisualPreviewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleUsage = UsageData(
        recent5h = UsageMetrics(
            totalTokens = 12450,
            turnCount = 18,
            usedPercent = 25,
            remainingPercent = 75,
            inputTokens = 8200,
            outputTokens = 4250,
            thinkingTokens = 1100
        ),
        weekly = null,
        lastTurn = UsageStats(totalTokens = 1420, outputTokens = 650, thinkingTokens = 210),
        lastUpdated = "10:45"
    )

    private val sampleVaultFiles = listOf(
        VaultItem(name = "10-Mimari-Plan.md", path = "10-Mimari-Plan.md", isDirectory = false),
        VaultItem(name = "20-Termux-Otomasyon.md", path = "20-Termux-Otomasyon.md", isDirectory = false),
        VaultItem(name = "30-Jetpack-Compose.md", path = "30-Jetpack-Compose.md", isDirectory = false),
        VaultItem(name = "Projeler", path = "Projeler", isDirectory = true),
        VaultItem(name = "Notlar", path = "Notlar", isDirectory = true)
    )

    // 1. Obsidian Vault Explorer View
    @Test
    @Config(qualifiers = "w393dp-h873dp-440dpi", sdk = [33])
    fun capture_obsidian_vault_explorer() {
        composeTestRule.setContent {
            AntigravityAITheme {
                VaultManagerScreen(
                    vaultFiles = sampleVaultFiles,
                    activeFileContent = null,
                    activeFilePath = null,
                    onDismiss = {},
                    onLoadFileContent = {},
                    onSaveNote = { _, _, _ -> },
                    onCreateFolder = {},
                    onDeleteFile = {},
                    onReferenceFile = {},
                    onReferenceParagraph = { _, _ -> }
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("build/outputs/roborazzi/matrix_obsidian_vault_explorer.png")
    }

    // 2. Obsidian Note Editor & Live Reader View
    @Test
    @Config(qualifiers = "w393dp-h873dp-440dpi", sdk = [33])
    fun capture_obsidian_vault_reader() {
        val sampleContent = """
            # Jetpack Compose Mimari Notları
            
            Bu notta Antigravity Android uygulamasının UI/UX yapısı ve Obsidian Design System prensipleri listelenmektedir.
            
            [[Roborazzi]] ile JVM tabanlı ekran yakalama ve [[Antigravity]] görsel analiz döngüsü aktif olarak çalışır.
            
            - [ ] Çoklu ekran görsel testleri (320dp, 360dp, 411dp)
            - [ ] Obsidian ağ haritası entegrasyonu
        """.trimIndent()

        composeTestRule.setContent {
            AntigravityAITheme {
                VaultManagerScreen(
                    vaultFiles = sampleVaultFiles,
                    activeFileContent = sampleContent,
                    activeFilePath = "30-Jetpack-Compose.md",
                    onDismiss = {},
                    onLoadFileContent = {},
                    onSaveNote = { _, _, _ -> },
                    onCreateFolder = {},
                    onDeleteFile = {},
                    onReferenceFile = {},
                    onReferenceParagraph = { _, _ -> }
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("build/outputs/roborazzi/matrix_obsidian_vault_reader.png")
    }

    // 3. Home Screen Standard
    @Test
    @Config(qualifiers = "w393dp-h873dp-440dpi", sdk = [33])
    fun capture_home_screen() {
        composeTestRule.setContent {
            AntigravityAITheme {
                Surface(modifier = Modifier.fillMaxSize(), color = BackgroundDark) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        ChatTopBar(
                            settings = ChatSettings(model = "gemini-3.7-flash-medium"),
                            usage = sampleUsage,
                            isGenerating = false,
                            onMenuClick = {},
                            onNewChatClick = {},
                            onSettingsClick = {},
                            onUsageClick = {}
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            FigmaGeminiHomeView(onSuggestionClick = {}, onOpenVault = {})
                        }
                        MessageInputBar(
                            text = "",
                            onTextChange = {},
                            pastedBlocks = emptyList(),
                            onRemovePastedBlock = {},
                            attachments = emptyList(),
                            onRemoveAttachment = {},
                            selectedModelName = "Gemini 3.7 Flash ⚡",
                            onModelPillClick = {},
                            isGenerating = false,
                            isListening = false,
                            onSend = {},
                            onStop = {},
                            onMicClick = {},
                            onAttachClick = {}
                        )
                    }
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage("build/outputs/roborazzi/matrix_home_screen.png")
    }
}
