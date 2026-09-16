package com.antigravity.ai.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.antigravity.ai.data.model.resolveMediaUrl
import com.antigravity.ai.ui.theme.*

sealed class MarkdownBlock {
    data class Header(val level: Int, val text: String) : MarkdownBlock()
    data class Code(val code: String, val language: String) : MarkdownBlock()
    data class Mermaid(val code: String) : MarkdownBlock()
    data class Alert(val type: String, val title: String, val text: String) : MarkdownBlock()
    data class BlockQuote(val text: String) : MarkdownBlock()
    data class BulletItem(val text: String, val indentLevel: Int = 0) : MarkdownBlock()
    data class NumberedItem(val number: String, val text: String) : MarkdownBlock()
    data class Image(val alt: String, val url: String) : MarkdownBlock()
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock()
    data class InteractiveChecklist(val title: String, val items: List<String>) : MarkdownBlock()
    data class InteractiveChoice(val question: String, val options: List<String>) : MarkdownBlock()
    object Divider : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
}

@Composable
fun MarkdownRenderer(
    markdown: String,
    fontSizeSp: Float = 13.5f,
    onOpenFile: ((String) -> Unit)? = null,
    onOpenImage: ((String, String) -> Unit)? = null,
    onSendMessage: ((String) -> Unit)? = null,
    onFillInput: ((String) -> Unit)? = null,
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

                is MarkdownBlock.Mermaid -> {
                    MermaidDiagramBlock(
                        code = block.code,
                        fontSizeSp = fontSizeSp,
                        onOpenImage = onOpenImage
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
                                        handleLinkClick(url, context, uriHandler, onOpenFile, onOpenImage)
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
                            onLinkClick = { url -> handleLinkClick(url, context, uriHandler, onOpenFile, onOpenImage) }
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
                            onLinkClick = { url -> handleLinkClick(url, context, uriHandler, onOpenFile, onOpenImage) }
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
                            onLinkClick = { url -> handleLinkClick(url, context, uriHandler, onOpenFile, onOpenImage) }
                        )
                    }
                }

                is MarkdownBlock.Image -> {
                    val resolvedUrl = resolveMediaUrl(block.url)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                if (onOpenImage != null) {
                                    onOpenImage(block.url, block.alt)
                                } else {
                                    handleLinkClick(block.url, context, uriHandler, onOpenFile, onOpenImage)
                                }
                            }
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 140.dp, max = 280.dp)
                                    .background(SurfaceVariantDark),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = resolvedUrl,
                                    contentDescription = block.alt.ifEmpty { "Görsel" },
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 140.dp, max = 280.dp)
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        tint = GeminiBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = block.alt.ifEmpty { block.url.substringAfterLast("/") },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "Büyüt ↗",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GeminiBlue
                                )
                            }
                        }
                    }
                }

                is MarkdownBlock.Table -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(8.dp)
                        ) {
                            // Header Row
                            Row(
                                modifier = Modifier
                                    .background(SurfaceVariantDark, RoundedCornerShape(8.dp))
                                    .padding(vertical = 8.dp, horizontal = 6.dp)
                            ) {
                                block.headers.forEach { h ->
                                    Text(
                                        text = h,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = (fontSizeSp * 0.95f).sp,
                                        color = PrimaryIndigo,
                                        modifier = Modifier
                                            .widthIn(min = 90.dp, max = 220.dp)
                                            .padding(horizontal = 6.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Rows
                            block.rows.forEachIndexed { rIdx, row ->
                                val rowBg = if (rIdx % 2 == 1) SurfaceVariantDark.copy(alpha = 0.45f) else Color.Transparent
                                Row(
                                    modifier = Modifier
                                        .background(rowBg, RoundedCornerShape(6.dp))
                                        .padding(vertical = 6.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    row.forEachIndexed { cIdx, cell ->
                                        Box(
                                            modifier = Modifier
                                                .widthIn(min = 90.dp, max = 220.dp)
                                                .padding(horizontal = 6.dp)
                                        ) {
                                            RenderInlineFormattedText(
                                                rawText = cell,
                                                fontSizeSp = (fontSizeSp * 0.92f),
                                                textColor = TextPrimary,
                                                onLinkClick = { url ->
                                                    handleLinkClick(url, context, uriHandler, onOpenFile, onOpenImage)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                is MarkdownBlock.Divider -> {
                    Divider(
                        color = BorderSubtle,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                is MarkdownBlock.InteractiveChecklist -> {
                    InteractiveChecklistCard(
                        title = block.title,
                        items = block.items,
                        onSendSelected = { selected ->
                            val text = "Seçilenler:\n" + selected.joinToString("\n") { "- $it" }
                            onSendMessage?.invoke(text)
                        }
                    )
                }

                is MarkdownBlock.InteractiveChoice -> {
                    InteractiveChoiceCard(
                        question = block.question,
                        options = block.options,
                        onSelectOption = { chosen ->
                            onSendMessage?.invoke(chosen)
                        },
                        onCustomAnswerClick = {
                            if (onFillInput != null) {
                                onFillInput(" ")
                            } else {
                                onSendMessage?.invoke("Farklı bir seçenek belirtmek istiyorum: ")
                            }
                        }
                    )
                }

                is MarkdownBlock.Paragraph -> {
                    RenderInlineFormattedText(
                        rawText = block.text,
                        fontSizeSp = fontSizeSp,
                        textColor = TextPrimary,
                        onLinkClick = { url -> handleLinkClick(url, context, uriHandler, onOpenFile, onOpenImage) }
                    )
                }
            }
        }
    }
}

@Composable
fun MermaidDiagramBlock(
    code: String,
    fontSizeSp: Float = 13.5f,
    onOpenImage: ((String, String) -> Unit)? = null
) {
    val context = LocalContext.current
    var showCode by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    val mermaidUrl = remember(code) { generateMermaidUrl(code) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .background(SurfaceDark)
    ) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceVariantDark)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountTree,
                    contentDescription = null,
                    tint = PrimaryIndigo,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Akış Şeması",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = PrimaryIndigo.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, PrimaryIndigo.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "Mermaid",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryIndigo,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Toggle Button (Şema / Kod)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (showCode) PrimaryIndigo.copy(alpha = 0.2f) else Color(0xFF282A2C),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, if (showCode) PrimaryIndigo else BorderSubtle),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { showCode = !showCode }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = if (showCode) Icons.Default.Visibility else Icons.Default.Code,
                            contentDescription = null,
                            tint = if (showCode) PrimaryIndigo else TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showCode) "Şema" else "Kod",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (showCode) PrimaryIndigo else TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Copy Code Button
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Mermaid Code", code))
                        Toast.makeText(context, "Mermaid kodu kopyalandı", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Kodu Kopyala",
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                if (!showCode && onOpenImage != null && !isError) {
                    Spacer(modifier = Modifier.width(2.dp))
                    IconButton(
                        onClick = {
                            onOpenImage(mermaidUrl, "Akış Şeması (Mermaid)")
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInFull,
                            contentDescription = "Tam Ekran Büyüt",
                            tint = GeminiBlue,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // Body Content
        if (showCode) {
            Box(modifier = Modifier.padding(8.dp)) {
                CodeBlock(code = code, language = "mermaid", fontSizeSp = fontSizeSp * 0.9f)
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 360.dp)
                    .background(Color(0xFF131415))
                    .clickable {
                        if (!isError && onOpenImage != null) {
                            onOpenImage(mermaidUrl, "Akış Şeması (Mermaid)")
                        }
                    }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (mermaidUrl.isNotEmpty()) {
                    AsyncImage(
                        model = mermaidUrl,
                        contentDescription = "Akış Şeması",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp, max = 340.dp),
                        onLoading = {
                            isLoading = true
                            isError = false
                        },
                        onSuccess = {
                            isLoading = false
                            isError = false
                        },
                        onError = {
                            isLoading = false
                            isError = true
                        }
                    )
                } else {
                    isError = true
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = PrimaryIndigo,
                        strokeWidth = 2.dp
                    )
                }

                if (isError) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Şema görseli yüklenemedi (Çevrimdışı veya geçersiz format)",
                            fontSize = 11.5.sp,
                            color = WarningAmber,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = { showCode = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Mermaid Kodunu Göster", fontSize = 11.sp, color = PrimaryIndigo)
                        }
                    }
                }
            }
        }
    }
}

fun generateMermaidUrl(code: String): String {
    return try {
        val payload = org.json.JSONObject().apply {
            put("code", code.trim())
            put("mermaid", org.json.JSONObject().apply {
                put("theme", "dark")
            })
        }
        val jsonBytes = payload.toString().toByteArray(Charsets.UTF_8)
        val b64 = android.util.Base64.encodeToString(jsonBytes, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
        "https://mermaid.ink/img/$b64"
    } catch (e: Exception) {
        ""
    }
}

private fun handleLinkClick(
    url: String,
    context: Context,
    uriHandler: androidx.compose.ui.platform.UriHandler,
    onOpenFile: ((String) -> Unit)? = null,
    onOpenImage: ((String, String) -> Unit)? = null
) {
    val cleanUrl = url.trim()
    val lower = cleanUrl.lowercase()
    val isImage = lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
            lower.endsWith(".webp") || lower.endsWith(".gif") || lower.endsWith(".svg") || lower.endsWith(".bmp")

    if (isImage && onOpenImage != null) {
        onOpenImage(cleanUrl, cleanUrl.substringAfterLast("/"))
        return
    }

    if ((cleanUrl.startsWith("/") || cleanUrl.startsWith("file://") || cleanUrl.startsWith("~")) && onOpenFile != null) {
        val path = cleanUrl.replace("file://", "")
        onOpenFile(path)
        return
    }

    try {
        if (cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
            uriHandler.openUri(cleanUrl)
        } else {
            val cleanPath = cleanUrl.replace("file://", "")
            if (onOpenFile != null) {
                onOpenFile(cleanPath)
            } else {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("File Path", cleanPath))
                Toast.makeText(context, "Yol kopyalandı: $cleanPath", Toast.LENGTH_SHORT).show()
            }
        }
    } catch (e: Exception) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Link", cleanUrl))
        Toast.makeText(context, "Panoya kopyalandı: $cleanUrl", Toast.LENGTH_SHORT).show()
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
                    val isFilePath = linkUrl.startsWith("file://") || linkUrl.startsWith("/") || linkUrl.startsWith("~")
                    val isImg = linkUrl.endsWith(".png", true) || linkUrl.endsWith(".jpg", true) || linkUrl.endsWith(".jpeg", true) || linkUrl.endsWith(".webp", true)
                    
                    val linkColor = when {
                        isImg -> GeminiBlue
                        isFilePath -> PrimaryIndigo
                        else -> PrimaryIndigo
                    }

                    pushStringAnnotation(tag = "URL", annotation = linkUrl)
                    withStyle(
                        SpanStyle(
                            color = linkColor,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append(linkText)
                    }
                    pop()
                }

                // Inline Code: `code`
                match.groups[4] != null -> {
                    val codeContent = match.groups[5]?.value ?: ""
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF93B4FC),
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

        // 0. Skip HTML comments & Sentinel tags (e.g. <!--__AGY_SESSION_TITLE: ...__-->)
        if (trimmed.startsWith("<!--")) {
            while (i < lines.size && !lines[i].contains("-->")) {
                i++
            }
            if (i < lines.size) i++
            continue
        }

        // 1. Code block fence or Mermaid diagram
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
            val fullCode = codeBuilder.toString().trimEnd()
            val langLower = language.lowercase()
            val isMermaid = langLower == "mermaid" || langLower == "flowchart" ||
                    fullCode.startsWith("flowchart ") || fullCode.startsWith("flowchart\n") ||
                    fullCode.startsWith("graph ") || fullCode.startsWith("graph\n") ||
                    fullCode.startsWith("sequenceDiagram") || fullCode.startsWith("classDiagram") ||
                    fullCode.startsWith("stateDiagram") || fullCode.startsWith("erDiagram")

            if (isMermaid) {
                blocks.add(MarkdownBlock.Mermaid(fullCode))
            } else {
                blocks.add(MarkdownBlock.Code(fullCode, language))
            }
            continue
        }

        // 2. Horizontal divider
        if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
            blocks.add(MarkdownBlock.Divider)
            i++
            continue
        }

        // 2.5 Markdown Table (| H1 | H2 |)
        if (trimmed.startsWith("|") && trimmed.endsWith("|") && i + 1 < lines.size && lines[i + 1].trim().startsWith("|") && lines[i + 1].contains("---")) {
            val headers = trimmed.split("|").map { it.trim() }.filter { it.isNotEmpty() }
            i += 2 // skip header and separator line
            val rows = mutableListOf<List<String>>()
            while (i < lines.size && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|")) {
                val rowCells = lines[i].trim().split("|").map { it.trim() }.filterIndexed { idx, _ -> idx != 0 && idx != lines[i].trim().split("|").lastIndex }
                rows.add(rowCells)
                i++
            }
            if (headers.isNotEmpty()) {
                blocks.add(MarkdownBlock.Table(headers, rows))
                continue
            }
        }

        // 2.6 Standalone Image block (![alt](url))
        val imgMatch = Regex("^!\\[([^\\]]*)\\]\\(([^\\)]+)\\)").find(trimmed)
        if (imgMatch != null) {
            val alt = imgMatch.groupValues[1]
            val url = imgMatch.groupValues[2]
            blocks.add(MarkdownBlock.Image(alt, url))
            i++
            continue
        }

        // 2.7 Interactive Checklist Block (- [ ] or - [x])
        if (trimmed.startsWith("- [ ]") || trimmed.startsWith("- [x]") || trimmed.startsWith("* [ ]") || trimmed.startsWith("* [x]")) {
            val checklistItems = mutableListOf<String>()
            while (i < lines.size) {
                val cl = lines[i].trim()
                if (cl.startsWith("- [ ]") || cl.startsWith("- [x]") || cl.startsWith("* [ ]") || cl.startsWith("* [x]")) {
                    val itemText = cl.substring(5).trim()
                    if (itemText.isNotEmpty()) {
                        checklistItems.add(itemText)
                    }
                    i++
                } else if (cl.isEmpty()) {
                    i++
                    break
                } else {
                    break
                }
            }
            if (checklistItems.isNotEmpty()) {
                blocks.add(MarkdownBlock.InteractiveChecklist("Seçenekler / Görev Listesi", checklistItems))
                continue
            }
        }

        // 2.8 Interactive Choice Block (<choice> or <select>)
        if (trimmed.startsWith("<choice>") || trimmed.startsWith("<select>")) {
            val tag = if (trimmed.startsWith("<choice>")) "choice" else "select"
            val endTag = "</$tag>"
            var question = "Lütfen bir seçenek belirleyin:"
            val options = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trim().contains(endTag)) {
                val optLine = lines[i].trim()
                if (optLine.startsWith("<question>")) {
                    question = optLine.removePrefix("<question>").removeSuffix("</question>").trim()
                } else if (optLine.startsWith("<option>")) {
                    val opt = optLine.removePrefix("<option>").removeSuffix("</option>").trim()
                    if (opt.isNotEmpty()) options.add(opt)
                } else if (optLine.startsWith("- ") || optLine.startsWith("1. ") || optLine.startsWith("2. ") || optLine.startsWith("3. ")) {
                    val opt = optLine.replace(Regex("^[0-9]+\\.\\s*|^-\\s*"), "").trim()
                    if (opt.isNotEmpty()) options.add(opt)
                }
                i++
            }
            if (i < lines.size) i++ // skip endTag
            if (options.isNotEmpty()) {
                blocks.add(MarkdownBlock.InteractiveChoice(question, options))
                continue
            }
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

        // 8. Normal Paragraph (also checking for embedded images)
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
                !lines[i].trim().startsWith("|") &&
                lines[i].trim() != "---"
            ) {
                pBuilder.append("\n").append(lines[i].trim())
                i++
            }

            val fullPara = pBuilder.toString()
            val embeddedImgMatch = Regex("!\\[([^\\]]*)\\]\\(([^\\)]+)\\)").findAll(fullPara).toList()
            if (embeddedImgMatch.isNotEmpty()) {
                var lastIdx = 0
                for (m in embeddedImgMatch) {
                    val before = fullPara.substring(lastIdx, m.range.first).trim()
                    if (before.isNotEmpty()) {
                        blocks.add(MarkdownBlock.Paragraph(before))
                    }
                    blocks.add(MarkdownBlock.Image(m.groupValues[1], m.groupValues[2]))
                    lastIdx = m.range.last + 1
                }
                val after = fullPara.substring(lastIdx).trim()
                if (after.isNotEmpty()) {
                    blocks.add(MarkdownBlock.Paragraph(after))
                }
            } else {
                blocks.add(MarkdownBlock.Paragraph(fullPara))
            }
            continue
        }

        i++
    }

    return blocks
}
