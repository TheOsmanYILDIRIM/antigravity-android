package com.antigravity.ai.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.ai.ui.theme.*

sealed class MarkdownBlock {
    data class Header(val level: Int, val text: String) : MarkdownBlock()
    data class Code(val code: String, val language: String) : MarkdownBlock()
    data class Alert(val type: String, val title: String, val text: String) : MarkdownBlock()
    data class BlockQuote(val text: String) : MarkdownBlock()
    data class BulletItem(val text: String, val indentLevel: Int = 0) : MarkdownBlock()
    data class NumberedItem(val number: String, val text: String) : MarkdownBlock()
    object Divider : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
}

@Composable
fun MarkdownRenderer(
    markdown: String,
    fontSizeSp: Float = 13.5f,
    modifier: Modifier = Modifier
) {
    val blocks = parseMarkdownBlocks(markdown)
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Header -> {
                    val headerSize = when (block.level) {
                        1 -> (fontSizeSp * 1.35f).sp
                        2 -> (fontSizeSp * 1.22f).sp
                        3 -> (fontSizeSp * 1.12f).sp
                        else -> fontSizeSp.sp
                    }
                    val topPadding = if (block.level <= 2) 8.dp else 4.dp
                    Column(modifier = Modifier.padding(top = topPadding, bottom = 2.dp)) {
                        Text(
                            text = block.text,
                            fontSize = headerSize,
                            fontWeight = FontWeight.Bold,
                            color = if (block.level <= 2) PrimaryIndigo else TextPrimary,
                            lineHeight = (headerSize.value * 1.3f).sp
                        )
                        if (block.level == 1) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(BorderSubtle)
                            )
                        }
                    }
                }

                is MarkdownBlock.Code -> {
                    CodeBlock(
                        code = block.code,
                        language = block.language,
                        fontSizeSp = (fontSizeSp * 0.9f)
                    )
                }

                is MarkdownBlock.Alert -> {
                    val alertColor = when (block.type.uppercase()) {
                        "TIP" -> SuccessGreen
                        "IMPORTANT", "WARNING" -> WarningAmber
                        "CAUTION" -> DangerRed
                        else -> GeminiBlue
                    }
                    val icon = when (block.type.uppercase()) {
                        "TIP" -> Icons.Default.Lightbulb
                        "IMPORTANT", "WARNING", "CAUTION" -> Icons.Default.Warning
                        else -> Icons.Default.Info
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = alertColor.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, alertColor.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = alertColor,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                if (block.title.isNotEmpty()) {
                                    Text(
                                        text = block.title,
                                        fontSize = (fontSizeSp * 0.95f).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = alertColor
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                }
                                RenderInlineFormattedText(
                                    rawText = block.text,
                                    fontSizeSp = fontSizeSp,
                                    textColor = TextPrimary,
                                    onLinkClick = { url ->
                                        handleLinkClick(url, context, uriHandler)
                                    }
                                )
                            }
                        }
                    }
                }

                is MarkdownBlock.BlockQuote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp))
                            .background(SurfaceVariantDark.copy(alpha = 0.6f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(22.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(PrimaryIndigo)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        RenderInlineFormattedText(
                            rawText = block.text,
                            fontSizeSp = fontSizeSp,
                            textColor = TextSecondary,
                            isItalic = true,
                            onLinkClick = { url -> handleLinkClick(url, context, uriHandler) }
                        )
                    }
                }

                is MarkdownBlock.BulletItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = (block.indentLevel * 12).dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = (fontSizeSp * 0.45f).dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(PrimaryIndigo)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        RenderInlineFormattedText(
                            rawText = block.text,
                            fontSizeSp = fontSizeSp,
                            textColor = TextPrimary,
                            onLinkClick = { url -> handleLinkClick(url, context, uriHandler) }
                        )
                    }
                }

                is MarkdownBlock.NumberedItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${block.number}.",
                            fontSize = (fontSizeSp * 0.95f).sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryIndigo,
                            modifier = Modifier.widthIn(min = 18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        RenderInlineFormattedText(
                            rawText = block.text,
                            fontSizeSp = fontSizeSp,
                            textColor = TextPrimary,
                            onLinkClick = { url -> handleLinkClick(url, context, uriHandler) }
                        )
                    }
                }

                is MarkdownBlock.Divider -> {
                    Divider(
                        color = BorderSubtle,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                is MarkdownBlock.Paragraph -> {
                    RenderInlineFormattedText(
                        rawText = block.text,
                        fontSizeSp = fontSizeSp,
                        textColor = TextPrimary,
                        onLinkClick = { url -> handleLinkClick(url, context, uriHandler) }
                    )
                }
            }
        }
    }
}

