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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
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
    onSubmitToken: (String) -> Unit,
    isSubmitting: Boolean = false,
    error: String? = null,
    onStartAgyLogin: () -> Unit = {},
    onAgyCodeSubmit: (String) -> Unit = {},
    isAgyAuthLoading: Boolean = false,
    agyAuthError: String? = null,
    isAgyWaitingCode: Boolean = false
) {
    var tokenText by remember { mutableStateOf("") }
    var agyCodeText by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("agy") } // "agy" | "token"
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val focusRequester = remember { FocusRequester() }

    fun readClipboard(): String? = try {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
    } catch (e: Exception) {
        null
    }

    fun pasteInto(target: (String) -> Unit) {
        val clip = readClipboard()
        if (!clip.isNullOrBlank()) {
            target(clip)
            Toast.makeText(context, "Panodan yapıştırıldı", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Panoda metin yok", Toast.LENGTH_SHORT).show()
        }
    }

    // Açılışta: tarayıcıyı otomatik aç + panodaki kodu alana doldur + odakla.
    var initialized by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
            if (!initialized) {
                initialized = true
                if (mode == "agy") onStartAgyLogin()
                val clip = readClipboard()
            if (!clip.isNullOrBlank()) {
                if (clip.startsWith("4/")) {
                    agyCodeText = clip
                } else {
                    tokenText = clip
                }
            }
            focusRequester.requestFocus()
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
                            text = if (isAuthenticated) "Antigravity hesabı etkin ve istekler imzalanıyor." else "Aşağıdaki yöntemlerden biriyle giriş yapın.",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceVariantDark)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ModeTab(
                    label = "Google ile Giriş",
                    selected = mode == "agy",
                    onClick = { mode = "agy" },
                    modifier = Modifier.weight(1f)
                )
                ModeTab(
                    label = "Token Yapıştır",
                    selected = mode == "token",
                    onClick = { mode = "token" },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (mode == "agy") {
                // --- agy native OAuth flow (TUI gibi) ---
                Text(
                    text = "1. ADIM: GOOGLE İLE BAŞLAT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GeminiBlue
                )
                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = { onStartAgyLogin() },
                    enabled = !isAgyAuthLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    if (isAgyAuthLoading && !isAgyWaitingCode) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = GeminiBlue, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Giriş başlatılıyor...", color = TextPrimary, fontSize = 13.sp)
                    } else {
                        Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, tint = GeminiBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Tarayıcıda Google Girişini Aç", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "2. ADIM: DÖNEN KODU YAPIŞTIR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GeminiBlue
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (isAgyWaitingCode) {
                    Text(
                        text = "Tarayıcıda girişi tamamlayın. Google size bir yetkilendirme KODU (4/... ile başlar) verecek; onu aşağıya yapıştırıp Gönder'e basın. agy gerisini hallediyor.",
                        fontSize = 11.sp,
                        color = TextMuted,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Yetkilendirme kodu (4/...):", fontSize = 11.sp, color = TextMuted)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { pasteInto { agyCodeText = it } }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, tint = GeminiBlue, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Panodan Yapıştır", fontSize = 11.sp, color = GeminiBlue, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = agyCodeText,
                    onValueChange = { agyCodeText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    ),
                    label = { Text("4/... ile başlayan kodu yapıştırın", color = TextMuted, fontSize = 11.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = InputBackground,
                        unfocusedContainerColor = InputBackground,
                        disabledContainerColor = InputBackground,
                        focusedIndicatorColor = GeminiBlue,
                        unfocusedIndicatorColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = GeminiBlue
                    ),
                    minLines = 2,
                    maxLines = 4
                )

                if (agyAuthError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF2C1E1B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5484D).copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Outlined.Lock, contentDescription = null, tint = Color(0xFFE5484D), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = agyAuthError, color = Color(0xFFFFB4B4), fontSize = 11.sp, lineHeight = 16.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { if (agyCodeText.isNotBlank() && !isAgyAuthLoading) onAgyCodeSubmit(agyCodeText.trim()) },
                    enabled = agyCodeText.isNotBlank() && !isAgyAuthLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    if (isAgyAuthLoading && isAgyWaitingCode) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Token alınıyor...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    } else {
                        Icon(imageVector = Icons.Outlined.VpnKey, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Kodu Gönder ve Giriş Yap", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            } else {
                // --- Manual token paste ---
                Text(
                    text = "MANUEL TOKEN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GeminiBlue
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "OAuth token'ı yapıştırın:", fontSize = 11.sp, color = TextMuted)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { pasteInto { tokenText = it } }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, tint = GeminiBlue, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Panodan Yapıştır", fontSize = 11.sp, color = GeminiBlue, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = tokenText,
                    onValueChange = { tokenText = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    ),
                    label = { Text("ya29... ile başlayan erişim token'ı", color = TextMuted, fontSize = 11.sp) },
                    placeholder = { Text("Gerçek erişim TOKEN'ını (uzun string) buraya yapıştırın...", color = TextMuted, fontSize = 11.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = InputBackground,
                        unfocusedContainerColor = InputBackground,
                        disabledContainerColor = InputBackground,
                        focusedIndicatorColor = GeminiBlue,
                        unfocusedIndicatorColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = GeminiBlue
                    ),
                    minLines = 3,
                    maxLines = 6,
                    isError = error != null
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF2C1E1B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5484D).copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Outlined.Lock, contentDescription = null, tint = Color(0xFFE5484D), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = error, color = Color(0xFFFFB4B4), fontSize = 11.sp, lineHeight = 16.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { if (tokenText.isNotBlank() && !isSubmitting) onSubmitToken(tokenText.trim()) },
                    enabled = tokenText.isNotBlank() && !isSubmitting,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Doğrulanıyor...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    } else {
                        Icon(imageVector = Icons.Outlined.VpnKey, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Tokenı Kaydet", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            if (agyAuthError != null && !isAgyWaitingCode) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { onStartAgyLogin() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = GeminiBlue, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Yeniden dene", fontSize = 12.sp, color = GeminiBlue)
                }
            }
        }
    }
}

@Composable
private fun ModeTab(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) GeminiBlue.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(onClick = onClick)
            .border(
                if (selected) androidx.compose.foundation.BorderStroke(1.dp, GeminiBlue) else androidx.compose.foundation.BorderStroke(0.dp, Color.Transparent),
                RoundedCornerShape(8.dp)
            )
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) GeminiBlue else TextMuted
        )
    }
}
