package com.antigravity.ai.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

data class BackendOption(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val color: Color
)

val AvailableBackends = listOf(
    BackendOption("agy", "AGY", Icons.Outlined.SmartToy, GeminiBlue),
    BackendOption("codex", "Codex", Icons.Outlined.Code, Color(0xFF10A37F)),
    BackendOption("opencode", "OpenCode", Icons.Outlined.Terminal, Color(0xFFFF9800)),
    BackendOption("cline", "Cline", Icons.Outlined.Bolt, Color(0xFFAB47BC))
)

/**
 * Hamburger menünün altında dikey sıralanan, kırmızı çizgiyle açılıp kapanan,
 * hafif saydam arka planlı 4'lü mini backend/ajan geçiş balonları.
 * Hamburger menü (drawer) açıldığında tamamen kaybolur.
 */
@Composable
fun FloatingBackendSwitcher(
    selectedBackend: String,
    onBackendSelected: (String) -> Unit,
    isDrawerOpen: Boolean,
    modifier: Modifier = Modifier
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    AnimatedVisibility(
        visible = !isDrawerOpen,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.padding(start = 12.dp, top = 4.dp)
        ) {
            // Hamburger menünün altındaki kırmızı tetikleyici çizgi
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(bounded = false, radius = 18.dp)
                    ) {
                        isExpanded = !isExpanded
                    }
                    .padding(vertical = 4.dp, horizontal = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 28.dp, height = 3.5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(DangerRed)
                )
            }

            // Aşağı doğru açılan hafif saydam yüzen balonlar
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    AvailableBackends.forEach { option ->
                        val isSelected = option.id == selectedBackend

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) {
                                option.color.copy(alpha = 0.22f)
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
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
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
                                    fontSize = 10.5.sp,
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
