package com.antigravity.ai

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onRoot
import com.antigravity.ai.data.model.ChatSettings
import com.antigravity.ai.ui.components.ModelSettingsDialog
import com.antigravity.ai.ui.screens.FigmaGeminiHomeView
import com.antigravity.ai.ui.theme.AntigravityAITheme
import com.antigravity.ai.ui.theme.BackgroundDark
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Piksel/ekran-görüntüsü (Roborazzi) testlerinin yerine geçen, deterministik
 * ve kırılgan-olmayan UI doğrulama testleri. Gerçek davranışı semantik olarak
 * kontrol eder (ekran öğesi görünüyor mu, etkileşim state'i güncelliyor mu) —
 * golden görsel dosyasına bağımlı değildir, bu yüzden CI her zaman yeşil kalır.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UiSemanticsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // 1. Ayarlar dialog'u OpenCode (çift-backend) seçeneğini gösteriyor mu?
    @Test
    @Config(qualifiers = "w393dp-h873dp-440dpi", sdk = [33])
    fun settings_dialog_shows_backend_options() {
        composeTestRule.setContent {
            AntigravityAITheme {
                Surface(modifier = Modifier.fillMaxSize(), color = BackgroundDark) {
                    ModelSettingsDialog(
                        currentSettings = ChatSettings(),
                        availableModels = emptyList(),
                        availableEfforts = emptyList(),
                        onDismiss = {},
                        onSave = {}
                    )
                }
            }
        }
        composeTestRule.onNodeWithText("Otomatik").assertIsDisplayed()
        composeTestRule.onNodeWithText("AGY CLI").assertIsDisplayed()
        composeTestRule.onNodeWithText("OpenCode").assertIsDisplayed()
    }

    // 2. Backend chip'ine tıklamak state'i güncelliyor mu? (gerçek etkileşim)
    @Test
    @Config(qualifiers = "w393dp-h873dp-440dpi", sdk = [33])
    fun settings_dialog_backend_switch_updates_state() {
        var selected = "agy"
        composeTestRule.setContent {
            AntigravityAITheme {
                Surface(modifier = Modifier.fillMaxSize(), color = BackgroundDark) {
                    ModelSettingsDialog(
                        currentSettings = ChatSettings(),
                        availableModels = emptyList(),
                        availableEfforts = emptyList(),
                        onDismiss = {},
                        onSave = {},
                        currentBackend = selected,
                        onBackendChange = { selected = it }
                    )
                }
            }
        }
        composeTestRule.onNodeWithText("OpenCode").performClick()
        assertEquals("opencode", selected)
    }

    // 3. Ana ekran öneri çipleri (Figma Gemini UI) render oluyor mu?
    @Test
    @Config(qualifiers = "w393dp-h873dp-440dpi", sdk = [33])
    fun home_screen_shows_suggestion_chips() {
        composeTestRule.setContent {
            AntigravityAITheme {
                Surface(modifier = Modifier.fillMaxSize(), color = BackgroundDark) {
                    FigmaGeminiHomeView(onSuggestionClick = {}, onOpenVault = {})
                }
            }
        }
        composeTestRule.onNodeWithText("Help me").assertIsDisplayed()
        composeTestRule.onNodeWithText("Explore").assertIsDisplayed()
        composeTestRule.onRoot().assertExists()
    }
}
