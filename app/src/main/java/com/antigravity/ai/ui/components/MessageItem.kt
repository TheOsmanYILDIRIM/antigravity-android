package com.antigravity.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.antigravity.ai.data.model.Message
import com.antigravity.ai.data.model.MessageState
import com.antigravity.ai.ui.theme.*

@Composable
fun MessageItem(message: Message) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // Bot Avatar
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.linearGradient(listOf(PrimaryIndigo, SecondaryPurple))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AG",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(if (isUser) UserBubbleColor else SurfaceDark)
                .border(
                    1.dp,
                    if (isUser) Color.Transparent else BorderSubtle,
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .padding(12.dp)
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
                            val imageModel = att.localUri ?: (if (att.relPath != null) "http://127.0.0.1:8080/${att.relPath}" else att.path)
                            AsyncImage(
                                model = imageModel,
                                contentDescription = att.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp, max = 220.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                            )
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SurfaceVariantDark,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (att.type == "vault") Icons.Default.Storage else Icons.Default.Description,
                                        contentDescription = null,
                                        tint = if (att.type == "vault") WarningAmber else PrimaryIndigo,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = att.name,
                                        fontSize = 11.sp,
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

            // Tool calls (if any)
            if (message.tools.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    message.tools.forEach { tool ->
                        ToolCard(tool = tool)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Message content
            if (message.content.isEmpty() && message.state == MessageState.GENERATING) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = PrimaryIndigo
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Antigravity düşünüyor…",
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                }
            } else {
                FormattedMessageText(text = message.content)
            }

            // Usage stats
            if (message.usage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "📊 ${message.usage?.totalTokens ?: 0} token",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                    if ((message.usage?.thinkingTokens ?: 0) > 0) {
                        Text(
                            text = "🧠 ${message.usage?.thinkingTokens} düşünme",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            // User Avatar
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF334155)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "U",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun FormattedMessageText(text: String) {
    val parts = text.split("```")
    Column(modifier = Modifier.fillMaxWidth()) {
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                // Code block
                val lines = part.split("\n", limit = 2)
                val lang = lines.getOrNull(0)?.trim() ?: "code"
                val code = lines.getOrNull(1) ?: ""
                CodeBlock(code = code.trimEnd(), language = lang)
            } else {
                // Normal text
                if (part.isNotEmpty()) {
                    Text(
                        text = part.trim(),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}
