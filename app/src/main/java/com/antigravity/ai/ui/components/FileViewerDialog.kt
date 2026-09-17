package com.antigravity.ai.ui.components

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.antigravity.ai.data.model.FsContentResponse
import com.antigravity.ai.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

@Composable
fun FileViewerDialog(
    filePath: String,
    contentResponse: FsContentResponse?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSaveFile: (String, String) -> Unit,
    onAttachToChat: (String) -> Unit,
    onMentionInChat: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isEditing by remember { mutableStateOf(false) }
    var editedContent by remember(contentResponse) { mutableStateOf(contentResponse?.content ?: "") }
    val fileName = filePath.substringAfterLast("/")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark),
            color = BackgroundDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                // Top Header Bar
                Surface(
                    color = SurfaceDark,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(SurfaceVariantDark)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Kapat",
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = fileName,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (contentResponse != null) {
                                    "${contentResponse.lineCount} satır • ${formatBytes(contentResponse.size)} • ${filePath.substringBeforeLast("/")}"
                                } else filePath,
                                fontSize = 11.sp,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Compact Top Action Icon Bar (Non-overlapping)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (contentResponse?.isBinary == false) {
                                if (isEditing) {
                                    Button(
                                        onClick = {
                                            onSaveFile(filePath, editedContent)
                                            isEditing = false
                                            Toast.makeText(context, "Kaydedildi!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Kaydet", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                } else {
                                    IconButton(
                                        onClick = { isEditing = true },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(SurfaceVariantDark)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Edit,
                                            contentDescription = "Düzenle",
                                            tint = GeminiBlue,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }

                            // Dışarıda Aç (Open In External App)
                            IconButton(
                                onClick = {
                                    openFileWithExternalApp(context, scope, filePath, fileName, contentResponse?.content)
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceVariantDark)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.OpenInNew,
                                    contentDescription = "Dışarıda Aç",
                                    tint = GeminiBlue,
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            // Storage'a Kopyala / İndir
                            IconButton(
                                onClick = {
                                    copyFileToStorage(context, scope, filePath, fileName, contentResponse?.content)
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceVariantDark)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Download,
                                    contentDescription = "İndir / Storage'a Kaydet",
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            // Dosyayı Paylaş (Share File with Intent)
                            IconButton(
                                onClick = {
                                    shareFileWithIntent(context, scope, filePath, fileName, contentResponse?.content)
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceVariantDark)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = "Dosyayı Paylaş",
                                    tint = GeminiPurple,
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            // İçeriği Kopyala
                            IconButton(
                                onClick = {
                                    val textToCopy = contentResponse?.content ?: filePath
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText(fileName, textToCopy))
                                    Toast.makeText(context, "İçerik panoya kopyalandı", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceVariantDark)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "Metni Kopyala",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }

                // Main Content View
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    if (isLoading) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = GeminiBlue, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Dosya içeriği Termux'tan okunuyor…", fontSize = 13.sp, color = TextMuted)
                        }
                    } else if (contentResponse?.isBinary == true) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.InsertDriveFile,
                                contentDescription = null,
                                tint = GeminiPurple,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "İkili / Binary Dosya",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Bu dosya metin formatında değil (${formatBytes(contentResponse.size)}).",
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { openFileWithExternalApp(context, scope, filePath, fileName, null) },
                                    colors = ButtonDefaults.buttonColors(containerColor = GeminiBlue)
                                ) {
                                    Icon(imageVector = Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Uygulama ile Aç")
                                }
                                OutlinedButton(
                                    onClick = { copyFileToStorage(context, scope, filePath, fileName, null) }
                                ) {
                                    Icon(imageVector = Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("İndirilenler'e Kaydet")
                                }
                            }
                        }
                    } else {
                        if (isEditing) {
                            OutlinedTextField(
                                value = editedContent,
                                onValueChange = { editedContent = it },
                                textStyle = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.5.sp,
                                    color = TextPrimary,
                                    lineHeight = 18.sp
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GeminiBlue,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedContainerColor = SurfaceDark,
                                    unfocusedContainerColor = SurfaceDark
                                ),
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SurfaceDark,
                                border = BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                val vScroll = rememberScrollState()
                                val hScroll = rememberScrollState()

                                SelectionContainer {
                                    Text(
                                        text = contentResponse?.content ?: "",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.5.sp,
                                        lineHeight = 18.sp,
                                        color = TextPrimary,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(vScroll)
                                            .horizontalScroll(hScroll)
                                            .padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Action Footer Bar (Scrollable to prevent any icon overlap on compact screens)
                Surface(
                    color = SurfaceDark,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Sohbete Ekle Button
                        ActionChip(
                            icon = Icons.Default.AddComment,
                            label = "Sohbete Ekle",
                            tint = PrimaryIndigo,
                            onClick = {
                                onAttachToChat(filePath)
                                Toast.makeText(context, "Sohbete eklendi", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        )

                        // Sohbette @Bahset Button
                        ActionChip(
                            icon = Icons.Default.AlternateEmail,
                            label = "@Bahset",
                            tint = GeminiBlue,
                            onClick = {
                                onMentionInChat(filePath)
                                Toast.makeText(context, "Yol prompta eklendi", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        )

                        // Dışarıda Aç
                        ActionChip(
                            icon = Icons.Outlined.OpenInNew,
                            label = "Dışarıda Aç",
                            tint = GeminiBlue,
                            onClick = {
                                openFileWithExternalApp(context, scope, filePath, fileName, contentResponse?.content)
                            }
                        )

                        // Storage'a Kaydet
                        ActionChip(
                            icon = Icons.Outlined.Download,
                            label = "İndirilenler'e Kaydet",
                            tint = SuccessGreen,
                            onClick = {
                                copyFileToStorage(context, scope, filePath, fileName, contentResponse?.content)
                            }
                        )

                        // Dosyayı Paylaş
                        ActionChip(
                            icon = Icons.Outlined.Share,
                            label = "Dosyayı Paylaş",
                            tint = GeminiPurple,
                            onClick = {
                                shareFileWithIntent(context, scope, filePath, fileName, contentResponse?.content)
                            }
                        )

                        // Yolu Kopyala
                        ActionChip(
                            icon = Icons.Outlined.ContentCopy,
                            label = "Yolu Kopyala",
                            tint = TextSecondary,
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Path", filePath))
                                Toast.makeText(context, "Yol kopyalandı", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = SurfaceVariantDark,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
    }
}

private fun getMimeType(fileName: String): String {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    if (ext.isEmpty()) return "*/*"
    val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
    return when {
        mime != null -> mime
        ext == "kt" || ext == "kts" -> "text/x-kotlin"
        ext == "java" -> "text/x-java-source"
        ext == "py" -> "text/x-python"
        ext == "js" || ext == "mjs" || ext == "cjs" -> "application/javascript"
        ext == "ts" || ext == "tsx" -> "application/typescript"
        ext == "json" -> "application/json"
        ext == "md" || ext == "markdown" -> "text/markdown"
        ext == "sh" || ext == "bash" -> "application/x-sh"
        ext == "html" || ext == "htm" -> "text/html"
        ext == "css" -> "text/css"
        ext == "xml" -> "text/xml"
        ext == "svg" -> "image/svg+xml"
        ext == "png" -> "image/png"
        ext == "jpg" || ext == "jpeg" -> "image/jpeg"
        ext == "gif" -> "image/gif"
        ext == "webp" -> "image/webp"
        ext == "pdf" -> "application/pdf"
        ext == "apk" -> "application/vnd.android.package-archive"
        ext == "zip" -> "application/zip"
        else -> "text/plain"
    }
}

private suspend fun fetchFileBytes(filePath: String, textContent: String?): ByteArray? = withContext(Dispatchers.IO) {
    // 1. Direct local file check
    val sourceFile = File(filePath)
    if (sourceFile.exists() && sourceFile.canRead() && sourceFile.length() > 0) {
        try {
            return@withContext sourceFile.readBytes()
        } catch (_: Exception) {}
    }

    // 2. Text content if available
    if (textContent != null) {
        return@withContext textContent.toByteArray(Charsets.UTF_8)
    }

    // 3. Fallback: stream from Termux HTTP server (/api/fs/raw)
    try {
        val encoded = URLEncoder.encode(filePath, "UTF-8")
        val url = URL("http://127.0.0.1:8080/api/fs/raw?path=$encoded")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5000
            readTimeout = 20000
        }
        if (conn.responseCode in 200..299) {
            conn.inputStream.use { input ->
                return@withContext input.readBytes()
            }
        }
    } catch (_: Exception) {}

    null
}

private suspend fun prepareLocalExportFileAsync(context: Context, filePath: String, fileName: String, textContent: String?): File? = withContext(Dispatchers.IO) {
    val cacheDir = File(context.cacheDir, "shared_files").apply { mkdirs() }
    val destFile = File(cacheDir, fileName)

    val bytes = fetchFileBytes(filePath, textContent)
    if (bytes != null && bytes.isNotEmpty()) {
        try {
            FileOutputStream(destFile).use { it.write(bytes) }
            return@withContext destFile
        } catch (_: Exception) {}
    }

    if (destFile.exists() && destFile.length() > 0) {
        return@withContext destFile
    }

    null
}

private fun openFileWithExternalApp(context: Context, scope: CoroutineScope, filePath: String, fileName: String, textContent: String?) {
    scope.launch {
        val file = prepareLocalExportFileAsync(context, filePath, fileName, textContent)
        if (file == null || !file.exists() || file.length() == 0L) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Dosya içeriği alınamadı.", Toast.LENGTH_SHORT).show()
            }
            return@launch
        }

        withContext(Dispatchers.Main) {
            try {
                val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val mimeType = getMimeType(fileName)

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                val chooser = Intent.createChooser(intent, "Dosyayı Aç: $fileName").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, "Bu dosya tipini (${fileName.substringAfterLast('.')}) açacak bir uygulama bulunamadı.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Dosya açılamadı: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private fun copyFileToStorage(context: Context, scope: CoroutineScope, filePath: String, fileName: String, textContent: String?) {
    scope.launch {
        val bytes = fetchFileBytes(filePath, textContent)
        if (bytes == null || bytes.isEmpty()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Dosya içeriği bulunamadı.", Toast.LENGTH_SHORT).show()
            }
            return@launch
        }

        try {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                ?: File("/storage/emulated/0/Download")
            if (!downloadDir.exists()) downloadDir.mkdirs()

            val destFile = File(downloadDir, fileName)
            FileOutputStream(destFile).use { it.write(bytes) }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "✅ İndirilenler klasörüne kaydedildi:\n${destFile.absolutePath}", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Kayıt başarısız: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

private fun shareFileWithIntent(context: Context, scope: CoroutineScope, filePath: String, fileName: String, textContent: String?) {
    scope.launch {
        val file = prepareLocalExportFileAsync(context, filePath, fileName, textContent)
        if (file == null || !file.exists() || file.length() == 0L) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Paylaşılacak dosya içeriği alınamadı.", Toast.LENGTH_SHORT).show()
            }
            return@launch
        }

        withContext(Dispatchers.Main) {
            try {
                val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val mimeType = getMimeType(fileName)

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, fileName)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(shareIntent, "Dosyayı Paylaş: $fileName").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } catch (e: Exception) {
                Toast.makeText(context, "Paylaşım başlatılamadı: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
    return String.format(java.util.Locale.US, "%.1f %s", value, units[digitGroups])
}
