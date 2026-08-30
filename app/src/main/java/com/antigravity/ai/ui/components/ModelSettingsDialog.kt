package com.antigravity.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.ai.data.api.ServerHealth
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
    onSave: (ChatSettings) -> Unit,
    onOpenAuthDialog: () -> Unit = {},
    currentBackend: String = "agy",
    onBackendChange: (String) -> Unit = {},
    onNotificationToggle: (Boolean) -> Unit = {},
    serverHealth: ServerHealth? = null,
    isCheckingHealth: Boolean = false,
    onCheckHealth: () -> Unit = {},
    onStartServer: () -> Unit = {},
    onStopServer: () -> Unit = {},
    onRestartServer: () -> Unit = {},
    isKeepAliveRunning: Boolean = false,
    keepAliveMode: String = "invisible",
    onToggleKeepAlive: (Boolean, String) -> Unit = { _, _ -> },
    onExportAllConversations: (() -> Unit)? = null
) {
    var selectedBackend by remember { mutableStateOf(currentBackend) }
    var selectedModel by remember { mutableStateOf(currentSettings.model) }
    var selectedEffort by remember { mutableStateOf(currentSettings.effort) }
    var selectedMode by remember { mutableStateOf(currentSettings.mode) }
    var useVault by remember { mutableStateOf(currentSettings.useVault) }
    var currentFontSize by remember { mutableStateOf(currentSettings.fontSizeSp) }
    var thermalMode by remember { mutableStateOf(currentSettings.thermalMode) }
    var notificationsEnabled by remember { mutableStateOf(currentSettings.notificationsEnabled) }

    val modelsList = if (availableModels.isNotEmpty()) availableModels else listOf(
        ModelItem("gemini-3.7-flash-medium", "Gemini 3.7 Flash (Medium)", "Dengeli ve hızlı standart model"),
        ModelItem("gemini-3.7-flash-high", "Gemini 3.7 Flash (High)", "Yüksek akıl yürütme ve analitik"),
        ModelItem("gemini-3.7-flash-low", "Gemini 3.7 Flash (Low)", "Minimum düşünme gecikmesi"),
        ModelItem("gemini-3.6-flash-high", "Gemini 3.6 Flash (High)", "Hızlı analitik model"),
        ModelItem("gemini-3.1-pro-high", "Gemini 3.1 Pro (High)", "Derin mimari ve kodlama"),
        ModelItem("claude-sonnet-4-6", "Claude Sonnet 4.6 (Thinking)", "Gelişmiş analitik düşünme"),
        ModelItem("claude-opus-4-6-thinking", "Claude Opus 4.6 (Thinking)", "Maksimum kapasiteli model")
    )

    val effortsList = if (availableEfforts.isNotEmpty()) availableEfforts else listOf(
        EffortItem("default", "Varsayılan", "Standart"),
        EffortItem("low", "Düşük", "Hızlı yanıt"),
        EffortItem("medium", "Orta", "Dengeli"),
        EffortItem("high", "Yüksek", "Derin analiz")
    )

    val fontPresets = listOf(
        Pair("Kompakt", 11.5f),
        Pair("Küçük", 13.5f),
        Pair("Orta", 15.0f),
        Pair("Büyük", 16.5f)
    )

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
            // 1. Header (Gemini Style)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GeminiSparkleIcon(size = 22.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Gemini & Sistem Ayarları",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Kapat", tint = TextMuted)
                }
            }

            Divider(color = BorderSubtle, modifier = Modifier.padding(vertical = 12.dp))

            // 0. BACKEND SEÇİMİ (agy CLI <-> opencode)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.SmartToy,
                    contentDescription = null,
                    tint = GeminiBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ARKA UÇ (BACKEND)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GeminiBlue
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val backends = listOf(
                    "auto" to "Otomatik",
                    "agy" to "AGY CLI",
                    "opencode" to "OpenCode"
                )
                backends.forEach { (id, label) ->
                    val isSel = selectedBackend == id
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSel) GeminiBlue else SurfaceVariantDark,
                        border = if (isSel) null else androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedBackend = id
                                onBackendChange(id)
                            }
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSel) Color.White else TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(vertical = 9.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. FONT VE METİN BOYUTU AYARI (User Request)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.FormatSize,
                    contentDescription = null,
                    tint = GeminiBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "METİN VE FONT BOYUTU",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GeminiBlue
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Live Preview Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceVariantDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Canlı Önizleme (${String.format("%.1f", currentFontSize)} sp):",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Antigravity Termux ortamında %50 CPU ile optimize çalışıyor.",
                        fontSize = currentFontSize.sp,
                        lineHeight = (currentFontSize * 1.4f).sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$ agy-limit 50 --continue",
                        fontFamily = FontFamily.Monospace,
                        fontSize = (currentFontSize * 0.9f).sp,
                        color = Color(0xFF93B4FC)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Font Preset Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                fontPresets.forEach { (label, size) ->
                    val isSelected = Math.abs(currentFontSize - size) < 0.5f
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) PrimaryIndigo else SurfaceVariantDark,
                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { currentFontSize = size }
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            // Fine-tuning Slider
            Slider(
                value = currentFontSize,
                onValueChange = { currentFontSize = it },
                valueRange = 10.5f..18.0f,
                steps = 15,
                colors = SliderDefaults.colors(
                    thumbColor = PrimaryIndigo,
                    activeTrackColor = PrimaryIndigo,
                    inactiveTrackColor = BorderSubtle
                ),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. TERMAL VE CPU KORUMA MODU
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.LocalFireDepartment,
                    contentDescription = null,
                    tint = GeminiAmber,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "TERMAL VE İŞLEMCİ (CPU) KORUMASI",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GeminiAmber
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isEco = thermalMode == "eco"
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isEco) Color(0xFF1B3B2B) else SurfaceVariantDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isEco) SuccessGreen else BorderSubtle),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { thermalMode = "eco" }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "❄️ %50 Sınır (Eko)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isEco) SuccessGreen else TextPrimary
                        )
                        Text(
                            text = "Aşırı ısınmayı önler",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }

                val isPerf = thermalMode == "performance"
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isPerf) Color(0xFF3E2D1D) else SurfaceVariantDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isPerf) GeminiAmber else BorderSubtle),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { thermalMode = "performance" }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "⚡ Tam Güç",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPerf) GeminiAmber else TextPrimary
                        )
                        Text(
                            text = "Sınırsız CPU modu",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 4. MODEL SEÇİMİ (CLI Modelleri)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.SmartToy,
                    contentDescription = null,
                    tint = PrimaryIndigo,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "YAPAY ZEKA MODELİ (${modelsList.size} Model)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryIndigo
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = PrimaryIndigo,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 5. DÜŞÜNME AĞIRLIĞI (REASONING EFFORT)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Psychology,
                    contentDescription = null,
                    tint = GeminiPurple,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "DÜŞÜNME SEVİYESİ (REASONING EFFORT)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GeminiPurple
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                effortsList.forEach { effortItem ->
                    val isSelected = selectedEffort == effortItem.id
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) GeminiPurple else SurfaceVariantDark,
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
                            modifier = Modifier.padding(vertical = 9.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 6. AGY VAULT HAFIZA ENTEGRASYONU
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceVariantDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Outlined.Storage, contentDescription = null, tint = GeminiBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "AGY Vault Hafızası", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                        }
                        Text(text = "Not ve doküman hafızasını sohbete dahil eder", fontSize = 10.sp, color = TextMuted)
                    }
                    Switch(
                        checked = useVault,
                        onCheckedChange = { useVault = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = GeminiBlue)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 6.5 BİLDİRİMLER
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceVariantDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Outlined.Notifications, contentDescription = null, tint = GeminiBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Bildirimler", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                        }
                        Text(text = "Üretim bitince / hata olunca yerel bildirim gönder", fontSize = 10.sp, color = TextMuted)
                    }
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = {
                            notificationsEnabled = it
                            onNotificationToggle(it)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = GeminiBlue)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 7. GOOGLE HESAP VE TOKEN GİRİŞİ (User Request)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceVariantDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = GeminiAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Oturum & Token Yönetimi", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                        }
                        Text(text = "Google OAuth yetkilendirme kodu veya token yapıştırın", fontSize = 10.sp, color = TextMuted)
                    }

                    OutlinedButton(
                        onClick = onOpenAuthDialog,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GeminiAmber),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GeminiAmber.copy(alpha = 0.6f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(text = "Token Gir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 8. TERMUX AGY-WEB SUNUCU VE DONDURMA KORUMASI (Keep-Alive & Server Manager)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceVariantDark,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (serverHealth?.isOnline == true) SuccessGreen.copy(alpha = 0.5f) else DangerRed.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (serverHealth?.isOnline == true) SuccessGreen else DangerRed)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Termux agy-web Sunucusu",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = TextPrimary
                            )
                        }

                        Text(
                            text = if (serverHealth?.isOnline == true) "🟢 ÇALIŞIYOR (${serverHealth.latencyMs}ms)" else "🔴 DURDURULDU",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (serverHealth?.isOnline == true) SuccessGreen else DangerRed
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (serverHealth?.isOnline == true) {
                            "PID: ${serverHealth.pid} • Uptime: ${serverHealth.uptimeSeconds / 60}dk • Port: 8080"
                        } else {
                            "Sunucu kapalı. Dosya yöneticisi ve CLI için başlatın."
                        },
                        fontSize = 11.sp,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action buttons (Start / Restart / Stop)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onStartServer,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f).height(34.dp)
                        ) {
                            Text("⚡ Başlat", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        OutlinedButton(
                            onClick = onRestartServer,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GeminiBlue),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GeminiBlue),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f).height(34.dp)
                        ) {
                            Text("🔄 Yeniden", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = onStopServer,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.6f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f).height(34.dp)
                        ) {
                            Text("🛑 Durdur", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Divider(color = BorderSubtle, modifier = Modifier.padding(vertical = 10.dp))

                    // Floating Keep-Alive Switch (Arka Plan Dondurma Koruması)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🪟 Arka Plan Dondurma Koruması (Yüzen Pencere)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Termux ve Node.js arka planda dondurulmadan sürekli çalışır",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }

                        Switch(
                            checked = isKeepAliveRunning,
                            onCheckedChange = { onToggleKeepAlive(it, keepAliveMode) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SuccessGreen)
                        )
                    }

                    if (isKeepAliveRunning) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isInv = keepAliveMode == "invisible"
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isInv) GeminiBlue.copy(alpha = 0.2f) else SurfaceDark,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isInv) GeminiBlue else BorderSubtle),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onToggleKeepAlive(true, "invisible") }
                            ) {
                                Text(
                                    text = "👻 Görünmez (1x1 px)",
                                    fontSize = 11.sp,
                                    fontWeight = if (isInv) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isInv) GeminiBlue else TextPrimary,
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (!isInv) GeminiPurple.copy(alpha = 0.2f) else SurfaceDark,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (!isInv) GeminiPurple else BorderSubtle),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onToggleKeepAlive(true, "pill") }
                            ) {
                                Text(
                                    text = "⚡ Yüzen Rozet",
                                    fontSize = 11.sp,
                                    fontWeight = if (!isInv) FontWeight.Bold else FontWeight.Normal,
                                    color = if (!isInv) GeminiPurple else TextPrimary,
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp)
                                )
                            }
                        }
                    }
                }

                if (onExportAllConversations != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceVariantDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onExportAllConversations() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FileDownload,
                                contentDescription = null,
                                tint = GeminiPurple,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tüm Sohbetleri Dışa Aktar (Markdown Arşivi)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Kaydet Butonu
            Button(
                onClick = {
                    onSave(
                        currentSettings.copy(
                            model = selectedModel,
                            effort = selectedEffort,
                            mode = selectedMode,
                            useVault = useVault,
                            fontSizeSp = currentFontSize,
                            thermalMode = thermalMode,
                            notificationsEnabled = notificationsEnabled
                        )
                    )
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
            ) {
                Text(text = "Ayarları Kaydet ve Uygula", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
