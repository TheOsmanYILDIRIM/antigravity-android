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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.antigravity.ai.ui.theme.SurfaceDark
import com.antigravity.ai.ui.theme.TextMuted
import com.antigravity.ai.data.api.ServerHealth
import com.antigravity.ai.data.api.AntigravityApiService
import com.antigravity.ai.data.api.StreamEvent
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun TerminalHubScreen(agyHealth: ServerHealth? = null, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val api = remember { AntigravityApiService() }
    val scope = rememberCoroutineScope()
    var actions by remember { mutableStateOf(emptyList<com.antigravity.ai.data.model.ActionItem>()) }
    var output by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Sunucu bağlantısı bekleniyor") }
    LaunchedEffect(Unit) {
        api.getActions().onSuccess { actions = it.actions; status = "Hazır" }.onFailure { status = "Sunucu kapalı / N/A" }
        api.observeEvents().collect { event -> when (event) {
            is StreamEvent.ActionStarted -> { busy = true; status = "Çalışıyor (PID ${event.pid ?: "?"})"; output = "" }
            is StreamEvent.ActionOutput -> output += "[${event.stream}] ${event.line}\n"
            is StreamEvent.ActionFinished -> { busy = false; status = "Bitti (exitCode=${event.exitCode ?: "?"})" }
            else -> Unit
        }}
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Terminal Hub", style = MaterialTheme.typography.headlineMedium)
            Text("Yerel servisler ve görevler", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
        }
        item {
            val agyRows = when {
                agyHealth == null -> listOf("AGY" to "N/A / Ölçülüyor")
                agyHealth.isOnline -> listOf(
                    "AGY" to "Çevrimiçi",
                    "Gecikme" to "${agyHealth.latencyMs} ms",
                    "Uptime" to formatUptime(agyHealth.uptimeSeconds),
                    "PID" to agyHealth.pid.toString()
                )
                else -> listOf("AGY" to "Çevrimdışı", "Hata" to (agyHealth.errorMessage ?: "Bilinmeyen hata"))
            } + listOf("Codex" to "N/A", "OpenCode" to "N/A")
            HubCard("Servis durumu", agyRows)
        }
        item { HubCard("Favori kısayollar", listOf("Yeni terminal oturumu" to "Hazır değil", "Projeleri aç" to "Hazır değil")) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Güvenli Actions", style = MaterialTheme.typography.titleMedium)
                    Text(status, color = TextMuted)
                    actions.forEach { action ->
                        androidx.compose.material3.Button(enabled = !busy, onClick = { scope.launch { api.runAction(action.id).onFailure { status = "Action hatası: ${it.message ?: "N/A"}" } } }) { Text(action.label) }
                    }
                    if (output.isNotBlank()) Text(output, color = TextMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { HubCard("Yaklaşan zamanlamalar", listOf("Zamanlama bulunamadı" to "N/A")) }
        item { HubCard("Çalışan görevler", listOf("Çalışan görev yok" to "N/A")) }
        item { HubCard("Kaynaklar", listOf("CPU / RAM" to "N/A", "Arka plan işlemleri" to "N/A")) }
    }
}

private fun formatUptime(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, remainingSeconds)
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
