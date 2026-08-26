package com.antigravity.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Storage
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
import com.antigravity.ai.data.model.VaultItem
import com.antigravity.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultBrowserSheet(
    vaultFiles: List<VaultItem>,
    onDismiss: () -> Unit,
    onSelectFile: (VaultItem) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Storage, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = "AGY Vault Hafızası", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                        Text(text = "/storage/emulated/0/Documents/AGY-Vault", fontSize = 10.sp, color = TextMuted)
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Kapat", tint = TextMuted)
                }
            }

            Divider(color = BorderSubtle, modifier = Modifier.padding(vertical = 12.dp))

            if (vaultFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Vault dizininde dosya bulunamadı veya taranıyor…", fontSize = 12.sp, color = TextMuted)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(vaultFiles) { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceVariantDark)
                                .clickable {
                                    onSelectFile(item)
                                    onDismiss()
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                                contentDescription = null,
                                tint = if (item.isDirectory) WarningAmber else PrimaryIndigo,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                                Text(text = item.path, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextMuted, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}
