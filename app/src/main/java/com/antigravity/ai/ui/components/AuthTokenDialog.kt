package com.antigravity.ai.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthTokenDialog(
    isAuthenticated: Boolean,
    onDismiss: () -> Unit
) {
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
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GeminiSparkleIcon(size = 22.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Hesap Girişi",
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

            // Auth Status Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isAuthenticated) Color(0xFF132A1C) else Color(0xFF2C1E1B),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isAuthenticated) SuccessGreen.copy(alpha = 0.5f) else WarningAmber.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isAuthenticated) Icons.Default.CheckCircle else Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = if (isAuthenticated) SuccessGreen else WarningAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isAuthenticated) "Oturum Açık (Doğrulandı)" else "Oturum Açma Gerekli",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isAuthenticated) SuccessGreen else WarningAmber
                        )
                        Text(
                            text = if (isAuthenticated) "Antigravity hesabı etkin ve istekler imzalanıyor." else "Giriş için aşağıdaki adımı izleyin.",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Terminal login instruction (only method)
            Text(
                text = "GİRİŞ İÇİN TERMİNAL KULLAN",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = GeminiBlue
            )
            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF0E1116),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Terminal, contentDescription = null, tint = GeminiBlue, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "agy",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Terminalde (agy'nin kurulu olduğu ortamda) 'agy' komutunu çalıştırın, açılan Google giriş ekranında yetkilendirmeyi tamamlayın. Giriş başarılı olunca bu uygulama otomatik olarak yetkilenecek; pencereyi kapatıp sohbete devam edebilirsiniz. Tekrar giriş istemez.",
                fontSize = 12.sp,
                color = TextMuted,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                Text(text = "Anladım", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
