package com.antigravity.ai.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.ai.data.model.PastedBlock
import com.antigravity.ai.ui.theme.*

/**
 * Chat ekranında yapıştırılmış uzun metinleri daraltılmış/genişletilebilir
 * ayrı bir blok olarak gösteren bileşen.
 * Kullanıcı isteği: Belirli bir uzunluktaki yapıştırmalar chat ekranında
 * daralmış genişletilebilir ayrı bir blok olarak duracak.
 */
@Composable
fun CollapsiblePastedBlock(
    block: PastedBlock,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val sizeText = remember(block.charCount) {
        if (block.charCount >= 1024) {
            String.format("%.1f KB", block.charCount / 1024f)
        } else {
            "${block.charCount} kr"
        }
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF141A29),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Collapsed / Header Row (Her zaman görünür, tıklanınca açılıp kapanır)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = null,
                        tint = GeminiBlue,
                        modifier = Modifier.size(15.dp)
                    )

                    Spacer(modifier = Modifier.width(7.dp))

                    Text(
                        text = "Yapıştırılmış Metin",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Line and byte count badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0x263B82F6),
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, GeminiBlue.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "${block.lineCount} satır • $sizeText",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = GeminiBlue,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Daralt" else "Genişlet",
                        tint = TextMuted,
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(if (isExpanded) 180f else 0f)
                    )
                }
            }

            // Expanded Content (Tıklanınca açılan kod/metin bloğu)
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0B0F19))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "METİN İÇERİĞİ",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )

                        IconButton(
                            onClick = {
                                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cb.setPrimaryClip(ClipData.newPlainText("Pasted Content", block.content))
                                Toast.makeText(context, "Metin kopyalandı", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Metni Kopyala",
                                tint = TextMuted,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF060910))
                            .border(0.6.dp, BorderSubtle.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .verticalScroll(rememberScrollState())
                            .padding(8.dp)
                    ) {
                        Text(
                            text = block.content,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }
            }
        }
    }
}
