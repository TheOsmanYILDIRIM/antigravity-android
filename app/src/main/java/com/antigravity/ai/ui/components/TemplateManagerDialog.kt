package com.antigravity.ai.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.antigravity.ai.data.model.PromptTemplate
import com.antigravity.ai.data.model.TemplateField
import com.antigravity.ai.data.model.TemplateManager
import com.antigravity.ai.ui.theme.*
import java.util.UUID

@Composable
fun TemplateManagerDialog(
    onDismiss: () -> Unit,
    onSelectTemplateToFill: (PromptTemplate) -> Unit
) {
    val context = LocalContext.current
    var templates by remember { mutableStateOf(TemplateManager.getTemplates(context)) }
    var editingTemplate by remember { mutableStateOf<PromptTemplate?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp)),
            color = SurfaceDark
        ) {
            if (isCreatingNew || editingTemplate != null) {
                // Template Edit / Create View
                TemplateEditorView(
                    template = editingTemplate,
                    onSave = { newTpl ->
                        TemplateManager.addOrUpdateTemplate(context, newTpl)
                        templates = TemplateManager.getTemplates(context)
                        editingTemplate = null
                        isCreatingNew = false
                    },
                    onCancel = {
                        editingTemplate = null
                        isCreatingNew = false
                    }
                )
            } else {
                // Template List View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = GeminiPurple,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Şablon & Prompt Yöneticisi",
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Kapat",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = BorderSubtle)
                    Spacer(modifier = Modifier.height(12.dp))

                    // List of Templates
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(templates, key = { it.id }) { tpl ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = SurfaceVariantDark,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        onSelectTemplateToFill(tpl)
                                        onDismiss()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = tpl.title,
                                                color = TextPrimary,
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (tpl.isDefault) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = GeminiBlue.copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = "Varsayılan",
                                                        color = GeminiBlue,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        if (tpl.description.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = tpl.description,
                                                color = TextMuted,
                                                fontSize = 12.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "${tpl.fields.size} Değişken Alanı: ${tpl.fields.joinToString(", ") { "{${it.key}}" }}",
                                            color = GeminiPurple.copy(alpha = 0.9f),
                                            fontSize = 11.sp
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { editingTemplate = tpl },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Düzenle",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        if (!tpl.isDefault) {
                                            IconButton(
                                                onClick = {
                                                    TemplateManager.deleteTemplate(context, tpl.id)
                                                    templates = TemplateManager.getTemplates(context)
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Sil",
                                                    tint = DangerRed.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = BorderSubtle)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Action: Add New Template Button
                    Button(
                        onClick = { isCreatingNew = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GeminiBlue)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Yeni Özel Şablon Ekle", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun TemplateEditorView(
    template: PromptTemplate?,
    onSave: (PromptTemplate) -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf(template?.title ?: "") }
    var description by remember { mutableStateOf(template?.description ?: "") }
    var format by remember { mutableStateOf(template?.format ?: "Hedef: {hedef}\nElindekiler: {elindekiler}\nSınırlar: {sinirlar}\nGerisi sende.") }

    // Değişken açıklamalarını ve etiketlerini saklayan harita
    val fieldConfigs = remember {
        mutableStateMapOf<String, TemplateField>().apply {
            template?.fields?.forEach { f ->
                put(f.key, f)
            }
        }
    }

    // Format içindeki {degisken} anahtarlarını ayrıştır
    val detectedKeys by remember(format) {
        derivedStateOf {
            val regex = Regex("\\{([a-zA-Z0-9_]+)\\}")
            regex.findAll(format).map { it.groupValues[1] }.distinct().toList()
        }
    }

    // Yeni tespit edilen anahtarları fieldConfigs haritasına ekle
    LaunchedEffect(detectedKeys) {
        detectedKeys.forEach { k ->
            if (!fieldConfigs.containsKey(k)) {
                fieldConfigs[k] = TemplateField(
                    key = k,
                    label = k.replaceFirstChar { it.uppercase() },
                    hint = "$k alanına yazılacak veri veya talimat",
                    isMultiline = true
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (template == null) "Yeni Şablon Oluştur" else "Şablonu Düzenle",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "İptal", tint = TextMuted, modifier = Modifier.size(18.dp))
            }
        }

        HorizontalDivider(color = BorderSubtle)

        Column {
            Text("Şablon Başlığı", color = GeminiBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Örn: Kod İnceleme & Optimizasyon", color = TextMuted, fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = InputBackground,
                    unfocusedContainerColor = InputBackground,
                    focusedBorderColor = GeminiBlue,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )
        }

        Column {
            Text("Açıklama (İsteğe Bağlı)", color = GeminiBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("Şablonun amacını belirten kısa not", color = TextMuted, fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = InputBackground,
                    unfocusedContainerColor = InputBackground,
                    focusedBorderColor = GeminiBlue,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )
        }

        Column {
            Text("Şablon Formatı ({değişken} şeklinde alanlar yazın)", color = GeminiBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = format,
                onValueChange = { format = it },
                placeholder = { Text("Hedef: {hedef}\nElindekiler: {elindekiler}\nSınırlar: {sinirlar}", color = TextMuted, fontSize = 13.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 200.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = InputBackground,
                    unfocusedContainerColor = InputBackground,
                    focusedBorderColor = GeminiBlue,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "💡 Süslü parantez içine yazdığınız her {kelime} aşağıda özelleştirilebilir açıklama ve ipucu kutularına dönüşür.",
                color = TextMuted,
                fontSize = 11.5.sp
            )
        }

        // Değişken Açıklamaları ve İpuçları Alanı
        if (detectedKeys.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "📌 DEĞİŞKEN AÇIKLAMALARI VE İPUÇLARI (${detectedKeys.size} Alan)",
                color = WarningAmber,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                detectedKeys.forEach { key ->
                    val currentField = fieldConfigs[key] ?: TemplateField(key = key, label = key, hint = "")
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceVariantDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = GeminiBlue.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "{$key}",
                                        color = GeminiBlue,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Alanı İçin Açıklamalar",
                                    color = TextPrimary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Kullanıcıya Görünecek Başlık (Label):", color = TextSecondary, fontSize = 11.5.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            OutlinedTextField(
                                value = currentField.label,
                                onValueChange = { newLabel ->
                                    fieldConfigs[key] = currentField.copy(label = newLabel)
                                },
                                placeholder = { Text("Örn: Hedef (Sonuç Dili)", color = TextMuted, fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = InputBackground,
                                    unfocusedContainerColor = InputBackground,
                                    focusedBorderColor = GeminiBlue,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.5.sp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Kullanıcıya Görünecek İpucu / Ne Yazılacak Açıklaması (Hint):", color = TextSecondary, fontSize = 11.5.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            OutlinedTextField(
                                value = currentField.hint,
                                onValueChange = { newHint ->
                                    fieldConfigs[key] = currentField.copy(hint = newHint)
                                },
                                placeholder = { Text("Örn: Tek cümle, sonuç dili: ne olursa iş bitmiş sayılır? Özetleme, olduğu gibi yapıştır.", color = TextMuted, fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = InputBackground,
                                    unfocusedContainerColor = InputBackground,
                                    focusedBorderColor = GeminiBlue,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.5.sp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
            ) {
                Text("İptal", fontSize = 13.5.sp)
            }

            Button(
                onClick = {
                    if (title.isNotBlank() && format.isNotBlank()) {
                        val fields = detectedKeys.map { k ->
                            fieldConfigs[k] ?: TemplateField(
                                key = k,
                                label = k.replaceFirstChar { it.uppercase() },
                                hint = "$k alanını doldurun",
                                isMultiline = true
                            )
                        }

                        val newTpl = PromptTemplate(
                            id = template?.id ?: UUID.randomUUID().toString(),
                            title = title.trim(),
                            description = description.trim(),
                            format = format.trim(),
                            fields = if (fields.isNotEmpty()) fields else listOf(
                                TemplateField(key = "icerik", label = "İçerik", hint = "Metin girin")
                            ),
                            isDefault = false
                        )
                        onSave(newTpl)
                    }
                },
                enabled = title.isNotBlank() && format.isNotBlank(),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GeminiBlue)
            ) {
                Text("Kaydet", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
