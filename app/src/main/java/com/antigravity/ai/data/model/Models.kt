package com.antigravity.ai.data.model

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val role: String, // "user" or "bot"
    var content: String = "",
    val tools: MutableList<ToolCall> = mutableListOf(),
    var usage: UsageStats? = null,
    val attachments: List<Attachment> = emptyList(),
    val pastedBlocks: List<PastedBlock> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    var state: MessageState = MessageState.DONE
)

enum class MessageState {
    GENERATING,
    DONE,
    ERROR
}

data class ToolCall(
    @SerializedName("step_index")
    val stepIndex: Int = 0,
    @SerializedName("name")
    val name: String = "",
    @SerializedName("state")
    var state: String = "ACTIVE", // "ACTIVE" or "DONE"
    @SerializedName("parameters")
    val parameters: Map<String, Any>? = null,
    @SerializedName("output")
    var output: String? = null,
    @SerializedName("duration_seconds")
    var durationSeconds: Double? = null,
    @SerializedName("error")
    val error: String? = null
)

data class UsageStats(
    @SerializedName("total_tokens")
    val totalTokens: Int = 0,
    @SerializedName("thinking_tokens")
    val thinkingTokens: Int = 0,
    @SerializedName("input_tokens")
    val inputTokens: Int = 0,
    @SerializedName("output_tokens")
    val outputTokens: Int = 0,
    @SerializedName("cache_read_tokens")
    val cacheReadTokens: Int = 0
)

data class PastedBlock(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val lineCount: Int = content.lines().size,
    val charCount: Int = content.length
)

data class Attachment(
    val name: String,
    val path: String? = null,
    val localUri: String? = null,
    val relPath: String? = null,
    val type: String = "file", // "image", "doc", "vault"
    val size: Long? = null
)

data class UploadResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("fileName")
    val fileName: String,
    @SerializedName("path")
    val path: String,
    @SerializedName("relPath")
    val relPath: String,
    @SerializedName("workspacePath")
    val workspacePath: String?,
    @SerializedName("size")
    val size: Long,
    @SerializedName("type")
    val type: String
)

data class ModelItem(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String = ""
)

data class EffortItem(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String = ""
)

data class ModeItem(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String
)

data class ModelsConfigResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("models")
    val models: List<ModelItem>?,
    @SerializedName("efforts")
    val efforts: List<EffortItem>?,
    @SerializedName("modes")
    val modes: List<ModeItem>?
)

data class ConversationMeta(
    @SerializedName("id")
    val id: String,
    @SerializedName("title")
    val title: String = "Yeni Sohbet",
    @SerializedName("createdAt")
    val createdAt: String? = null,
    @SerializedName("lastMessageTime")
    val lastMessageTime: String? = null,
    @SerializedName("messageCount")
    val messageCount: Int = 0
)

data class ConversationsResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("currentSessionId")
    val currentSessionId: String?,
    @SerializedName("conversations")
    val conversations: List<ConversationMeta>?
)

data class VaultItem(
    @SerializedName("name")
    val name: String,
    @SerializedName("path")
    val path: String,
    @SerializedName("isDirectory")
    val isDirectory: Boolean = false,
    @SerializedName("size")
    val size: Long? = null,
    @SerializedName("updatedAt")
    val updatedAt: String? = null
)

data class VaultResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("vaultDir")
    val vaultDir: String,
    @SerializedName("files")
    val files: List<VaultItem>?
)

data class VaultFileContent(
    @SerializedName("status")
    val status: String,
    @SerializedName("path")
    val path: String?,
    @SerializedName("content")
    val content: String?
)

data class FsItem(
    @SerializedName("name")
    val name: String,
    @SerializedName("path")
    val path: String,
    @SerializedName("isDirectory")
    val isDirectory: Boolean = false,
    @SerializedName("size")
    val size: Long? = null,
    @SerializedName("updatedAt")
    val updatedAt: String? = null,
    @SerializedName("extension")
    val extension: String? = null,
    @SerializedName("isProject")
    val isProject: Boolean = false,
    @SerializedName("projectType")
    val projectType: String? = null,
    @SerializedName("itemCount")
    val itemCount: Int? = null
)

data class ProjectItem(
    @SerializedName("name")
    val name: String,
    @SerializedName("path")
    val path: String,
    @SerializedName("type")
    val type: String, // Android, Node.js, Next.js, React, Python, Rust, Go, Git
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("gitBranch")
    val gitBranch: String? = null,
    @SerializedName("lastModified")
    val lastModified: String? = null
)

data class FsListResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("currentDir")
    val currentDir: String,
    @SerializedName("parentDir")
    val parentDir: String?,
    @SerializedName("homeDir")
    val homeDir: String,
    @SerializedName("items")
    val items: List<FsItem>?,
    @SerializedName("projects")
    val projects: List<ProjectItem>?
)

