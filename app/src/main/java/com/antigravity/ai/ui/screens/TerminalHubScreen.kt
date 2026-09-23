package com.antigravity.ai.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.antigravity.ai.ui.theme.SurfaceDark
import com.antigravity.ai.ui.theme.TextMuted
import com.antigravity.ai.data.api.ServerHealth
import com.antigravity.ai.data.api.AntigravityApiService
import com.antigravity.ai.data.api.StreamEvent
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.antigravity.ai.data.model.TerminalPlugin
import com.antigravity.ai.data.model.TerminalSchedule
import com.antigravity.ai.data.model.TerminalTask
import com.antigravity.ai.data.model.ActionItem
import com.antigravity.ai.service.TerminalScheduleReceiver
import com.antigravity.ai.service.TerminalScheduleManager

@Composable
fun TerminalHubScreen(agyHealth: ServerHealth? = null, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val api = remember { AntigravityApiService() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var actions by remember { mutableStateOf(emptyList<com.antigravity.ai.data.model.ActionItem>()) }
    var output by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Sunucu bağlantısı bekleniyor") }
    var tasks by remember { mutableStateOf<List<TerminalTask>?>(null) }
    var plugins by remember { mutableStateOf<List<TerminalPlugin>?>(null) }
    var schedules by remember { mutableStateOf<List<TerminalSchedule>?>(null) }
    var scheduleAction by remember { mutableStateOf("agy-start") }
    var scheduleMinutes by remember { mutableStateOf("5") }
    var scheduleMenuOpen by remember { mutableStateOf(false) }
    var scheduleStatus by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        api.getActions().onSuccess { actions = it.actions; status = "Hazır" }.onFailure { status = "Sunucu kapalı / N/A" }
        api.getTerminalTasks().onSuccess { tasks = it.tasks }
        api.getTerminalPlugins().onSuccess { plugins = it.plugins }
        api.getTerminalSchedules().onSuccess { schedules = it.schedules }
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
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Yeni zamanlama", style = MaterialTheme.typography.titleMedium)
                    Text("Yalnızca güvenli manifest action'ları çalıştırır.", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                    androidx.compose.foundation.layout.Box {
                        Button(onClick = { scheduleMenuOpen = true }) { Text(scheduleAction) }
                        DropdownMenu(expanded = scheduleMenuOpen, onDismissRequest = { scheduleMenuOpen = false }) {
                            TerminalScheduleReceiver.ALLOWED_ACTIONS.forEach { actionId ->
                                DropdownMenuItem(text = { Text(actionId) }, onClick = { scheduleAction = actionId; scheduleMenuOpen = false })
                            }
                        }
                    }
                    OutlinedTextField(value = scheduleMinutes, onValueChange = { scheduleMinutes = it.filter(Char::isDigit).take(4) }, label = { Text("Kaç dakika sonra") })
                    Button(onClick = {
                        val minutes = scheduleMinutes.toLongOrNull()
                        if (minutes == null || minutes < 1L) {
                            scheduleStatus = "En az 1 dakika girin"
                        } else {
                            val triggerAt = System.currentTimeMillis() + minutes * 60_000L
                            scope.launch {
                                api.createTerminalSchedule(scheduleAction, triggerAt).onSuccess {
                                    if (TerminalScheduleManager.schedule(context, it.id ?: "$scheduleAction-$triggerAt", triggerAt, scheduleAction)) {
                                        schedules = schedules.orEmpty() + it
                                        scheduleStatus = "Zamanlandı"
                                    } else scheduleStatus = "Alarm kurulamadı"
                                }.onFailure { scheduleStatus = "Zamanlama hatası: ${it.message ?: "N/A"}" }
                            }
                        }
                    }) { Text("Zamanla") }
                    scheduleStatus?.let { Text(it, color = TextMuted) }
                }
            }
        }
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
                    actions.groupBy { action -> action.category ?: action.id.substringBefore('-').lowercase() }
                        .toSortedMap()
                        .forEach { (category, categoryActions) ->
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(category, style = MaterialTheme.typography.labelLarge)
                                categoryActions.sortedWith(compareBy<ActionItem> { it.order }.thenBy { it.label })
                                    .chunked(2)
                                    .forEach { rowActions ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            rowActions.forEach { action ->
                                                androidx.compose.material3.Button(
                                                    modifier = Modifier.weight(1f),
                                                    enabled = !busy,
                                                    onClick = {
                                                        scope.launch {
                                                            api.runAction(action.id).onFailure {
                                                                status = "Action hatası: ${it.message ?: "N/A"}"
                                                            }
                                                        }
                                                    }
                                                ) {
                                                    Text(
                                                        action.compactLabel?.ifBlank { null } ?: actionActionLabel(action.label),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                            if (rowActions.size == 1) {
                                                androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                            }
                    }
                    if (output.isNotBlank()) Text(output, color = TextMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            val rows = schedules?.takeIf { it.isNotEmpty() }?.map { schedule ->
                (schedule.label ?: schedule.actionId ?: schedule.id ?: "Zamanlama") to
                    listOfNotNull(schedule.triggerAt?.let { java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it)) }, schedule.nextRunAt, schedule.enabled?.let { if (it) "Açık" else "Kapalı" }).joinToString(" · ").ifBlank { "N/A" }
            } ?: listOf("Zamanlama" to "N/A")
            HubCard("Yaklaşan zamanlamalar", rows)
        }
        item {
            val rows = tasks?.takeIf { it.isNotEmpty() }?.map { task ->
                (task.name ?: "PID ${task.pid ?: "?"}") to listOfNotNull(
                    task.status,
                    task.pid?.let { "PID $it" },
                    task.cpuPercent?.let { "CPU ${it}%" },
                    task.rssBytes?.let { "RSS ${it / (1024 * 1024)} MB" }
                ).joinToString(" · ").ifBlank { "N/A" }
            } ?: listOf("Çalışan görev" to "N/A")
            HubCard("Çalışan görevler", rows)
        }
        item {
            val rows = plugins?.takeIf { it.isNotEmpty() }?.map { plugin ->
                (plugin.name ?: plugin.id ?: "Plugin") to (plugin.actions?.size?.let { "$it action" } ?: "N/A")
            } ?: listOf("Plugin" to "N/A")
            HubCard("Plugin özeti", rows)
        }
        item { HubCard("Kaynaklar", listOf("CPU / RAM" to "N/A", "Arka plan işlemleri" to "N/A")) }
    }
}

