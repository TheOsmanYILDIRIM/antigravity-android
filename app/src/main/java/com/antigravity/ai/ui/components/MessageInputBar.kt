package com.antigravity.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.ai.data.model.Attachment
import com.antigravity.ai.data.model.PastedBlock
import com.antigravity.ai.ui.theme.*

@Composable
fun MessageInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    pastedBlocks: List<PastedBlock>,
    onRemovePastedBlock: (PastedBlock) -> Unit,
    attachments: List<Attachment>,
    onRemoveAttachment: (Attachment) -> Unit,
    isGenerating: Boolean,
    isListening: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onMicClick: () -> Unit,
    onAttachClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Surface(
        color = SurfaceDark,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Multi-Paste Blocks & Attachments Chips Bar
            if (pastedBlocks.isNotEmpty() || attachments.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Multi-Pasted Text Chips
                    pastedBlocks.forEachIndexed { index, block ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryIndigo)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Yapıştırma #${index + 1} (${block.lineCount} satır, ${block.charCount}b)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Kaldır",
                                    tint = TextMuted,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { onRemovePastedBlock(block) }
                                )
                            }
                        }
                    }

                    // Attached Files Chips
                    attachments.forEach { att ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceVariantDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = when (att.type) {
                                        "image" -> Icons.Default.Image
                                        "vault" -> Icons.Default.Storage
                                        else -> Icons.Default.Description
                                    },
                                    contentDescription = null,
                                    tint = if (att.type == "vault") WarningAmber else PrimaryIndigo,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = att.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Kaldır",
                                    tint = TextMuted,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { onRemoveAttachment(att) }
                                )
                            }
                        }
                    }
                }
            }

            // 2. Input Row
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Attach button (+)
                IconButton(
                    onClick = onAttachClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(SurfaceVariantDark)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Ekle",
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Input Box
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(InputBackground)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(22.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    // Voice Mic
                    IconButton(
                        onClick = onMicClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Sesle Yaz",
                            tint = if (isListening) DangerRed else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 6.dp)
                    ) {
                        if (text.isEmpty() && pastedBlocks.isEmpty()) {
                            Text(
                                text = if (isListening) "Dinleniyor…" else "Antigravity'ye yazın… (/ ve @ destekli)",
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        }
                        BasicTextField(
                            value = text,
                            onValueChange = onTextChange,
                            textStyle = TextStyle(
                                color = TextPrimary,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            ),
                            cursorBrush = SolidColor(PrimaryIndigo),
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Send or Stop
                val canSend = text.isNotBlank() || pastedBlocks.isNotEmpty() || attachments.isNotEmpty()

                if (isGenerating) {
                    IconButton(
                        onClick = onStop,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(DangerRed)
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = "Durdur", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                } else {
                    IconButton(
                        onClick = onSend,
                        enabled = canSend,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (canSend) {
                                    Brush.linearGradient(listOf(PrimaryIndigo, SecondaryPurple))
                                } else {
                                    Brush.linearGradient(listOf(Color(0xFF334155), Color(0xFF334155)))
                                }
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Gönder",
                            tint = if (canSend) Color.White else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
