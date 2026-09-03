package com.antigravity.ai.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
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
    onDismiss: () -> Unit,
    onStartAgyLogin: () -> Unit = {},
    onAgyCodeSubmit: (String) -> Unit = {},
    isAgyAuthLoading: Boolean = false,
    agyAuthError: String? = null,
    isAgyWaitingCode: Boolean = false,
    agyAuthUrl: String? = null
) {
    val clipboardManager = LocalClipboardManager.current
    var inputCode by remember { mutableStateOf("") }

    // Auto-detect if clipboard already contains an authorization code (starts with 4/)
    LaunchedEffect(isAgyWaitingCode) {
        if (inputCode.isBlank()) {
            val clipText = clipboardManager.getText()?.text?.trim() ?: ""
            if (clipText.startsWith("4/") || clipText.contains("code=4/")) {
                inputCode = clipText
            }
        }
    }

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
                        text = "Google Antigravity Girişi",
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
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isAuthenticated) SuccessGreen.copy(alpha = 0.5f) else WarningAmber.copy(alpha = 0.5f)
                ),
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
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isAuthenticated) "Oturum Açık (Doğrulandı)" else "Kimlik Doğrulaması Gerekli",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isAuthenticated) SuccessGreen else WarningAmber
                        )
                        Text(
                            text = if (isAuthenticated)
                                "Antigravity OAuth jetonu etkin, arka planda otomatik yenileniyor."
                            else
                                "Google hesabınızla yetkilendirme yaparak hemen bağlanabilirsiniz.",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            // Error display if any
            if (!agyAuthError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF331414),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF882222)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = agyAuthError,
                            fontSize = 12.sp,
                            color = Color(0xFFFFD1D1),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step 1: Google Login Button
            Text(
                text = "1. ADIM: GOOGLE HESABI İLE YETKİLENDİR",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = GeminiBlue
            )
            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = onStartAgyLogin,
                enabled = !isAgyAuthLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                if (isAgyAuthLoading && !isAgyWaitingCode) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Giriş Başlatılıyor...", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                } else {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAuthenticated) "Google ile Tekrar Giriş Başlat" else "Tarayıcıda Google Girişini Aç",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Step 2: Code input
            Text(
                text = "2. ADIM: YETKİLENDİRME KODUNU GİRİN",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isAgyWaitingCode || inputCode.isNotBlank()) GeminiBlue else TextMuted
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = inputCode,
                onValueChange = { inputCode = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "4/0A... ile başlayan kodu buraya yapıştırın",
                        color = TextMuted.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                },
                singleLine = false,
                maxLines = 3,
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = TextPrimary
                ),
                trailingIcon = {
                    IconButton(onClick = {
                        val clip = clipboardManager.getText()?.text?.trim()
                        if (!clip.isNullOrBlank()) {
                            inputCode = clip
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Panodan Yapıştır",
                            tint = GeminiBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GeminiBlue,
                    unfocusedBorderColor = BorderSubtle,
                    focusedContainerColor = Color(0xFF0E1116),
                    unfocusedContainerColor = Color(0xFF0E1116)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Submit Button
            Button(
                onClick = {
                    val codeToSubmit = inputCode.trim()
                    if (codeToSubmit.isNotBlank()) {
                        onAgyCodeSubmit(codeToSubmit)
                    }
                },
                enabled = inputCode.isNotBlank() && !isAgyAuthLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GeminiBlue,
                    disabledContainerColor = GeminiBlue.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                if (isAgyAuthLoading && isAgyWaitingCode) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Oturum Doğrulanıyor...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Girişi Tamamla", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Terminal fallback hint
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
                    Icon(imageVector = Icons.Default.Terminal, contentDescription = null, tint = GeminiBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Terminal alternatifi: 'agy' komutunu terminalde de çalıştırabilirsiniz.",
                        fontSize = 11.sp,
                        color = TextMuted,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}
