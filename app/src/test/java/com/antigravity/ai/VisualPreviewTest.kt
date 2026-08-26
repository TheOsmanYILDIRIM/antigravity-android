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
import com.antigravity.ai.data.model.ChatMessage
import com.antigravity.ai.data.model.ModelSettings
import com.antigravity.ai.data.model.UsageInfo
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
                            settings = ModelSettings(model = "gemini-2.5-flash"),
                            usage = UsageInfo(totalTokens = 1420, dailyLimit = 1000000),
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
                            selectedModelName = "Gemini 2.5 Flash ⚡",
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
            ChatMessage(
                id = "1",
                role = "user",
                content = "Termux üzerinde headless Jetpack Compose UI testini görsel olarak nasıl çalıştırırız?"
            ),
            ChatMessage(
                id = "2",
                role = "bot",
                content = "Roborazzi kullanarak JVM üzerinde emülatör veya APK olmadan doğrudan PNG çıktısı üretebiliriz:\n\n```bash\n./gradlew recordRoborazziDebug\n```\n\nBu yöntemle üretilen ekran görüntüsü **Antigravity AI** tarafından anında analiz edilir!",
                tokenCount = 84,
                durationMs = 1240
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
                            settings = ModelSettings(model = "gemini-2.5-flash"),
                            usage = UsageInfo(totalTokens = 3580, dailyLimit = 1000000),
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
                            text = "Harika, şimdi kodda değişiklik yapalım",
                            onTextChange = {},
                            pastedBlocks = emptyList(),
                            onRemovePastedBlock = {},
                            attachments = emptyList(),
                            onRemoveAttachment = {},
                            selectedModelName = "Gemini 2.5 Flash ⚡",
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
