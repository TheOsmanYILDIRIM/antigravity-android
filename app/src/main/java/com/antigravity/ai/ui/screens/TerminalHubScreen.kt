package com.antigravity.ai.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.antigravity.ai.ui.theme.SurfaceDark
import com.antigravity.ai.ui.theme.TextMuted

@Composable
fun TerminalHubScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Terminal Hub", style = MaterialTheme.typography.headlineMedium)
            Text("Yerel servisler ve görevler", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
        }
        item { HubCard("Servis durumu", listOf("AGY" to "N/A", "Codex" to "N/A", "OpenCode" to "N/A")) }
        item { HubCard("Favori kısayollar", listOf("Yeni terminal oturumu" to "Hazır değil", "Projeleri aç" to "Hazır değil")) }
        item { HubCard("Yaklaşan zamanlamalar", listOf("Zamanlama bulunamadı" to "N/A")) }
        item { HubCard("Çalışan görevler", listOf("Çalışan görev yok" to "N/A")) }
        item { HubCard("Kaynaklar", listOf("CPU / RAM" to "N/A", "Arka plan işlemleri" to "N/A")) }
    }
}

@Composable
private fun HubCard(title: String, rows: List<Pair<String, String>>) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            rows.forEach { (label, value) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, color = TextMuted)
                    Text(value, color = TextMuted)
                }
            }
        }
    }
}
