package com.antigravity.ai.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Figma Gemini Dark Surface Palette
val BackgroundDark = Color(0xFF131314)
val SurfaceDark = Color(0xFF1E1F20)
val SurfaceVariantDark = Color(0xFF282A2C)
val SurfaceSelected = Color(0xFF333538)
val InputBackground = Color(0xFF1E1F20)
val BorderSubtle = Color(0xFF37393B)

// Gemini Sparkle & Accent Gradients
val GeminiBlue = Color(0xFF4285F4)
val GeminiPurple = Color(0xFF9B72CB)
val GeminiPink = Color(0xFFD96570)
val GeminiAmber = Color(0xFFF2994A)

val GeminiSparkleGradient = Brush.linearGradient(
    colors = listOf(GeminiBlue, GeminiPurple, GeminiPink, GeminiAmber)
)

val PrimaryIndigo = Color(0xFF6B8AF6)
val SecondaryPurple = Color(0xFF9B72CB)
val AccentPink = Color(0xFFD96570)

// Typography Palette
val TextPrimary = Color(0xFFF0F4F9)
val TextSecondary = Color(0xFFC4C7C5)
val TextMuted = Color(0xFF8E918F)

// Chat Bubbles
val UserBubbleColor = Color(0xFF282A2C)
val BotBubbleColor = Color(0x00000000) // Seamless on dark background as in Figma

// Status Indicators
val SuccessGreen = Color(0xFF34A853)
val WarningAmber = Color(0xFFFBBC04)
val DangerRed = Color(0xFFEA4335)

// Code Blocks
val CodeBlockBackground = Color(0xFF1E1F20)
val CodeBlockBorder = Color(0xFF37393B)

// Deterministic Project Colors (Subtle card theme tints & mini badges)
object ProjectColorUtil {
    private val projectColors = listOf(
        Color(0xFF4285F4), // Gemini Blue
        Color(0xFF10B981), // Emerald Green
        Color(0xFF9B72CB), // Purple / Violet
        Color(0xFFF59E0B), // Amber / Gold
        Color(0xFFEC4899), // Rose Pink
        Color(0xFF06B6D4), // Cyan
        Color(0xFFF97316), // Orange
        Color(0xFF14B8A6), // Teal
        Color(0xFF6366F1), // Indigo
        Color(0xFF84CC16), // Lime
        Color(0xFFA855F7), // Magenta
        Color(0xFFE11D48)  // Crimson
    )

    fun getColorForProject(projectName: String?): Color {
        if (projectName.isNullOrBlank()) return PrimaryIndigo
        val hash = kotlin.math.abs(projectName.lowercase().trim().hashCode())
        return projectColors[hash % projectColors.size]
    }
}
