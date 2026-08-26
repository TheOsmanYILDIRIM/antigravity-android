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

    // 1. Standard Modern Phone (Pixel 7 - 411dp)
    @Test
    @Config(qualifiers = "w411dp-h891dp-420dpi", sdk = [33])
    fun capture_standard_411dp() {
        renderHomeScreen("build/outputs/roborazzi/matrix_411dp_standard.png")
    }

    // 2. Compact Phone (Galaxy A series / Common Android - 360dp)
    @Test
    @Config(qualifiers = "w360dp-h780dp-360dpi", sdk = [33])
    fun capture_compact_360dp() {
        renderHomeScreen("build/outputs/roborazzi/matrix_360dp_compact.png")
    }

    // 3. Narrow Phone (Extreme Narrow screen - 320dp)
    @Test
    @Config(qualifiers = "w320dp-h640dp-320dpi", sdk = [33])
    fun capture_narrow_320dp() {
        renderHomeScreen("build/outputs/roborazzi/matrix_320dp_narrow.png")
    }

    // 4. Large Font / Accessibility (360dp with 1.3x Font Scale)
    @Test
    @Config(qualifiers = "w360dp-h780dp-360dpi", sdk = [33], fontScale = 1.3f)
    fun capture_large_font_1_3x() {
        renderHomeScreen("build/outputs/roborazzi/matrix_360dp_large_font.png")
    }

    private fun renderHomeScreen(outputPath: String) {
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

        composeTestRule.onRoot().captureRoboImage(outputPath)
    }
}