private fun actionServiceLabel(serviceId: String): String = when (serviceId) {
    "agy" -> "AGY"
    "codex" -> "Codex"
    "opencode" -> "OpenCode"
    "cline" -> "Cline"
    "vault" -> "Vault"
    else -> serviceId.replaceFirstChar { it.uppercase() }
}

private fun actionActionLabel(label: String): String = when {
    label.contains("başlat", ignoreCase = true) || label.contains("start", ignoreCase = true) -> "Başlat"
    label.contains("durdur", ignoreCase = true) || label.contains("kapat", ignoreCase = true) || label.contains("stop", ignoreCase = true) -> "Durdur"
    label.contains("senkronize", ignoreCase = true) || label.contains("sync", ignoreCase = true) -> "Senkronize et"
    label.contains("listele", ignoreCase = true) || label.contains("list", ignoreCase = true) -> "Listele"
    label.contains("durum", ignoreCase = true) || label.contains("status", ignoreCase = true) -> "Durum"
    label.contains("kota", ignoreCase = true) || label.contains("quota", ignoreCase = true) -> "Kota"
    label.contains("geçmiş", ignoreCase = true) || label.contains("history", ignoreCase = true) -> "Geçmiş"
    label.contains("yenile", ignoreCase = true) || label.contains("refresh", ignoreCase = true) -> "Yenile"
    label.contains("doktor", ignoreCase = true) || label.contains("doctor", ignoreCase = true) -> "Doktor"
    label.contains("süreç", ignoreCase = true) || label.contains("ps", ignoreCase = true) -> "Süreçler"
    label.contains("düzenle", ignoreCase = true) || label.contains("organize", ignoreCase = true) -> "Düzenle"
    label.contains("geri al", ignoreCase = true) || label.contains("rollback", ignoreCase = true) -> "Geri al"
    else -> label
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
