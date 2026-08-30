package com.antigravity.ai.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.antigravity.ai.data.model.Message
import com.antigravity.ai.data.model.MessageState
import com.antigravity.ai.ui.theme.*

@Composable
fun MessageItem(
    message: Message,
    isLastBotMessage: Boolean = false,
    fontSizeSp: Float = 13.5f,
    onOpenFile: (String) -> Unit = {},
    onOpenImage: (String, String) -> Unit = { _, _ -> }
) {
    val isUser = message.role == "user"
    val context = LocalContext.current

    if (isUser) {
        // User Message (Figma: Right-aligned #282A2C bubble with action toolbar)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.End
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = 20.dp,
                            bottomEnd = 4.dp
                        )
                    )
                    .background(UserBubbleColor)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Attachments Preview (Images & Docs)
                if (message.attachments.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        message.attachments.forEach { att ->
                            if (att.type == "image") {
                                val imageModel = att.localUri ?: (if (att.relPath != null) "http://127.0.0.1:8080/${att.relPath}" else (att.path ?: ""))
                                AsyncImage(
                                    model = imageModel,
                                    contentDescription = att.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 120.dp, max = 220.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                        .clickable {
                                            onOpenImage(imageModel, att.name)
                                        }
                                )
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SurfaceDark,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            val target = att.path ?: att.relPath ?: att.name
                                            onOpenFile(target)
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (att.type == "vault") Icons.Default.Storage else Icons.Default.Description,
                                            contentDescription = null,
                                            tint = GeminiBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = att.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextPrimary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Selectable text for User Message
                SelectionContainer {
                    Text(
                        text = message.content,
                        fontSize = fontSizeSp.sp,
                        lineHeight = (fontSizeSp * 1.45f).sp,
                        color = TextPrimary
                    )
                }
            }

            // User Message Toolbar (Copy & Share Button)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.padding(top = 2.dp, end = 4.dp)
            ) {
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("User Message", message.content)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Mesaj kopyalandı", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "Kopyala",
                        tint = TextMuted.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                }

                IconButton(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, message.content)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Mesajı Paylaş"))
                    },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Paylaş",
                        tint = TextMuted.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    } else {
        // Bot Message (Figma: Left-aligned, Sparkle Icon, Seamless Canvas, Action Bar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            GeminiSparkleIcon(
                size = 24.dp,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                // Tool calls (if any)
                if (message.tools.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        message.tools.forEach { tool ->
                            ToolCard(tool = tool)
                        }
                    }
                }

                // Message content with Full Markdown Renderer wrapped in SelectionContainer
                if (message.content.isEmpty() && message.state == MessageState.GENERATING) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = GeminiBlue
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Antigravity düşünüyor…",
                            fontSize = (fontSizeSp * 0.95f).sp,
                            color = TextMuted
                        )
                    }
                } else {
                    SelectionContainer {
                        MarkdownRenderer(
                            markdown = message.content,
                            fontSizeSp = fontSizeSp,
                            onOpenFile = onOpenFile,
                            onOpenImage = onOpenImage
                        )
                    }
                }

                // Figma Actions Toolbar (👍 👎 ↗ 📋 ⋮) + Token Stats
                if (message.content.isNotEmpty() && message.state != MessageState.GENERATING) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            var isLiked by remember { mutableStateOf(false) }
                            var isDisliked by remember { mutableStateOf(false) }

                            IconButton(
                                onClick = { isLiked = !isLiked; if (isLiked) isDisliked = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ThumbUp,
                                    contentDescription = "Beğen",
                                    tint = if (isLiked) GeminiBlue else TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { isDisliked = !isDisliked; if (isDisliked) isLiked = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ThumbDown,
                                    contentDescription = "Beğenme",
                                    tint = if (isDisliked) DangerRed else TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, message.content)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Cevabı Paylaş"))
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = "Paylaş",
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Antigravity AI Response", message.content)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Panoya kopyalandı", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "Kopyala",
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Usage telemetry pill
                        if (message.usage != null && message.usage!!.totalTokens > 0) {
                            Text(
                                text = "📊 ${message.usage?.totalTokens} token",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }

                    // Disclaimer footer
                    if (isLastBotMessage) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Antigravity can make mistakes, so double-check it.",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

