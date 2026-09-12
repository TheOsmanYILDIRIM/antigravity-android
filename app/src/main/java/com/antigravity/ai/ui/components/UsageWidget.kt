package com.antigravity.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
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
import com.antigravity.ai.data.model.UsageBucket
import com.antigravity.ai.data.model.UsageData
import com.antigravity.ai.data.model.UsageGroup
import com.antigravity.ai.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

fun formatTokenCount(tokens: Long): String {
    return when {
        tokens >= 1_000_000 -> String.format(Locale.US, "%.1fM", tokens / 1_000_000.0)
        tokens >= 1_000 -> String.format(Locale.US, "%.1fk", tokens / 1_000.0)
        else -> tokens.toString()
    }
}

fun formatResetTime(isoTime: String?): String {
    if (isoTime.isNullOrBlank()) return "-"
    return try {
        val clean = isoTime.substringBefore('.')
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = inputFormat.parse(clean) ?: return isoTime
        val outputFormat = SimpleDateFormat("d MMM, HH:mm", Locale("tr", "TR"))
        outputFormat.format(date)
    } catch (e: Exception) {
        isoTime
    }
}

@Composable
fun UsageWidget(
    usage: UsageData?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fiveHourRemaining = usage?.recent5h?.remainingPercent ?: 100

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
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (fiveHourRemaining > 20) SuccessGreen else DangerRed)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "⚡ %$fiveHourRemaining",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1
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
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Model Kotası & Limitler",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextPrimary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Kapat", tint = TextMuted)
                }
            }

            Divider(color = BorderSubtle, modifier = Modifier.padding(vertical = 12.dp))

            val groups = usage?.groups
            if (!groups.isNullOrEmpty()) {
                // Dynamic Multi-Group Display (Gemini, Claude, GPT, etc.)
                groups.forEachIndexed { idx, group ->
                    UsageGroupCard(group = group)
                    if (idx < groups.size - 1) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            } else {
                // Fallback for standalone/legacy metrics
                val fiveHour = usage?.recent5h
                val fhRemaining = fiveHour?.remainingPercent ?: 100
                val fhUsed = fiveHour?.usedPercent ?: 0

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
                            Text(text = "5 Saatlik Kayan Pencere", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                            Text(
                                text = "%$fhRemaining KALAN",
                                fontSize = 12.sp,
                                color = if (fhRemaining > 20) SuccessGreen else DangerRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = (fhUsed / 100f).coerceIn(0f, 1f),
                            color = if (fhRemaining > 20) PrimaryIndigo else DangerRed,
                            trackColor = BorderSubtle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val weekly = usage?.weekly
                val wRemaining = weekly?.remainingPercent ?: 100
                val wUsed = weekly?.usedPercent ?: 0

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
                            Text(text = "Haftalık Kota", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                            Text(
                                text = "%$wRemaining KALAN",
                                fontSize = 12.sp,
                                color = SecondaryPurple,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = (wUsed / 100f).coerceIn(0f, 1f),
                            color = SecondaryPurple,
                            trackColor = BorderSubtle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Last Turn Telemetry
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
                            Text(
                                text = String.format("%,d", lastTurn.inputTokens),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceVariantDark,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "Çıktı", fontSize = 10.sp, color = TextMuted)
                            Text(
                                text = String.format("%,d", lastTurn.outputTokens),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = PrimaryIndigo
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceVariantDark,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "Düşünme", fontSize = 10.sp, color = TextMuted)
                            Text(
                                text = String.format("%,d", lastTurn.thinkingTokens),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = SecondaryPurple
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Info banner
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SurfaceVariantDark.copy(alpha = 0.6f),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Her model grubu içinde modeller ortak 5 saatlik ve haftalık havuzu paylaşır. Limitler 5 dakikada bir otomatik güncellenir.",
                        fontSize = 11.sp,
                        color = TextMuted,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun UsageGroupCard(group: UsageGroup) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Group Title & Description
            Text(
                text = group.name.ifBlank { "Model Grubu" },
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextPrimary
            )
            if (!group.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = group.description,
                    fontSize = 11.sp,
                    color = TextMuted,
                    lineHeight = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Buckets
            group.buckets.forEachIndexed { bIdx, bucket ->
                if (bIdx > 0) {
                    Divider(color = BorderSubtle.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                }
                UsageBucketRow(bucket = bucket)
            }
        }
    }
}

@Composable
fun UsageBucketRow(bucket: UsageBucket) {
    val is5h = bucket.window.equals("5h", ignoreCase = true) || bucket.id.contains("5h", ignoreCase = true)
    val displayName = when {
        bucket.name.isNotBlank() -> bucket.name
        is5h -> "5 Saatlik Kayan Limit"
        else -> "Haftalık Limit"
    }

    val remainingFraction = bucket.remainingFraction.coerceIn(0.0, 1.0)
    val remainingPercent = (remainingFraction * 100).toInt()
    val usedFraction = (1.0 - remainingFraction).toFloat().coerceIn(0f, 1f)

    val (statusText, statusColor, barColor) = when {
        bucket.disabled -> Triple("Devre Dışı", TextMuted, BorderSubtle)
        remainingFraction == 0.0 -> Triple("❌ %0 (Doldu)", DangerRed, DangerRed)
        remainingFraction > 0.5 -> Triple("%$remainingPercent KALAN", SuccessGreen, PrimaryIndigo)
        remainingFraction > 0.2 -> Triple("%$remainingPercent KALAN", WarningAmber, WarningAmber)
        else -> Triple("%$remainingPercent KALAN", DangerRed, DangerRed)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = displayName,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.5.sp,
                color = TextPrimary
            )
            Text(
                text = statusText,
                fontSize = 11.5.sp,
                color = statusColor,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = if (bucket.disabled) 0f else usedFraction,
            color = barColor,
            trackColor = BorderSubtle,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            val windowLabel = if (is5h) "Pencere: 5 Saat" else "Pencere: Haftalık"
            Text(
                text = windowLabel,
                fontSize = 10.5.sp,
                color = TextMuted
            )

            if (!bucket.resetTime.isNullOrBlank()) {
                val resetStr = formatResetTime(bucket.resetTime)
                Text(
                    text = "⏰ Yenilenme: $resetStr",
                    fontSize = 10.5.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
