package com.antigravity.ai.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.ai.data.model.ToolCall
import com.antigravity.ai.ui.theme.*

@Composable
fun ToolCard(tool: ToolCall) {
    var isExpanded by remember { mutableStateOf(false) }
    val isDone = tool.state.equals("DONE", ignoreCase = true) || tool.state.equals("ERROR", ignoreCase = true)
    val isError = tool.state.equals("ERROR", ignoreCase = true)
    val accent = when {
        isError -> Color(0xFFF87171)
        isDone -> SuccessGreen
        else -> WarningAmber
    }

    val infiniteTransition = rememberInfiniteTransition(label = "tool_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tool_alpha"
    )

    val toolIcon: ImageVector = when {
        tool.name.contains("command") || tool.name.contains("run") -> Icons.Default.Terminal
        tool.name.contains("file") || tool.name.contains("view") || tool.name.contains("write") -> Icons.Default.Description
        tool.name.contains("search") || tool.name.contains("find") || tool.name.contains("grep") -> Icons.Default.Search
        else -> Icons.Default.Code
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(
                1.dp,
                if (isError || isDone) BorderSubtle else WarningAmber.copy(alpha = pulseAlpha),
                RoundedCornerShape(10.dp)
            )
            .background(Color(0xFF0D1322))
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF141D33))
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = toolIcon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tool.name,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = accent
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Status Pill with Active Animation
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isError || isDone) Color(0x26F87171) else WarningAmber.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isError || isDone) accent.copy(alpha = 0.4f) else WarningAmber.copy(alpha = pulseAlpha)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        if (!isDone) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(8.dp),
                                strokeWidth = 1.5.dp,
                                color = WarningAmber
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                        }
                        Text(
                            text = when {
                            isError -> "HATA"
                            isDone -> "TAMAMLANDI"
                            else -> "ÇALIŞIYOR"
                        },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                    }
                }

                if (tool.durationSeconds != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${String.format("%.1f", tool.durationSeconds)}s",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier
                        .size(14.dp)
                        .rotate(if (isExpanded) 90f else 0f)
                )
            }
        }

        // Expanded Body (Parameters and Output)
        AnimatedVisibility(visible = isExpanded || !isDone) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF090E1A))
                    .padding(10.dp)
            ) {
                if (tool.parameters != null && tool.parameters.isNotEmpty()) {
                    Text(
                        text = "PARAMETRELER:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                    Text(
                        text = tool.parameters.toString(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFFCBD5E1),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                if (!tool.output.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ÇIKTI / TERMINAL:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                    Text(
                        text = tool.output!!,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFFA7F3D0),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (!tool.error.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "HATA:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF87171)
                    )
                    Text(
                        text = tool.error!!,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFFFCA5A5),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (tool.output.isNullOrEmpty() && tool.error.isNullOrEmpty() && !isDone) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(10.dp),
                            strokeWidth = 1.5.dp,
                            color = WarningAmber
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Komut yürütülüyor ve çıktı bekleniyor...",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}