private fun handleLinkClick(url: String, context: Context, uriHandler: androidx.compose.ui.platform.UriHandler) {
    try {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            uriHandler.openUri(url)
        } else {
            // Local file link or custom scheme
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val cleanPath = url.replace("file://", "")
            clipboard.setPrimaryClip(ClipData.newPlainText("File Path", cleanPath))
            Toast.makeText(context, "Yol kopyalandı: $cleanPath", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Link", url))
        Toast.makeText(context, "Panoya kopyalandı: $url", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun RenderInlineFormattedText(
    rawText: String,
    fontSizeSp: Float,
    textColor: Color,
    isItalic: Boolean = false,
    onLinkClick: (String) -> Unit
) {
    val annotatedString = buildAnnotatedMarkdown(
        rawText = rawText,
        fontSizeSp = fontSizeSp,
        baseColor = textColor,
        baseItalic = isItalic
    )

    ClickableText(
        text = annotatedString,
        style = TextStyle(
            fontSize = fontSizeSp.sp,
            lineHeight = (fontSizeSp * 1.45f).sp,
            color = textColor
        ),
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    onLinkClick(annotation.item)
                }
        }
    )
}

fun buildAnnotatedMarkdown(
    rawText: String,
    fontSizeSp: Float,
    baseColor: Color,
    baseItalic: Boolean = false
): AnnotatedString {
    return buildAnnotatedString {
        // Regex for inline elements:
        // 1. Links: [text](url)
        // 2. Inline Code: `code`
        // 3. Bold-Italic: ***text*** or ___text___
        // 4. Bold: **text** or __text__
        // 5. Italic: *text* or _text_
        val pattern = Regex("(\\[([^\\]]+)\\]\\(([^\\)]+)\\))|(`([^`]+)`)|(\\*\\*\\*([^*]+)\\*\\*\\*)|(\\*\\*([^*]+)\\*\\*)|(\\*([^*]+)\\*)")

        var currentIndex = 0
        val matches = pattern.findAll(rawText)

        for (match in matches) {
            val range = match.range
            if (range.first > currentIndex) {
                append(rawText.substring(currentIndex, range.first))
            }

            val fullMatch = match.value
            when {
                // Link: [text](url)
                match.groups[1] != null -> {
                    val linkText = match.groups[2]?.value ?: ""
                    val linkUrl = match.groups[3]?.value ?: ""
                    val start = length
                    pushStringAnnotation(tag = "URL", annotation = linkUrl)
                    withStyle(
                        SpanStyle(
                            color = PrimaryIndigo,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append(linkText)
                    }
                    pop()
                }

                // Inline code: `code`
                match.groups[4] != null -> {
                    val codeContent = match.groups[5]?.value ?: ""
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF93B4FC), // Gemini icy cyan/blue
                            background = Color(0xFF282A2C),
                            fontSize = (fontSizeSp * 0.92f).sp
                        )
                    ) {
                        append(" $codeContent ")
                    }
                }

                // Bold-Italic: ***text***
                match.groups[6] != null -> {
                    val content = match.groups[7]?.value ?: ""
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                        append(content)
                    }
                }

                // Bold: **text**
                match.groups[8] != null -> {
                    val content = match.groups[9]?.value ?: ""
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary)) {
                        append(content)
                    }
                }

                // Italic: *text*
                match.groups[10] != null -> {
                    val content = match.groups[11]?.value ?: ""
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(content)
                    }
                }

                else -> {
                    append(fullMatch)
                }
            }
            currentIndex = range.last + 1
        }

        if (currentIndex < rawText.length) {
            append(rawText.substring(currentIndex))
        }

        if (baseItalic) {
            addStyle(SpanStyle(fontStyle = FontStyle.Italic), 0, length)
        }
    }
}

fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = markdown.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()

        // 1. Code block fence
        if (trimmed.startsWith("```")) {
            val language = trimmed.removePrefix("```").trim()
            val codeBuilder = StringBuilder()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeBuilder.append(lines[i]).append("\n")
                i++
            }
            // skip closing fence
            if (i < lines.size) i++
            blocks.add(MarkdownBlock.Code(codeBuilder.toString().trimEnd(), language))
            continue
        }

        // 2. Horizontal divider
        if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
            blocks.add(MarkdownBlock.Divider)
            i++
            continue
        }

        // 3. Headers (#, ##, ###, ####)
        if (trimmed.startsWith("#")) {
            val level = trimmed.takeWhile { it == '#' }.length
            val text = trimmed.dropWhile { it == '#' }.trim()
            blocks.add(MarkdownBlock.Header(level, text))
            i++
            continue
        }

        // 4. Alerts (> [!NOTE], > [!TIP], > [!WARNING], > [!CAUTION])
        if (trimmed.startsWith("> [!")) {
            val alertType = trimmed.substringAfter("> [!").substringBefore("]").trim()
            val alertBuilder = StringBuilder()
            i++
            while (i < lines.size && lines[i].trim().startsWith(">")) {
                alertBuilder.append(lines[i].trim().removePrefix(">").trim()).append(" ")
                i++
            }
            blocks.add(MarkdownBlock.Alert(type = alertType, title = alertType, text = alertBuilder.toString().trim()))
            continue
        }

        // 5. Standard Blockquotes (> text)
        if (trimmed.startsWith(">")) {
            val quoteBuilder = StringBuilder()
            while (i < lines.size && lines[i].trim().startsWith(">")) {
                quoteBuilder.append(lines[i].trim().removePrefix(">").trim()).append(" ")
                i++
            }
            blocks.add(MarkdownBlock.BlockQuote(quoteBuilder.toString().trim()))
            continue
        }

        // 6. Bullet lists (- item, * item)
        if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ")) {
            val indent = line.takeWhile { it.isWhitespace() }.length / 2
            val text = trimmed.substring(2).trim()
            blocks.add(MarkdownBlock.BulletItem(text, indent))
            i++
            continue
        }

        // 7. Numbered lists (1. item, 2. item)
        val numMatch = Regex("^([0-9]+)\\.\\s+(.*)").find(trimmed)
        if (numMatch != null) {
            val num = numMatch.groupValues[1]
            val text = numMatch.groupValues[2]
            blocks.add(MarkdownBlock.NumberedItem(num, text))
            i++
            continue
        }

        // 8. Normal Paragraph
        if (trimmed.isNotEmpty()) {
            val pBuilder = StringBuilder(trimmed)
            i++
            while (i < lines.size && lines[i].isNotBlank() &&
                !lines[i].trim().startsWith("```") &&
                !lines[i].trim().startsWith("#") &&
                !lines[i].trim().startsWith(">") &&
                !lines[i].trim().startsWith("- ") &&
                !lines[i].trim().startsWith("* ") &&
                !Regex("^([0-9]+)\\.\\s+").containsMatchIn(lines[i].trim()) &&
                lines[i].trim() != "---"
            ) {
                pBuilder.append("\n").append(lines[i].trim())
                i++
            }
            blocks.add(MarkdownBlock.Paragraph(pBuilder.toString()))
            continue
        }

        i++
    }

    return blocks
}
