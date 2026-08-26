package com.antigravity.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.ai.data.model.UsageData
import com.antigravity.ai.ui.theme.*

@Composable
fun UsageWidget(
    usage: UsageData?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fiveHourRemaining = usage?.fiveHour?.remainingPercent ?: 100
    val weeklyRemaining = usage?.weekly?.remainingPercent ?: 100

    val badgeColor = when {
        fiveHourRemaining < 20 -> DangerRed
        fiveHourRemaining < 50 -> WarningAmber
        else -> SuccessGreen
    }

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
                    .background(badgeColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "⚡ 5s: %$fiveHourRemaining",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = " • H: %$weeklyRemaining",
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
                    Text(text = "Model Kullanım & Kota Takibi", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Kapat", tint = TextMuted)
                }
            }

            Divider(color = BorderSubtle, modifier = Modifier.padding(vertical = 12.dp))

            // 1. 5-Hour Rolling Window
            val fiveHour = usage?.fiveHour
            val fhUsed = fiveHour?.usedTokens ?: 0
            val fhLimit = fiveHour?.limitTokens ?: 250000
            val fhPercent = fiveHour?.usedPercent ?: 0
            val fhRemaining = fiveHour?.remainingPercent ?: 100

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
                        Text(text = "5 Saatlik Kayan Pencere Kotası", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                        Text(text = "%$fhRemaining Kalan", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (fhRemaining > 30) SuccessGreen else DangerRed)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (fhPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (fhPercent > 80) DangerRed else PrimaryIndigo,
                        trackColor = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Kullanılan: ${String.format("%,d", fhUsed)} token", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextMuted)
                        Text(text = "Limit: ${String.format("%,d", fhLimit)}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Weekly Quota
            val weekly = usage?.weekly
            val wUsed = weekly?.usedTokens ?: 0
            val wLimit = weekly?.limitTokens ?: 2000000
            val wPercent = weekly?.usedPercent ?: 0
            val wRemaining = weekly?.remainingPercent ?: 100

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
                        Text(text = "Haftalık Toplam Kota", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                        Text(text = "%$wRemaining Kalan", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SuccessGreen)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (wPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = SecondaryPurple,
                        trackColor = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Kullanılan: ${String.format("%,d", wUsed)} token", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextMuted)
                        Text(text = "Limit: ${String.format("%,d", wLimit)}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Totals
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val inputToks = usage?.totals?.get("inputTokens") ?: 0
                val outputToks = usage?.totals?.get("outputTokens") ?: 0
                val thinkToks = usage?.totals?.get("thinkingTokens") ?: 0

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceVariantDark,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "Girdi Token", fontSize = 10.sp, color = TextMuted)
                        Text(text = String.format("%,d", inputToks), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceVariantDark,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "Çıktı Token", fontSize = 10.sp, color = TextMuted)
                        Text(text = String.format("%,d", outputToks), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryIndigo)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceVariantDark,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "Düşünme Token", fontSize = 10.sp, color = TextMuted)
                        Text(text = String.format("%,d", thinkToks), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SecondaryPurple)
                    }
                }
            }
        }
    }
}
