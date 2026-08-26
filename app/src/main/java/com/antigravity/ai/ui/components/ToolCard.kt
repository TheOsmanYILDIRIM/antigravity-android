package com.antigravity.ai.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
    val isDone = tool.state == "DONE"

    val toolIcon: ImageVector = when {
        tool.name.contains("command") -> Icons.Default.Terminal
        tool.name.contains("file") -> Icons.Default.Description
        tool.name.contains("search") || tool.name.contains("find") -> Icons.Default.Search
        else -> Icons.Default.Code
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(
                1.dp,
                if (isDone) BorderSubtle else PrimaryIndigo,
                RoundedCornerShape(8.dp)
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
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = toolIcon,
                    contentDescription = null,
                    tint = PrimaryIndigo,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = tool.name,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF67E8F9)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isDone) Color(0x2610B981) else Color(0x33F59E0B)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isDone) "TAMAMLANDI" else "ÇALIŞIYOR",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDone) SuccessGreen else WarningAmber
                    )
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

        // Expanded Body
        AnimatedVisibility(visible = isExpanded) {
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
            }
        }
    }
}
