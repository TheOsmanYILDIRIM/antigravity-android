package com.antigravity.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.ai.data.model.SkillItem
import com.antigravity.ai.data.model.SlashCommand
import com.antigravity.ai.ui.theme.*

val CoreSlashCommands = listOf(
    SlashCommand("/goal", "Uzun süreli otonom görev modunu etkinleştirir", "/goal <hedef>"),
    SlashCommand("/plan", "Adım adım derin mimari planlama yapar", "/plan <görev>"),
    SlashCommand("/schedule", "Zamanlanmış görev veya hatırlatıcı ayarlar", "/schedule <zaman>"),
    SlashCommand("/grill-me", "Gereksinimleri netleştirmek için mülakat yapar", "/grill-me"),
    SlashCommand("/teamwork-preview", "Çoklu otonom alt ajan takımı simülasyonu", "/teamwork-preview"),
    SlashCommand("/boost", "Derin analiz ve çoklu bakış açısı modu", "/boost <konu>"),
    SlashCommand("/learn", "Yeni kural veya davranışı hafızaya kaydeder", "/learn <kural>"),
    SlashCommand("/status", "Arka plan ve sunucu durumunu görüntüler", "/status"),
    SlashCommand("/clear", "Sohbet ekranını ve geçmişini temizler", "/clear"),
    SlashCommand("/help", "Kullanılabilir tüm agy komutlarını listeler", "/help")
)

@Composable
fun SlashCommandPopup(
    query: String,
    installedSkills: List<SkillItem>,
    onSelect: (SlashCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    val skillsAsCommands = installedSkills.map {
        SlashCommand(
            command = it.command,
            description = it.description,
            example = it.command,
            isSkill = true
        )
    }

    val allCommands = CoreSlashCommands + skillsAsCommands

    val filtered = allCommands.filter {
        it.command.startsWith(query, ignoreCase = true) || it.description.contains(query.removePrefix("/"), ignoreCase = true)
    }

    if (filtered.isNotEmpty()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                .background(SurfaceDark)
                .heightIn(max = 240.dp)
        ) {
            Text(
                text = "KOMUTLAR VE SKILL'LER (/SLASH & /SKILL)",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(filtered) { cmd ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(cmd) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = cmd.command,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (cmd.isSkill) SecondaryPurple else PrimaryIndigo
                                )
                                if (cmd.isSkill) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0x338B5CF6))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(text = "SKILL", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = SecondaryPurple)
                                    }
                                }
                            }
                            Text(
                                text = cmd.description,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = cmd.example,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}
