package com.antigravity.ai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.ai.ui.theme.*

@Composable
fun InteractiveChoiceCard(
    question: String,
    options: List<String>,
    onSelectOption: (String) -> Unit,
    onCustomAnswerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, GeminiBlue.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = null,
                    tint = GeminiBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = question.ifEmpty { "Lütfen bir seçenek belirleyin:" },
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    fontSize = 13.5.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            options.forEach { opt ->
                OutlinedButton(
                    onClick = { onSelectOption(opt) },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderSubtle),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = BackgroundDark,
                        contentColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Text(
                        text = opt,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = GeminiBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onCustomAnswerClick,
                    colors = ButtonDefaults.textButtonColors(contentColor = SecondaryPurple)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = SecondaryPurple
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "✍️ Yazarak Yanıtla",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun InteractiveChecklistCard(
    title: String,
    items: List<String>,
    onSendSelected: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedItems = remember { mutableStateListOf<String>() }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Checklist,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title.ifEmpty { "Seçenekler / Görev Listesi" },
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 13.5.sp
                    )
                }

                if (items.size > 1) {
                    TextButton(
                        onClick = {
                            if (selectedItems.size == items.size) {
                                selectedItems.clear()
                            } else {
                                selectedItems.clear()
                                selectedItems.addAll(items)
                            }
                        }
                    ) {
                        Text(
                            text = if (selectedItems.size == items.size) "Seçimi Kaldır" else "Tümünü Seç",
                            fontSize = 11.sp,
                            color = PrimaryIndigo
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            items.forEach { item ->
                val isChecked = selectedItems.contains(item)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isChecked) GeminiBlue.copy(alpha = 0.08f) else Color.Transparent)
                        .clickable {
                            if (isChecked) selectedItems.remove(item) else selectedItems.add(item)
                        }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { checked ->
                            if (checked) selectedItems.add(item) else selectedItems.remove(item)
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = GeminiBlue,
                            uncheckedColor = TextMuted
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item,
                        color = if (isChecked) TextPrimary else TextMuted,
                        fontSize = 13.sp,
                        fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    if (selectedItems.isNotEmpty()) {
                        onSendSelected(selectedItems.toList())
                    }
                },
                enabled = selectedItems.isNotEmpty(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GeminiBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Seçilenleri Gönder (${selectedItems.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
