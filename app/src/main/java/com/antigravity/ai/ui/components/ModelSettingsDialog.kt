package com.antigravity.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.antigravity.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSettingsDialog(
    currentSettings: ChatSettings,
    onDismiss: () -> Unit,
    onSave: (ChatSettings) -> Unit
) {
    var selectedModel by remember { mutableStateOf(currentSettings.model) }
    var selectedEffort by remember { mutableStateOf(currentSettings.effort) }
    var selectedMode by remember { mutableStateOf(currentSettings.mode) }
    var useVault by remember { mutableStateOf(currentSettings.useVault) }

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

            // 1. AI Model Seçimi
            Text(
                text = "YAPAY ZEKA MODELİ",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val models = listOf(
                "default" to "Varsayılan (CLI Default)",
                "gemini-3.7-flash" to "Gemini 3.7 Flash (Hızlı & Hibrit)",
                "gemini-2.5-pro" to "Gemini 2.5 Pro (Derin Akıl Yürütme)",
                "gemini-2.5-flash" to "Gemini 2.5 Flash",
                "claude-3-5-sonnet" to "Claude 3.5 Sonnet"
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                models.forEach { (id, label) ->
                    val isSelected = selectedModel == id
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) SurfaceVariantDark else Color.Transparent)
                            .border(1.dp, if (isSelected) PrimaryIndigo else BorderSubtle, RoundedCornerShape(10.dp))
                            .clickable { selectedModel = id }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                        if (isSelected) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Düşünme Ağırlığı (Reasoning Effort)
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
                listOf("low" to "Düşük (Hızlı)", "medium" to "Orta", "high" to "Yüksek (Derin)").forEach { (eff, label) ->
                    val isSelected = selectedEffort == eff
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) PrimaryIndigo else SurfaceVariantDark,
                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedEffort = eff }
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
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
