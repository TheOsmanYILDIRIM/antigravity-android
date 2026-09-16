package com.antigravity.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.ai.data.model.EffortItem
import com.antigravity.ai.data.model.ModelItem
import com.antigravity.ai.ui.theme.*

/**
 * Dedicated, ultra-clean Model Selector Bottom Sheet.
 * Separated from full server/system settings for lightning-fast model switching.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickModelSelectorSheet(
    selectedModelId: String,
    selectedEffort: String,
    availableModels: List<ModelItem>,
    availableEfforts: List<EffortItem>,
    onSelectModel: (modelId: String, effort: String) -> Unit,
    onDismiss: () -> Unit
) {
    var tempModelId by remember { mutableStateOf(selectedModelId) }
    var tempEffort by remember { mutableStateOf(selectedEffort) }

    val modelsList = if (availableModels.isNotEmpty()) availableModels else listOf(
        ModelItem("gemini-3.8-flash-high", "Gemini 3.8 Flash (High)", "Yüksek akıl yürütme & ultra hızlı yanıt"),
        ModelItem("gemini-3.8-flash-medium", "Gemini 3.8 Flash (Medium)", "Dengeli standart model"),
        ModelItem("gemini-3.8-flash-low", "Gemini 3.8 Flash (Low)", "Minimum düşünme gecikmesi"),
        ModelItem("gemini-3.7-flash-high", "Gemini 3.7 Flash (High)", "Yüksek akıl yürütme & hızlı yanıt"),
        ModelItem("gemini-3.7-flash-medium", "Gemini 3.7 Flash (Medium)", "Dengeli standart model"),
        ModelItem("gemini-3.7-flash-low", "Gemini 3.7 Flash (Low)", "Minimum düşünme gecikmesi"),
        ModelItem("gemini-3.1-pro-high", "Gemini 3.1 Pro (High)", "Derin mimari ve karmaşık kodlama"),
        ModelItem("claude-sonnet-4-6", "Claude Sonnet 4.6 (Thinking)", "Gelişmiş analitik akıl yürütme"),
        ModelItem("claude-opus-4-6-thinking", "Claude Opus 4.6 (Thinking)", "En yüksek kapasiteli düşünme modeli")
    )

    val effortsList = if (availableEfforts.isNotEmpty()) availableEfforts else listOf(
        EffortItem("default", "Varsayılan", "Standart"),
        EffortItem("low", "Düşük", "Hızlı"),
        EffortItem("medium", "Orta", "Dengeli"),
        EffortItem("high", "Yüksek", "Derin Düşünme")
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
                color = TextMuted.copy(alpha = 0.4f),
                shape = RoundedCornerShape(3.dp)
            ) {
                Box(modifier = Modifier.size(width = 36.dp, height = 4.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = GeminiBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Model Seçimi",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kapat",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Thinking Effort Selector Chips
            Text(
                text = "Düşünme Seviyesi (Reasoning / Effort)",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                effortsList.forEach { effort ->
                    val isEffortSelected = tempEffort == effort.id
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isEffortSelected) PrimaryIndigo.copy(alpha = 0.2f) else SurfaceVariantDark,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isEffortSelected) PrimaryIndigo else BorderSubtle
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                tempEffort = effort.id
                                onSelectModel(tempModelId, tempEffort)
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = effort.name,
                                fontSize = 12.sp,
                                fontWeight = if (isEffortSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isEffortSelected) PrimaryIndigo else TextPrimary
                            )
                        }
                    }
                }
            }

            Divider(color = BorderSubtle, thickness = 1.dp, modifier = Modifier.padding(bottom = 12.dp))

            // Models List
            Text(
                text = "Kullanılabilir Modeller",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(modelsList) { model ->
                    val isSelected = tempModelId == model.id
                    val isClaude = model.id.contains("claude", ignoreCase = true)
                    val isPro = model.id.contains("pro", ignoreCase = true)
                    val isThinking = model.name.contains("Thinking", ignoreCase = true) || model.id.contains("high", ignoreCase = true)

                    val brandColor = when {
                        isClaude -> Color(0xFFD97706) // Claude Amber
                        isPro -> PrimaryIndigo
                        else -> GeminiBlue
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) brandColor.copy(alpha = 0.12f) else SurfaceVariantDark.copy(alpha = 0.7f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) brandColor else BorderSubtle
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                tempModelId = model.id
                                onSelectModel(tempModelId, tempEffort)
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Model icon
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(brandColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isThinking) Icons.Default.Psychology else Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = brandColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = model.name,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) brandColor else TextPrimary
                                    )
                                    if (isThinking) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = brandColor.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "Thinking",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = brandColor,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                if (model.description.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = model.description,
                                        fontSize = 11.5.sp,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(brandColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Seçili",
                                        tint = Color.Black,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
