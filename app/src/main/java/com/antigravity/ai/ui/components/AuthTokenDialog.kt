package com.antigravity.ai.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
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
    authMethod: String,
    onDismiss: () -> Unit,
    onSubmitToken: (String) -> Unit
) {
    var tokenText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val loginUrl = "https://accounts.google.com/o/oauth2/auth?access_type=offline&client_id=1071006060591-tmhssin2h21lcre235vtolojh4g403ep.apps.googleusercontent.com&code_challenge=8UeT9jf4mnW3ey1FRS1d4z3ebhrJqr-d7ImVENDWxKw&code_challenge_method=S256&prompt=consent&redirect_uri=https%3A%2F%2Fantigravity.google%2Foauth-callback&response_type=code&scope=https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fcloud-platform+https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fuserinfo.email+https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fuserinfo.profile+https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fcclog+https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fexperimentsandconfigs+https%3A%2F%2Fwww.googleapis.com%2Fauth%2Faicode+openid"

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
                        text = "Hesap ve Token Girişi",
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
                            text = if (isAuthenticated) "Antigravity hesabı etkin ve istekler imzalanıyor." else "Token veya yetkilendirme kodunuzu aşağıya yapıştırın.",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step 1: Open Login URL in Browser
            Text(
                text = "1. ADIM: GOOGLE İLE YETKİLENDİR",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = GeminiBlue
            )
            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = {
                    try {
                        uriHandler.openUri(loginUrl)
                    } catch (e: Exception) {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Login URL", loginUrl))
                        Toast.makeText(context, "Bağlantı kopyalandı! Tarayıcıya yapıştırın.", Toast.LENGTH_LONG).show()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, tint = GeminiBlue, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Tarayıcıda Google Girişini Aç", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Step 2: Paste Token / Code
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "2. ADIM: TOKEN VEYA KODU YAPIŞTIR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GeminiBlue
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                            if (clipText.isNotBlank()) {
                                tokenText = clipText.trim()
                                Toast.makeText(context, "Panodan yapıştırıldı", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, tint = GeminiBlue, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Panodan Yapıştır", fontSize = 11.sp, color = GeminiBlue, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Token input area
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = InputBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp, max = 150.dp)
            ) {
                Box(modifier = Modifier.padding(12.dp)) {
                    if (tokenText.isEmpty()) {
                        Text(
                            text = "OAuth yetkilendirme kodunu (4/0A...) veya OAuth token JSON'ını buraya yapıştırın...",
                            color = TextMuted,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                    BasicTextField(
                        value = tokenText,
                        onValueChange = { tokenText = it },
                        textStyle = TextStyle(
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        ),
                        cursorBrush = SolidColor(GeminiBlue),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Submit Button
            Button(
                onClick = {
                    if (tokenText.isNotBlank()) {
                        onSubmitToken(tokenText.trim())
                        onDismiss()
                    } else {
                        Toast.makeText(context, "Lütfen önce bir token veya kod yapıştırın", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = tokenText.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(imageVector = Icons.Outlined.VpnKey, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Tokenı Doğrula ve Kaydet", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