data class FsProjectsResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("count")
    val count: Int = 0,
    @SerializedName("projects")
    val projects: List<ProjectItem>?
)

data class FsContentResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("path")
    val path: String,
    @SerializedName("content")
    val content: String? = null,
    @SerializedName("size")
    val size: Long = 0,
    @SerializedName("lineCount")
    val lineCount: Int = 0,
    @SerializedName("isBinary")
    val isBinary: Boolean = false,
    @SerializedName("extension")
    val extension: String? = null
)

data class FsSaveResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("path")
    val path: String? = null,
    @SerializedName("size")
    val size: Long? = null
)

fun resolveMediaUrl(rawPath: String, baseUrl: String = "http://127.0.0.1:8080"): String {
    if (rawPath.isBlank()) return ""
    if (rawPath.startsWith("http://") || rawPath.startsWith("https://") || rawPath.startsWith("content://")) {
        return rawPath
    }
    val clean = if (rawPath.startsWith("file://")) rawPath.substring(7) else rawPath
    return try {
        val encoded = java.net.URLEncoder.encode(clean, "UTF-8")
        "$baseUrl/api/files/raw?path=$encoded"
    } catch (e: Exception) {
        "$baseUrl/api/files/raw?path=$clean"
    }
}

data class SkillItem(
    @SerializedName("name")
    val name: String,
    @SerializedName("command")
    val command: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("path")
    val path: String? = null
)

data class SkillsResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("count")
    val count: Int = 0,
    @SerializedName("skills")
    val skills: List<SkillItem>?
)

data class UsageMetrics(
    @SerializedName("totalTokens")
    val totalTokens: Long = 0,
    @SerializedName("turnCount")
    val turnCount: Int = 0,
    @SerializedName("usedPercent")
    val usedPercent: Int = 0,
    @SerializedName("remainingPercent")
    val remainingPercent: Int = 100,
    @SerializedName("windowHours")
    val windowHours: Int = 5,
    @SerializedName("inputTokens")
    val inputTokens: Long = 0,
    @SerializedName("outputTokens")
    val outputTokens: Long = 0,
    @SerializedName("thinkingTokens")
    val thinkingTokens: Long = 0
)

data class UsageData(
    @SerializedName("recent5h")
    val recent5h: UsageMetrics?,
    @SerializedName("weekly")
    val weekly: UsageMetrics?,
    @SerializedName("lastTurn")
    val lastTurn: UsageStats?,
    @SerializedName("lastUpdated")
    val lastUpdated: String? = null
)

data class UsageResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("usage")
    val usage: UsageData?
)

data class AuthStatusResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("isAuthenticated")
    val isAuthenticated: Boolean = false,
    @SerializedName("authMethod")
    val authMethod: String = "none"
)

data class AgLoginResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("authUrl")
    val authUrl: String? = null,
    @SerializedName("error")
    val error: String? = null
)

data class AgLoginCodeResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("error")
    val error: String? = null
)

data class ChatSettings(
    val model: String = "gemini-3.7-flash-medium",
    val effort: String = "default", // default, low, medium, high
    val mode: String = "default", // default, plan, accept-edits
    val useVault: Boolean = true,
    val fontSizeSp: Float = 13.5f, // 11.5f (Kompakt), 13.5f (Küçük/Standart), 15.0f (Orta), 16.5f (Büyük)
    val thermalMode: String = "eco", // "eco" (%50 CPU Sınırı), "balanced", "performance"
    val notificationsEnabled: Boolean = true // Üretim bittiğinde / hata olduğunda yerel bildirim
)

data class SessionResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("session")
    val session: SessionData?,
    @SerializedName("isGenerating")
    val isGenerating: Boolean = false
)

data class SessionData(
    @SerializedName("id")
    val id: String?,
    @SerializedName("conversationId")
    val conversationId: String?,
    @SerializedName("title")
    val title: String?,
    @SerializedName("messages")
    val messages: List<SessionMessage>?,
    @SerializedName("isGenerating")
    val isGenerating: Boolean = false
)

data class SessionMessage(
    @SerializedName("role")
    val role: String,
    @SerializedName("content")
    val content: String?,
    @SerializedName("tools")
    val tools: List<ToolCall>?,
    @SerializedName("usage")
    val usage: UsageStats?,
    @SerializedName("attachments")
    val attachments: List<Attachment>?,
    @SerializedName("time")
    val time: String?,
    @SerializedName("state")
    val state: String?
)

data class SlashCommand(
    val command: String,
    val description: String,
    val example: String,
    val isSkill: Boolean = false
)

data class TemplateField(
    val key: String,
    val label: String,
    val hint: String = "",
    val defaultValue: String = "",
    val isMultiline: Boolean = true
)

data class PromptTemplate(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val format: String,
    val fields: List<TemplateField> = emptyList(),
    val isDefault: Boolean = false
)
