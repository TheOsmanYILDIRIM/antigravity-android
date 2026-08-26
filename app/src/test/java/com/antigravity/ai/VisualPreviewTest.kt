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
import com.antigravity.ai.ui.components.ChatTopBar
import com.antigravity.ai.ui.components.MessageInputBar
import com.antigravity.ai.ui.components.MessageItem
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
@Config(qualifiers = "w411dp-h891dp-420dpi", sdk = [33])
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

    @Test
    fun captureFigmaGeminiHomeView() {
        composeTestRule.setContent {
            AntigravityAITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundDark
                ) {
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
                            FigmaGeminiHomeView(
                                onSuggestionClick = {},
                                onOpenVault = {}
                            )
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

        composeTestRule.onRoot().captureRoboImage("build/outputs/roborazzi/preview_home_screen.png")
    }

    @Test
    fun captureChatConversationView() {
        val mockMessages = listOf(
            Message(
                id = "1",
                role = "user",
                content = "Termux üzerinde headless Jetpack Compose UI testini görsel olarak nasıl çalıştırırız?"
            ),
            Message(
                id = "2",
                role = "bot",
                content = "Roborazzi kullanarak JVM üzerinde emülatör veya APK olmadan doğrudan PNG çıktısı üretebiliriz:\n\n```bash\n./gradlew recordRoborazziDebug\n```\n\nBu yöntemle üretilen ekran görüntüsü **Antigravity AI** tarafından anında analiz edilir!",
                usage = UsageStats(totalTokens = 84, outputTokens = 50, thinkingTokens = 12)
            )
        )

        composeTestRule.setContent {
            AntigravityAITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundDark
                ) {
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

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp)
                        ) {
                            items(mockMessages, key = { it.id }) { msg ->
                                val isLastBot = msg.id == "2"
                                MessageItem(message = msg, isLastBotMessage = isLastBot)
                            }
                        }

                        MessageInputBar(
                            text = "Harika, şimdi görsel analizi başlatalım!",
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

        composeTestRule.onRoot().captureRoboImage("build/outputs/roborazzi/preview_chat_conversation.png")
    }
}
