package com.antigravity.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
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
        composeTestRule.onRoot().captureRoboImage("build/outputs/roborazzi/matrix_gemini_settings_sheet.png")
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
        composeTestRule.onRoot().captureRoboImage("build/outputs/roborazzi/matrix_auth_token_dialog.png")
    }
}

