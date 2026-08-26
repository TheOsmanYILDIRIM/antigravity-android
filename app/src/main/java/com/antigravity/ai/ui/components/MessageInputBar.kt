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
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    selectedModelName: String = "Gemini 3.7 Flash",
    onModelPillClick: () -> Unit = {},
    isGenerating: Boolean,
    isListening: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onMicClick: () -> Unit,
    onAttachClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = BackgroundDark,
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // Container (Figma Gemini Rounded Pill Input #1E1F20)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(InputBackground)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Multi-Paste and Attachment Chips
                if (pastedBlocks.isNotEmpty() || attachments.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pastedBlocks.forEach { block ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceVariantDark,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = null,
                                        tint = GeminiBlue,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${block.lineCount} satır metin",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { onRemovePastedBlock(block) },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Kaldır",
                                            tint = TextMuted,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }

                        attachments.forEach { att ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceVariantDark,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (att.type == "image") Icons.Default.Image else Icons.Default.Description,
                                        contentDescription = null,
                                        tint = GeminiPurple,
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
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { onRemoveAttachment(att) },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Kaldır",
                                            tint = TextMuted,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Text Input Field
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 28.dp, max = 140.dp)
                ) {
                    if (text.isEmpty() && pastedBlocks.isEmpty()) {
                        Text(
                            text = if (isListening) "Dinleniyor..." else "Ask Gemini...",
                            color = TextMuted,
                            fontSize = 16.sp
                        )
                    }

                    BasicTextField(
                        value = text,
                        onValueChange = onTextChange,
                        textStyle = TextStyle(
                            color = TextPrimary,
                            fontSize = 16.sp,
                            lineHeight = 22.sp
                        ),
                        cursorBrush = SolidColor(GeminiBlue),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action Sub-row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Left Actions: + Attach and Model Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // + Attachment
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(SurfaceVariantDark)
                                .clickable { onAttachClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Ekle",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Model Selector Pill (Figma "Fast" / Model chip)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SurfaceVariantDark,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onModelPillClick() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = GeminiBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = selectedModelName.replace(" (Medium)", "").replace(" (High)", " ⚡"),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Right Actions: Mic & Send
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Voice Mic
                        IconButton(
                            onClick = onMicClick,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isListening) DangerRed.copy(alpha = 0.2f) else Color.Transparent)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Mic,
                                contentDescription = "Sesli Yaz",
                                tint = if (isListening) DangerRed else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Send / Stop Button
                        if (isGenerating) {
                            IconButton(
                                onClick = onStop,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(DangerRed)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Stop,
                                    contentDescription = "Durdur",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            val canSend = text.isNotBlank() || pastedBlocks.isNotEmpty() || attachments.isNotEmpty()
                            IconButton(
                                onClick = onSend,
                                enabled = canSend,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (canSend) TextPrimary else SurfaceVariantDark)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Gönder",
                                    tint = if (canSend) BackgroundDark else TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
