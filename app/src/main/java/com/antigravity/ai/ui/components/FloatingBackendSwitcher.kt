package com.antigravity.ai.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.ai.ui.theme.DangerRed
import com.antigravity.ai.ui.theme.GeminiBlue
import com.antigravity.ai.ui.theme.TextPrimary
import com.antigravity.ai.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

data class BackendOption(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val port: Int
)

val AvailableBackends = listOf(
    BackendOption("agy", "AGY", Icons.Outlined.SmartToy, GeminiBlue, 8080),
    BackendOption("codex", "Codex", Icons.Outlined.Code, Color(0xFF10A37F), 4500),
    BackendOption("opencode", "OpenCode", Icons.Outlined.Terminal, Color(0xFFFF9800), 4096),
    BackendOption("cline", "Cline", Icons.Outlined.Bolt, Color(0xFFAB47BC), 5115)
)

private fun isPortReachable(host: String, port: Int): Boolean = runCatching {
    Socket().use { it.connect(InetSocketAddress(host, port), 400) }
}.isSuccess

/**
 * Hamburger menünün altında dikey sıralanan, 2.5x büyütülmüş kırmızı tetikleyici çizgiyle açılıp kapanan,
 * sadece sunucusu AÇIK olan backend'lerin yüzen balonlarını gösteren mini geçiş çubuğu.
 * Hamburger menü (drawer) açıldığında tamamen kaybolur.
 */
@Composable
fun FloatingBackendSwitcher(
    selectedBackend: String,
    onBackendSelected: (String) -> Unit,
    isDrawerOpen: Boolean,
    modifier: Modifier = Modifier,
    activeBackendsOverride: Set<String>? = null
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    var onlineBackends by remember { mutableStateOf(activeBackendsOverride ?: setOf("agy", "codex", "opencode", "cline")) }

    // Periyodik olarak açık sunucuları denetler, kapalı olan backend isimlerini otomatik gizler
    LaunchedEffect(activeBackendsOverride) {
        if (activeBackendsOverride != null) {
            onlineBackends = activeBackendsOverride
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            while (true) {
                val openSet = mutableSetOf<String>()
                AvailableBackends.forEach { opt ->
                    if (isPortReachable("127.0.0.1", opt.port)) {
                        openSet.add(opt.id)
                    }
                }
                onlineBackends = openSet
                delay(4000)
            }
        }
    }

    AnimatedVisibility(
        visible = !isDrawerOpen,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.padding(start = 10.dp, top = 2.dp)
        ) {
            // 2.5x Büyütülmüş ve basması kolay kırmızı tetikleyici buton çizgisi
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(bounded = false, radius = 24.dp)
                    ) {
                        isExpanded = !isExpanded
                    }
                    .padding(vertical = 8.dp, horizontal = 4.dp) // Genişletilmiş dokunma alanı (hitbox)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 38.dp, height = 8.5.dp) // 2.5x kalınlık ve genişlik
                        .clip(RoundedCornerShape(4.25.dp))
                        .background(DangerRed)
                )
            }

            // Aşağı doğru açılan hafif saydam yüzen balonlar (Sadece sunucusu açık olanlar veya seçili olan görünür)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
            ) {
                val visibleOptions = remember(selectedBackend, onlineBackends) {
                    AvailableBackends.filter { it.id == selectedBackend || onlineBackends.contains(it.id) }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    visibleOptions.forEach { option ->
                        val isSelected = option.id == selectedBackend

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) {
                                option.color.copy(alpha = 0.24f)
                            } else {
                                Color(0xFF14171C).copy(alpha = 0.78f)
                            },
                            border = BorderStroke(
                                width = if (isSelected) 1.2.dp else 0.8.dp,
                                color = if (isSelected) option.color.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.14f)
                            ),
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onBackendSelected(option.id)
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.5.dp)
                            ) {
                                Icon(
                                    imageVector = option.icon,
                                    contentDescription = option.label,
                                    tint = if (isSelected) option.color else TextSecondary,
                                    modifier = Modifier.size(13.dp)
                                )

                                Spacer(Modifier.width(5.dp))

                                Text(
                                    text = option.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) TextPrimary else TextSecondary
                                )

                                if (isSelected) {
                                    Spacer(Modifier.width(5.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(4.5.dp)
                                            .clip(CircleShape)
                                            .background(option.color)
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
