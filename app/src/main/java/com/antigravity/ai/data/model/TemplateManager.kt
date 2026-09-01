package com.antigravity.ai.data.model

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.UUID

object TemplateManager {
    private const val FILE_NAME = "prompt_templates.json"
    private val gson = Gson()

    val DEFAULT_TEMPLATES = listOf(
        PromptTemplate(
            id = "tpl_standard_goal_limits",
            title = "Hedef & Sınırlar (Standart)",
            description = "Kısa, iddialı ve odaklı görev şablonu",
            format = """Hedef: {hedef}
Elindekiler: {elindekiler}
Sınırlar: {sinirlar}
Gerisi sende. Ortalama iş yerine iddialı kararın riskini al.""",
            fields = listOf(
                TemplateField(
                    key = "hedef",
                    label = "Hedef",
                    hint = "Ne olursa iş bitmiş sayılır? (Tek cümle)",
                    isMultiline = true
                ),
                TemplateField(
                    key = "elindekiler",
                    label = "Elindekiler",
                    hint = "Ham malzemeyi özetlemeden buraya yapıştır",
                    isMultiline = true
                ),
                TemplateField(
                    key = "sinirlar",
                    label = "Sınırlar",
                    hint = "Dokunma listesi; neyi asla yapmasın",
                    isMultiline = true
                )
            ),
            isDefault = true
        ),
        PromptTemplate(
            id = "tpl_detailed_metric_limits",
            title = "Detaylı & Ölçülü Görev",
            description = "Ölçü, serbestlik ve katı sınırlar içeren tam şablon",
            format = """Hedef: {hedef}
Elindekiler: {elindekiler}
Sınırlar:
{sinirlar}
Ölçü: {olcu}
Serbestlik: Gerisi sende. Nasıl yapacağını sen seç; ortalama iş yerine iddialı kararın riskini al. Talimatta hata görürsen uygulamadan önce söyle.""",
            fields = listOf(
                TemplateField(
                    key = "hedef",
                    label = "Hedef (Sonuç Dili)",
                    hint = "Tek cümle, sonuç dili: ne olursa iş bitmiş sayılır?",
                    isMultiline = true
                ),
                TemplateField(
                    key = "elindekiler",
                    label = "Elindekiler (Ham Malzeme)",
                    hint = "Ham malzeme: mail, log, veri, örnek; özetleme, olduğu gibi yapıştır",
                    isMultiline = true
                ),
                TemplateField(
                    key = "sinirlar",
                    label = "Sınırlar",
                    hint = "- [neye dokunamaz]\n- [neyi kullanamaz]\n- [neyi asla yapamaz; istemediklerini de isimlendir]",
                    defaultValue = "- [neye dokunamaz]\n- [neyi kullanamaz]\n- [neyi asla yapamaz]",
                    isMultiline = true
                ),
                TemplateField(
                    key = "olcu",
                    label = "Ölçü",
                    hint = "Neyin iyi olduğunu nasıl anlayacağız; sayı verebiliyorsan ver",
                    isMultiline = true
                )
            ),
            isDefault = true
        )
    )

    fun getTemplates(context: Context): List<PromptTemplate> {
        return try {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) {
                saveTemplates(context, DEFAULT_TEMPLATES)
                return DEFAULT_TEMPLATES
            }
            val json = file.readText()
            val type = object : TypeToken<List<PromptTemplate>>() {}.type
            val list: List<PromptTemplate> = gson.fromJson(json, type) ?: emptyList()
            if (list.isEmpty()) {
                saveTemplates(context, DEFAULT_TEMPLATES)
                DEFAULT_TEMPLATES
            } else {
                list
            }
        } catch (e: Exception) {
            DEFAULT_TEMPLATES
        }
    }

    fun saveTemplates(context: Context, templates: List<PromptTemplate>) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            file.writeText(gson.toJson(templates))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addOrUpdateTemplate(context: Context, template: PromptTemplate) {
        val current = getTemplates(context).toMutableList()
        val index = current.indexOfFirst { it.id == template.id }
        if (index >= 0) {
            current[index] = template
        } else {
            current.add(template)
        }
        saveTemplates(context, current)
    }

    fun deleteTemplate(context: Context, templateId: String) {
        val current = getTemplates(context).toMutableList()
        current.removeAll { it.id == templateId }
        saveTemplates(context, current)
    }

    fun render(template: PromptTemplate, values: Map<String, String>): String {
        var result = template.format
        for ((k, v) in values) {
            result = result.replace("{$k}", v.trim())
        }
        return result
    }
}
