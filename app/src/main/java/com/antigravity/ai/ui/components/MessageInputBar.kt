package com.antigravity.ai.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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

@OptIn(ExperimentalFoundationApi::class)
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
    onOpenFileManager: () -> Unit = {},
    onOpenTemplateFill: (com.antigravity.ai.data.model.PromptTemplate) -> Unit = {},
    onOpenTemplateManager: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showAttachMenu by remember { mutableStateOf(false) }
    var showTemplateMenu by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "generating_pulse")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinner_rotation"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

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
            // Live Status Banner when Generating (Running / Thinking)
            AnimatedVisibility(
                visible = isGenerating,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp, start = 6.dp, end = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(GeminiBlue.copy(alpha = pulseAlpha))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Antigravity düşünüyor ve komutları yürütüyor (Running)...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = GeminiBlue.copy(alpha = pulseAlpha)
                    )
                }
            }

            // Container (Figma Gemini Rounded Pill Input #1E1F20)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(InputBackground)
                    .border(
                        1.dp,
                        if (isGenerating) GeminiBlue.copy(alpha = 0.5f) else BorderSubtle,
                        RoundedCornerShape(24.dp)
                    )
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
                                        imageVector = if (att.type == "vault") Icons.Default.Storage else Icons.Default.Attachment,
                                        contentDescription = null,
                                        tint = if (att.type == "vault") GeminiPurple else PrimaryIndigo,
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

                // Input Text Field
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 28.dp, max = 130.dp)
                ) {
                    if (text.isEmpty() && pastedBlocks.isEmpty() && attachments.isEmpty()) {
                        Text(
                            text = if (isGenerating) "Antigravity çalışıyor..." else "Antigravity'ye bir şey sorun veya / yazın...",
                            color = TextMuted,
                            fontSize = 15.sp
                        )
                    }

                    BasicTextField(
                        value = text,
                        onValueChange = onTextChange,
                        textStyle = TextStyle(
                            color = TextPrimary,
                            fontSize = 15.sp,
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
                        // + Attachment / Long-click Template Action
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceVariantDark)
                                    .combinedClickable(
                                        onClick = { showAttachMenu = true },
                                        onLongClick = { showTemplateMenu = true }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Ekle (Basılı Tut: Şablonlar)",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // 1. Kısa Tıklama Menüsü (SADECE Dosya & Projeler & Galeri)
                            DropdownMenu(
                                expanded = showAttachMenu,
                                onDismissRequest = { showAttachMenu = false },
                                modifier = Modifier
                                    .background(SurfaceDark)
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = GeminiPurple, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("Termux Dosyaları & Projeler", color = TextPrimary, fontSize = 13.5.sp)
                                        }
                                    },
                                    onClick = {
                                        showAttachMenu = false
                                        onOpenFileManager()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = GeminiBlue, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("Cihaz Galerisi / Dosya Seç", color = TextPrimary, fontSize = 13.5.sp)
                                        }
                                    },
                                    onClick = {
                                        showAttachMenu = false
                                        onAttachClick()
                                    }
                                )
                            }

                            // 2. Basılı Tutunca Açılan ŞABLONLAR & PROMPTLAR Menüsü
                            DropdownMenu(
                                expanded = showTemplateMenu,
                                onDismissRequest = { showTemplateMenu = false },
                                modifier = Modifier
                                    .background(SurfaceDark)
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                            ) {
                                // Başlık
                                Surface(
                                    color = SurfaceVariantDark,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Default.Bolt, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "HIZLI ŞABLONLAR & PROMPTLAR",
                                            color = WarningAmber,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                val allTemplates = com.antigravity.ai.data.model.TemplateManager.getTemplates(context)
                                allTemplates.forEach { tpl ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (tpl.id.contains("standard")) Icons.Default.Bolt else Icons.Default.Assignment,
                                                    contentDescription = null,
                                                    tint = if (tpl.id.contains("standard")) WarningAmber else GeminiBlue,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(tpl.title, color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                                                    if (tpl.description.isNotBlank()) {
                                                        Text(tpl.description, color = TextMuted, fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        },
                                        onClick = {
                                            showTemplateMenu = false
                                            onOpenTemplateFill(tpl)
                                        }
                                    )
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = BorderSubtle
                                )

                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Tune, contentDescription = null, tint = GeminiPurple, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("Tüm Şablonları Yönet & Yeni Ekle...", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        }
                                    },
                                    onClick = {
                                        showTemplateMenu = false
                                        onOpenTemplateManager()
                                    }
                                )
                            }
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

                    // Right Actions: Mic & Live Waveform / Send Button
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

                        // Send / Stop / Live Button with Animated Rotating Spinner
                        if (isGenerating) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable { onStop() }
                            ) {
                                // Animated rotating gradient ring
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .rotate(rotationAngle)
                                        .border(
                                            2.dp,
                                            Brush.sweepGradient(
                                                listOf(GeminiBlue, GeminiPurple, GeminiPink, GeminiAmber, GeminiBlue)
                                            ),
                                            CircleShape
                                        )
                                )
                                // Inner Stop Button
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(DangerRed),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Stop,
                                        contentDescription = "Durdur",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        } else {
                            val canSend = text.isNotBlank() || pastedBlocks.isNotEmpty() || attachments.isNotEmpty()
                            if (canSend) {
                                IconButton(
                                    onClick = onSend,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(GeminiBlue)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "Gönder",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else {
                                // Live Waveform Button (Figma & Stitch Gemini Live)
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Transparent,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(GeminiSparkleGradient)
                                        .clickable { onMicClick() }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 7.dp)
                                    ) {
                                        Box(modifier = Modifier.width(2.5.dp).height(10.dp).clip(RoundedCornerShape(2.dp)).background(Color.White))
                                        Spacer(modifier = Modifier.width(2.5.dp))
                                        Box(modifier = Modifier.width(2.5.dp).height(16.dp).clip(RoundedCornerShape(2.dp)).background(Color.White))
                                        Spacer(modifier = Modifier.width(2.5.dp))
                                        Box(modifier = Modifier.width(2.5.dp).height(12.dp).clip(RoundedCornerShape(2.dp)).background(Color.White))
                                        Spacer(modifier = Modifier.width(2.5.dp))
                                        Box(modifier = Modifier.width(2.5.dp).height(7.dp).clip(RoundedCornerShape(2.dp)).background(Color.White))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
