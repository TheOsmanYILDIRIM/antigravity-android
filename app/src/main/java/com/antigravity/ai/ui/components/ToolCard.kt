package com.antigravity.ai.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.ai.data.model.ToolCall
import com.antigravity.ai.ui.theme.*

/**
 * Toplu komut/araç çağrısı kapsayıcısı.
 * Kullanıcı isteği: Tüm komutlar dikeyde dar, tek bir üst (parent) kod bloğu içinde
 * toplanır ve bu blok daraltılıp açılabilir.
 */
@Composable
fun ToolCallsContainer(
    tools: List<ToolCall>,
    isGenerating: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (tools.isEmpty()) return

    val anyRunning = tools.any { !it.state.equals("DONE", true) && !it.state.equals("ERROR", true) }
    val anyError = tools.any { it.state.equals("ERROR", true) || !it.error.isNullOrBlank() }
    val allDone = tools.all { it.state.equals("DONE", true) }

    // Üretim sürerken otomatik açık, bittiğinde kullanıcı manuel kapatıp açabilir (varsayılan: bittiğinde kapalı)
    var isExpanded by remember(tools.size, isGenerating) {
        mutableStateOf(anyRunning || (isGenerating && tools.size <= 2))
    }

    val totalDuration = tools.mapNotNull { it.durationSeconds }.sum()

    val headerBorderColor = when {
        anyError -> Color(0xFFEF4444).copy(alpha = 0.5f)
        anyRunning -> WarningAmber.copy(alpha = 0.5f)
        allDone -> SuccessGreen.copy(alpha = 0.4f)
        else -> BorderSubtle
    }

    val headerBgColor = when {
        anyError -> Color(0xFF1C1318)
        anyRunning -> Color(0xFF191610)
        else -> Color(0xFF101726)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, headerBorderColor, RoundedCornerShape(12.dp))
            .background(Color(0xFF090D17))
    ) {
        // Parent Header: Tek tıkla açılıp daraltılabilir
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBgColor)
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (anyRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(13.dp),
                        strokeWidth = 2.dp,
                        color = WarningAmber
                    )
                } else {
                    Icon(
                        imageVector = if (anyError) Icons.Default.Warning else Icons.Default.Terminal,
                        contentDescription = null,
                        tint = if (anyError) Color(0xFFF87171) else if (allDone) SuccessGreen else GeminiBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = when {
                        anyRunning -> "Komut Yürütülüyor (${tools.count { it.state.equals("DONE", true) }}/${tools.size})"
                        anyError -> "${tools.size} Komut / Araç (${tools.count { it.state.equals("ERROR", true) }} Hata)"
                        else -> "${tools.size} Komut & Araç Yürütüldü"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (anyError) Color(0xFFFCA5A5) else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (totalDuration > 0) {
                    Text(
                        text = "${String.format("%.1f", totalDuration)}s",
                        fontSize = 10.5.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }

                // Status pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        anyError -> Color(0x33EF4444)
                        anyRunning -> WarningAmber.copy(alpha = 0.2f)
                        else -> SuccessGreen.copy(alpha = 0.18f)
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        0.8.dp,
                        when {
                            anyError -> Color(0xFFEF4444).copy(alpha = 0.4f)
                            anyRunning -> WarningAmber.copy(alpha = 0.4f)
                            else -> SuccessGreen.copy(alpha = 0.4f)
                        }
                    )
                ) {
                    Text(
                        text = when {
                            anyError -> "HATA"
                            anyRunning -> "ÇALIŞIYOR"
                            else -> "TAMAM"
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            anyError -> Color(0xFFF87171)
                            anyRunning -> WarningAmber
                            else -> SuccessGreen
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Daralt" else "Genişlet",
                    tint = TextMuted,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(if (isExpanded) 180f else 0f)
                )
            }
        }

        // Expanded Body: Dikeyde dar ve kompakt komutlar listesi
        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                tools.forEachIndexed { index, tool ->
                    if (index > 0) {
                        Divider(
                            color = BorderSubtle.copy(alpha = 0.35f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                    CompactToolRow(tool = tool)
                }
            }
        }
    }
}

/**
 * Parent blok içinde dikeyde minimum yer kaplayan, tıklanınca çıktı/parametre detayını açan satır.
 */
@Composable
fun CompactToolRow(tool: ToolCall) {
    var isDetailOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val isDone = tool.state.equals("DONE", ignoreCase = true) || tool.state.equals("ERROR", ignoreCase = true)
    val isError = tool.state.equals("ERROR", ignoreCase = true)

    val icon: ImageVector = when {
        tool.name.contains("command") || tool.name.contains("run") -> Icons.Default.Terminal
        tool.name.contains("file") || tool.name.contains("view") || tool.name.contains("write") -> Icons.Default.Description
        tool.name.contains("search") || tool.name.contains("find") || tool.name.contains("grep") -> Icons.Default.Search
        else -> Icons.Default.Code
    }

    val paramSummary = remember(tool.parameters) {
        if (tool.parameters == null || tool.parameters.isEmpty()) ""
        else {
            val p = tool.parameters
            val cmd = p["CommandLine"] as? String
            val path = (p["TargetFile"] ?: p["AbsolutePath"] ?: p["SearchPath"] ?: p["DirectoryPath"]) as? String
            val query = (p["Query"] ?: p["Pattern"] ?: p["Prompt"]) as? String
            when {
                !cmd.isNullOrBlank() -> cmd
                !path.isNullOrBlank() -> path.split("/").takeLast(2).joinToString("/")
                !query.isNullOrBlank() -> query
                else -> p.entries.firstOrNull()?.let { "${it.key}: ${it.value}" } ?: ""
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isDetailOpen = !isDetailOpen }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Status Indicator Dot
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isError -> Color(0xFFF87171)
                            isDone -> SuccessGreen
                            else -> WarningAmber
                        }
                    )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = when {
                    isError -> Color(0xFFF87171)
                    isDone -> SuccessGreen.copy(alpha = 0.8f)
                    else -> WarningAmber
                },
                modifier = Modifier.size(13.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = tool.name,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                color = Color(0xFF93C5FD)
            )

            if (paramSummary.isNotBlank()) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = paramSummary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.5.sp,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            if (tool.durationSeconds != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${String.format("%.1f", tool.durationSeconds)}s",
                    fontSize = 9.5.sp,
                    color = TextMuted.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(12.dp)
                    .rotate(if (isDetailOpen) 90f else 0f)
            )
        }

        // Expanded detail for this tool
        AnimatedVisibility(visible = isDetailOpen) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF060910))
                    .border(0.8.dp, BorderSubtle.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                // Header with Copy
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "KOMUT AYRINTISI",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )

                    val textToCopy = (tool.output ?: tool.error ?: tool.parameters?.toString() ?: "").trim()
                    if (textToCopy.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cb.setPrimaryClip(ClipData.newPlainText("Tool Output", textToCopy))
                                Toast.makeText(context, "Kopyalandı", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Kopyala",
                                tint = TextMuted,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                if (tool.parameters != null && tool.parameters.isNotEmpty()) {
                    Text(
                        text = tool.parameters.toString(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.5.sp,
                        color = Color(0xFFCBD5E1),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                if (!tool.output.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(top = 2.dp)
                    ) {
                        Text(
                            text = tool.output!!,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.5.sp,
                            color = Color(0xFFA7F3D0)
                        )
                    }
                }

                if (!tool.error.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 100.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(top = 2.dp)
                    ) {
                        Text(
                            text = tool.error!!,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.5.sp,
                            color = Color(0xFFFCA5A5)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tekil kullanım için geriye dönük uyumlu bileşen.
 */
@Composable
fun ToolCard(tool: ToolCall) {
    ToolCallsContainer(tools = listOf(tool))
}
