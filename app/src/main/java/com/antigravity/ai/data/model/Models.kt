package com.antigravity.ai.data.model

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val role: String, // "user" or "bot"
    var content: String = "",
    val tools: MutableList<ToolCall> = mutableListOf(),
    var usage: UsageStats? = null,
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
    var durationSeconds: Double? = null
)

data class UsageStats(
    @SerializedName("total_tokens")
    val totalTokens: Int = 0,
    @SerializedName("thinking_tokens")
    val thinkingTokens: Int = 0,
    @SerializedName("input_tokens")
    val inputTokens: Int = 0,
    @SerializedName("output_tokens")
    val outputTokens: Int = 0
)

data class Conversation(
    val id: String,
    val title: String,
    val lastMessageTime: Long = System.currentTimeMillis(),
    val messageCount: Int = 0
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
    @SerializedName("conversationId")
    val conversationId: String?,
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
    @SerializedName("time")
    val time: String?,
    @SerializedName("state")
    val state: String?
)

data class StatusResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("busy")
    val busy: Boolean,
    @SerializedName("conversationId")
    val conversationId: String?,
    @SerializedName("messagesCount")
    val messagesCount: Int = 0
)
