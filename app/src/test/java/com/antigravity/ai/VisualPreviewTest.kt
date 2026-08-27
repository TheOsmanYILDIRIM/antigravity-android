package com.antigravity.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.test.isRoot
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.antigravity.ai.data.model.*
import com.antigravity.ai.ui.components.*
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
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

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

    // 1. Rich Markdown & Syntax Highlighting Message Test
    @Test
    @Config(qualifiers = "w393dp-h873dp-440dpi", sdk = [33])
    fun capture_rich_markdown_rendering() {
        val sampleMarkdown = """
            # Termux & Antigravity Optimizasyonu
            
            Termux ortamında dosya yolları ve yapılandırmalar `~/.bashrc` ve `.ignore` dosyalarında saklanır.
            
            > [!TIP]
            > CLI komutlarını çalıştırmadan önce `cpulimit -l 50` korumasını aktif tutun.
            
            ```bash
            # Termal koruma ile agy başlatma
            alias agy='agy-limit 50'
            export NODE_OPTIONS="--max-old-space-size=512"
            ```
            
            - **Dosya Yolu:** [`~/.bashrc`](file:///data/data/com.termux/files/home/.bashrc)
            - **CPU Sınırı:** %50 sabit frekans koruması
            
            1. Paketleri güncelleyin
            2. Arka plan süreçlerini denetleyin
        """.trimIndent()

        val botMessage = Message(
            role = "bot",
            content = sampleMarkdown,
            usage = UsageStats(totalTokens = 840, outputTokens = 320, thinkingTokens = 120)
        )

        composeTestRule.setContent {
            AntigravityAITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundDark
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        MessageItem(
                            message = botMessage,
                            isLastBotMessage = true,
                            fontSizeSp = 13.5f
                        )
                    }
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage("build/outputs/roborazzi/matrix_rich_markdown_rendering.png")
    }

    // 2. Gemini Settings Sheet with Font Size & Thermal Guard
    @Test
    @Config(qualifiers = "w393dp-h873dp-440dpi", sdk = [33])
    fun capture_gemini_settings_sheet() {
        composeTestRule.setContent {
            AntigravityAITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundDark
                ) {
                    ModelSettingsDialog(
                        currentSettings = ChatSettings(fontSizeSp = 13.5f, thermalMode = "eco"),
                        availableModels = emptyList(),
                        availableEfforts = emptyList(),
                        onDismiss = {},
                        onSave = {}
                    )
                }
            }
        }
        composeTestRule.onAllNodes(isRoot()).onLast().captureRoboImage("build/outputs/roborazzi/matrix_gemini_settings_sheet.png")
    }

    // 3. Compact Screen (320dp) with Scaled Font (1.3x Accessibility)
    @Test
    @Config(qualifiers = "w320dp-h640dp-320dpi", sdk = [33], fontScale = 1.3f)
    fun capture_compact_screen_accessibility() {
        composeTestRule.setContent {
            AntigravityAITheme {
                Surface(modifier = Modifier.fillMaxSize(), color = BackgroundDark) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        ChatTopBar(
                            settings = ChatSettings(model = "gemini-3.7-flash-medium", fontSizeSp = 11.5f),
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
                    }
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage("build/outputs/roborazzi/matrix_compact_screen_accessibility.png")
    }

    // 4. Standard Home Screen
    @Test
    @Config(qualifiers = "w393dp-h873dp-440dpi", sdk = [33])
    fun capture_home_screen() {
        composeTestRule.setContent {
            AntigravityAITheme {
                Surface(modifier = Modifier.fillMaxSize(), color = BackgroundDark) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        ChatTopBar(
                            settings = ChatSettings(model = "gemini-3.7-flash-medium", fontSizeSp = 13.5f),
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
                    }
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage("build/outputs/roborazzi/matrix_home_screen.png")
    }

    // 5. Auth Token Input Dialog Test
    @Test
    @Config(qualifiers = "w393dp-h873dp-440dpi", sdk = [33])
    fun capture_auth_token_dialog() {
        composeTestRule.setContent {
            AntigravityAITheme {
                Surface(modifier = Modifier.fillMaxSize(), color = BackgroundDark) {
                    AuthTokenDialog(
                        isAuthenticated = false,
                        authMethod = "oauth",
                        onDismiss = {},
                        onSubmitToken = {}
                    )
                }
            }
        }
        composeTestRule.onAllNodes(isRoot()).onLast().captureRoboImage("build/outputs/roborazzi/matrix_auth_token_dialog.png")
    }

    // 6. Full Chat Conversation (user + bot markdown + bot tool call)
    @Test
    @Config(qualifiers = "w393dp-h873dp-440dpi", sdk = [33])
    fun capture_chat_conversation() {
        val userMsg = Message(role = "user", content = "Termux'ta opencode serve çalıştırıp GUI'den bağlanabilir miyiz?")
        val botMarkdown = Message(
            role = "bot",
            content = "Evet. `opencode serve --port 4096` ile sunucu açılır; uygulama **Arka Uç** switch'inden OpenCode'u seçer.",
            usage = UsageStats(totalTokens = 540, outputTokens = 210, thinkingTokens = 90)
        )
        val botTool = Message(
            role = "bot",
            content = "",
            tools = mutableListOf(
                ToolCall(
                    stepIndex = 1,
                    name = "Terminal",
                    state = "ACTIVE",
                    parameters = mapOf("command" to "opencode serve --port 4096")
                )
            )
        )
        composeTestRule.setContent {
            AntigravityAITheme {
                Surface(modifier = Modifier.fillMaxSize(), color = BackgroundDark) {
                    Column(modifier = Modifier.verticalScroll(androidx.compose.foundation.ScrollState(0)).padding(12.dp)) {
                        MessageItem(message = userMsg, fontSizeSp = 13.5f)
                        Spacer(modifier = Modifier.height(8.dp))
                        MessageItem(message = botMarkdown, isLastBotMessage = false, fontSizeSp = 13.5f)
                        Spacer(modifier = Modifier.height(8.dp))
                        MessageItem(message = botTool, isLastBotMessage = true, fontSizeSp = 13.5f)
                    }
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage("build/outputs/roborazzi/matrix_chat_conversation.png")
    }

    // 7. Code Block with Syntax Highlighting
    @Test
    @Config(qualifiers = "w393dp-h873dp-440dpi", sdk = [33])
    fun capture_code_block() {
        val code = """fun greet(name: String) = "Hello, ${'$'}name"
// opencode sunucusuna bağlan
val client = OpenCodeApiService("http://127.0.0.1:4096")"""
        composeTestRule.setContent {
            AntigravityAITheme {
                Surface(modifier = Modifier.fillMaxSize(), color = BackgroundDark) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        CodeBlock(code = code, language = "kotlin", fontSizeSp = 13.5f)
                    }
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage("build/outputs/roborazzi/matrix_code_block.png")
    }

    // 8. Usage Widget
    @Test
    @Config(qualifiers = "w393dp-h873dp-440dpi", sdk = [33])
    fun capture_usage_widget() {
        composeTestRule.setContent {
            AntigravityAITheme {
                Surface(modifier = Modifier.fillMaxSize(), color = BackgroundDark) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        UsageWidget(usage = sampleUsage, onClick = {})
                    }
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage("build/outputs/roborazzi/matrix_usage_widget.png")
    }

    // 9. Chat Drawer (Side Menu)
    @Test
    @Config(qualifiers = "w393dp-h873dp-440dpi", sdk = [33])
    fun capture_chat_drawer() {
        val convs = listOf(
            ConversationMeta(id = "1", title = "opencode entegrasyonu", messageCount = 12),
            ConversationMeta(id = "2", title = "Termux otomasyonu", messageCount = 4),
            ConversationMeta(id = "3", title = "Vault notları", messageCount = 1)
        )
        composeTestRule.setContent {
            AntigravityAITheme {
                Surface(modifier = Modifier.fillMaxSize(), color = BackgroundDark) {
                    ChatDrawer(
                        conversations = convs,
                        currentSessionId = "1",
                        onSelectConversation = {},
                        onNewChat = {},
                        onDeleteConversation = {},
                        onOpenVault = {}
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage("build/outputs/roborazzi/matrix_chat_drawer.png")
    }

    // 10. Vault Browser Sheet
    @Test
    @Config(qualifiers = "w393dp-h873dp-440dpi", sdk = [33])
    fun capture_vault_browser_sheet() {
        composeTestRule.setContent {
            AntigravityAITheme {
                Surface(modifier = Modifier.fillMaxSize(), color = BackgroundDark) {
                    VaultBrowserSheet(
                        vaultFiles = sampleVaultFiles,
                        onDismiss = {},
                        onSelectFile = {}
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage("build/outputs/roborazzi/matrix_vault_browser_sheet.png")
    }

    // 11. Vault Manager Screen (full editor)
    @Test
    @Config(qualifiers = "w393dp-h873dp-440dpi", sdk = [33])
    fun capture_vault_manager_screen() {
        composeTestRule.setContent {
            AntigravityAITheme {
                Surface(modifier = Modifier.fillMaxSize(), color = BackgroundDark) {
                    VaultManagerScreen(
                        vaultFiles = sampleVaultFiles,
                        activeFileContent = "# Mimari Plan\n\n- OpenCode backend adapter eklendi.",
                        activeFilePath = "10-Mimari-Plan.md",
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
        }
        composeTestRule.onRoot().captureRoboImage("build/outputs/roborazzi/matrix_vault_manager_screen.png")
    }

    // 12. Settings with OpenCode backend selected (Arka Uç switch)
    @Test
    @Config(qualifiers = "w393dp-h873dp-440dpi", sdk = [33])
    fun capture_settings_opencode_backend() {
        composeTestRule.setContent {
            AntigravityAITheme {
                Surface(modifier = Modifier.fillMaxSize(), color = BackgroundDark) {
                    ModelSettingsDialog(
                        currentSettings = ChatSettings(fontSizeSp = 13.5f, thermalMode = "eco"),
                        availableModels = emptyList(),
                        availableEfforts = emptyList(),
                        onDismiss = {},
                        onSave = {},
                        currentBackend = "opencode",
                        onBackendChange = {}
                    )
                }
            }
        }
        composeTestRule.onAllNodes(isRoot()).onLast().captureRoboImage("build/outputs/roborazzi/matrix_settings_opencode_backend.png")
    }

    // 13. Multi-Device: Standard 360dp Home Screen
    @Test
    @Config(qualifiers = "w360dp-h800dp-420dpi", sdk = [33])
    fun capture_home_screen_360dp() {
        composeTestRule.setContent {
            AntigravityAITheme {
                Surface(modifier = Modifier.fillMaxSize(), color = BackgroundDark) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        ChatTopBar(
                            settings = ChatSettings(model = "gemini-3.7-flash-medium", fontSizeSp = 13.5f),
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
                    }
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage("build/outputs/roborazzi/matrix_home_screen_360dp.png")
    }

    // 14. Multi-Device: Large 411dp + Accessibility Font Scale 1.25x
    @Test
    @Config(qualifiers = "w411dp-h915dp-420dpi", sdk = [33], fontScale = 1.25f)
    fun capture_home_screen_411dp_fontscale() {
        composeTestRule.setContent {
            AntigravityAITheme {
                Surface(modifier = Modifier.fillMaxSize(), color = BackgroundDark) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        ChatTopBar(
                            settings = ChatSettings(model = "gemini-3.7-flash-medium", fontSizeSp = 16f),
                            usage = sampleUsage,
                            isGenerating = true,
                            onMenuClick = {},
                            onNewChatClick = {},
                            onSettingsClick = {},
                            onUsageClick = {}
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            FigmaGeminiHomeView(onSuggestionClick = {}, onOpenVault = {})
                        }
                    }
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage("build/outputs/roborazzi/matrix_home_screen_411dp_fontscale.png")
    }

    // 15. Compact 320dp Settings Sheet (Arka Uç switch on narrow screen)
    @Test
    @Config(qualifiers = "w320dp-h640dp-320dpi", sdk = [33], fontScale = 1.3f)
    fun capture_compact_320_settings() {
        composeTestRule.setContent {
            AntigravityAITheme {
                Surface(modifier = Modifier.fillMaxSize(), color = BackgroundDark) {
                    ModelSettingsDialog(
                        currentSettings = ChatSettings(fontSizeSp = 11.5f, thermalMode = "eco"),
                        availableModels = emptyList(),
                        availableEfforts = emptyList(),
                        onDismiss = {},
                        onSave = {},
                        currentBackend = "auto",
                        onBackendChange = {}
                    )
                }
            }
        }
        composeTestRule.onAllNodes(isRoot()).onLast().captureRoboImage("build/outputs/roborazzi/matrix_compact_320_settings.png")
    }
}

