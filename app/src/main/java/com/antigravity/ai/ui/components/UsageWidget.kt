package com.antigravity.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.ai.data.model.UsageData
import com.antigravity.ai.ui.theme.*

fun formatTokenCount(tokens: Long): String {
    return when {
        tokens >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", tokens / 1_000_000.0)
        tokens >= 1_000 -> String.format(java.util.Locale.US, "%.1fk", tokens / 1_000.0)
        else -> tokens.toString()
    }
}

@Composable
fun UsageWidget(
    usage: UsageData?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fiveHourTokens = usage?.recent5h?.totalTokens ?: 0L
    val weeklyTokens = usage?.weekly?.totalTokens ?: 0L

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SurfaceVariantDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(PrimaryIndigo)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "⚡ 5s: ${formatTokenCount(fiveHourTokens)}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = " • H: ${formatTokenCount(weeklyTokens)}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = TextSecondary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageDetailDialog(
    usage: UsageData?,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "CLI Telemetri & Token Kullanımı", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Kapat", tint = TextMuted)
                }
            }

            Divider(color = BorderSubtle, modifier = Modifier.padding(vertical = 12.dp))

            // 1. 5-Hour Window Telemetry
            val fiveHour = usage?.recent5h
            val fhTokens = fiveHour?.totalTokens ?: 0L
            val fhTurns = fiveHour?.turnCount ?: 0

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Son 5 Saatlik Tüketim", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                        Text(text = "$fhTurns Yanıt / Dönüş", fontSize = 11.sp, color = PrimaryIndigo, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${String.format("%,d", fhTokens)} token",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Antigravity CLI tarafından son 5 saatte işlenen toplam girdi, çıktı ve düşünme tokenları.",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Weekly Total Telemetry
            val weekly = usage?.weekly
            val wTokens = weekly?.totalTokens ?: 0L
            val wTurns = weekly?.turnCount ?: 0

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Haftalık Toplam Tüketim", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                        Text(text = "$wTurns Toplam İstek", fontSize = 11.sp, color = SecondaryPurple, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${String.format("%,d", wTokens)} token",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Aktif çalışma alanınızdaki tüm oturumlar boyunca üretilen kümülatif token sayısı.",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Last Turn Telemetry
            val lastTurn = usage?.lastTurn
            if (lastTurn != null && lastTurn.totalTokens > 0) {
                Text(
                    text = "SON DÖNÜŞ AYRINTISI",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceVariantDark,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "Girdi", fontSize = 10.sp, color = TextMuted)
                            Text(text = String.format("%,d", lastTurn.inputTokens), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceVariantDark,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "Çıktı", fontSize = 10.sp, color = TextMuted)
                            Text(text = String.format("%,d", lastTurn.outputTokens), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryIndigo)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceVariantDark,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "Düşünme", fontSize = 10.sp, color = TextMuted)
                            Text(text = String.format("%,d", lastTurn.thinkingTokens), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SecondaryPurple)
                        }
                    }
                }
            }
        }
    }
}
