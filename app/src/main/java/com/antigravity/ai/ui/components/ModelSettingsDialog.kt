package com.antigravity.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.ai.data.model.ChatSettings
import com.antigravity.ai.data.model.EffortItem
import com.antigravity.ai.data.model.ModelItem
import com.antigravity.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSettingsDialog(
    currentSettings: ChatSettings,
    availableModels: List<ModelItem>,
    availableEfforts: List<EffortItem>,
    onDismiss: () -> Unit,
    onSave: (ChatSettings) -> Unit
) {
    var selectedModel by remember { mutableStateOf(currentSettings.model) }
    var selectedEffort by remember { mutableStateOf(currentSettings.effort) }
    var selectedMode by remember { mutableStateOf(currentSettings.mode) }
    var useVault by remember { mutableStateOf(currentSettings.useVault) }

    val modelsList = if (availableModels.isNotEmpty()) availableModels else listOf(
        ModelItem("gemini-3.7-flash-high", "Gemini 3.7 Flash (High)", "Yüksek akıl yürütme"),
        ModelItem("gemini-3.7-flash-medium", "Gemini 3.7 Flash (Medium)", "Dengeli standart model"),
        ModelItem("gemini-3.7-flash-low", "Gemini 3.7 Flash (Low)", "Hızlı yanıt"),
        ModelItem("gemini-3.6-flash-high", "Gemini 3.6 Flash (High)", "Hızlı analitik"),
        ModelItem("gemini-3.1-pro-high", "Gemini 3.1 Pro (High)", "Derin mimari"),
        ModelItem("claude-sonnet-4-6", "Claude Sonnet 4.6 (Thinking)", "Gelişmiş düşünme"),
        ModelItem("claude-opus-4-6-thinking", "Claude Opus 4.6 (Thinking)", "Maksimum kapasite")
    )

    val effortsList = if (availableEfforts.isNotEmpty()) availableEfforts else listOf(
        EffortItem("default", "Varsayılan", "Standart"),
        EffortItem("low", "Düşük", "Hızlı"),
        EffortItem("medium", "Orta", "Dengeli"),
        EffortItem("high", "Yüksek", "Derin")
    )

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
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Model ve Yürütme Ayarları",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = TextPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Kapat", tint = TextMuted)
                }
            }

            Divider(color = BorderSubtle, modifier = Modifier.padding(vertical = 12.dp))

            // 1. Dynamic Models from CLI (`agy models`)
            Text(
                text = "ANTIGRAVITY CLI MODELLERİ (${modelsList.size} Model)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                modelsList.forEach { modelItem ->
                    val isSelected = selectedModel == modelItem.id
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) SurfaceVariantDark else Color.Transparent)
                            .border(1.dp, if (isSelected) PrimaryIndigo else BorderSubtle, RoundedCornerShape(10.dp))
                            .clickable { selectedModel = modelItem.id }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = modelItem.name,
                                fontSize = 13.sp,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                            if (modelItem.description.isNotEmpty()) {
                                Text(
                                    text = modelItem.description,
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }
                        }
                        if (isSelected) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Dynamic Reasoning Effort
            Text(
                text = "DÜŞÜNME AĞIRLIĞI (REASONING EFFORT)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                effortsList.forEach { effortItem ->
                    val isSelected = selectedEffort == effortItem.id
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) PrimaryIndigo else SurfaceVariantDark,
                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedEffort = effortItem.id }
                    ) {
                        Text(
                            text = effortItem.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) Color.White else TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. AGY Vault Entegrasyonu Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceVariantDark)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "AGY Vault Entegrasyonu", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                    Text(text = "/storage/emulated/0/Documents/AGY-Vault hafızası", fontSize = 11.sp, color = TextMuted)
                }
                Switch(
                    checked = useVault,
                    onCheckedChange = { useVault = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryIndigo)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Kaydet Button
            Button(
                onClick = {
                    onSave(
                        currentSettings.copy(
                            model = selectedModel,
                            effort = selectedEffort,
                            mode = selectedMode,
                            useVault = useVault
                        )
                    )
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
            ) {
                Text(text = "Ayarları Kaydet ve Uygula", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
