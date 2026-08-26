package com.antigravity.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
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
import com.antigravity.ai.data.model.VaultItem
import com.antigravity.ai.ui.theme.*

@Composable
fun MentionPopup(
    query: String,
    items: List<VaultItem>,
    onSelect: (VaultItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val cleanQuery = query.removePrefix("@")
    val filtered = items.filter {
        it.name.contains(cleanQuery, ignoreCase = true) || it.path.contains(cleanQuery, ignoreCase = true)
    }.take(8)

    if (filtered.isNotEmpty()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                .background(SurfaceDark)
                .heightIn(max = 220.dp)
        ) {
            Text(
                text = "DOSYA VE VAULT ETİKETLERİ (@MENTION)",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(filtered) { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(item) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                            contentDescription = null,
                            tint = if (item.isDirectory) WarningAmber else PrimaryIndigo,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "@" + item.name,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = item.path,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = TextMuted,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
