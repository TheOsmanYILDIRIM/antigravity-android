package com.antigravity.ai.data.api

import com.antigravity.ai.data.model.*
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge

/**
 * opencode `serve` sunucusunu [ChatBackend] arayüzüne uyarlayan backend.
 *
 * opencode'un zengin olay protokolünü (text delta, tool part, permission,
 * question, error) uygulamanın ortak [StreamEvent] modeline çevirir; böylece
 * UI/ViewModel opencode mu yoksa agy mi olduğundan habersiz kalır.
 */
class OpenCodeBackend(
    private val api: OpenCodeApiService = OpenCodeApiService()
) : ChatBackend {

    private var currentSessionID: String? = null
    private val fullText = mutableMapOf<String, String>()
    private val toolStepIndex = mutableMapOf<String, Int>()
    private val controlEvents = MutableSharedFlow<StreamEvent>(extraBufferCapacity = 64)

    override fun observeEvents(): Flow<StreamEvent> =
        merge(api.observeRawEvents().mapNotNull { mapRaw(it) }, controlEvents)

    override suspend fun getConversations(): Result<ConversationsResponse> = api.listSessions().map { list ->
        val convs = list.map { s ->
            ConversationMeta(
                id = s.get("id")?.asString ?: "",
                title = s.get("title")?.asString ?: "Sohbet",
                messageCount = 0
            )
        }
        ConversationsResponse("ok", currentSessionID, convs)
    }

    override suspend fun loadConversation(id: String): Result<SessionResponse> = api.getMessages(id).map { msgs ->
        currentSessionID = id
        val sessionMessages = msgs.map { mapMessage(it) }
        SessionResponse(
            status = "ok",
            session = SessionData(id = id, conversationId = id, title = "Sohbet", messages = sessionMessages, isGenerating = false),
            isGenerating = false
        )
    }

    override suspend fun deleteConversation(id: String): Result<Unit> = api.deleteSession(id).mapCatching {}

    override suspend fun getModelsConfig(): Result<ModelsConfigResponse> =
        api.getJson("/api/model").map { root ->
            val arr = root.getAsJsonArray("models") ?: root.getAsJsonArray("data")
            val models = arr?.map { m ->
                val o = m.asJsonObject
                ModelItem(
                    id = o.get("id")?.asString ?: "",
                    name = o.get("name")?.asString ?: (o.get("id")?.asString ?: ""),
                    description = o.get("description")?.asString ?: ""
                )
            } ?: emptyList()
            ModelsConfigResponse("ok", models, null, null)
        }

    override suspend fun getSkills(): Result<SkillsResponse> =
        api.getJson("/api/skill").map { root ->
            val arr = root.getAsJsonArray("skills") ?: root.getAsJsonArray("data")
            val skills = arr?.map { m ->
                val o = m.asJsonObject
                SkillItem(
                    name = o.get("name")?.asString ?: "",
                    command = o.get("name")?.asString ?: "",
                    description = o.get("description")?.asString ?: "",
                    path = o.get("path")?.asString
                )
            } ?: emptyList()
            SkillsResponse("ok", skills.size, skills)
        }

    override suspend fun getUsage(): Result<UsageResponse> =
        Result.success(UsageResponse("ok", null))

    // opencode'da "mevcut oturum" kavramı yok; bu çağrı mesajları silmesin diye no-op.
    override suspend fun fetchSession(): Result<SessionResponse> =
        Result.failure(UnsupportedOperationException("opencode'da fetchSession yok"))

    override suspend fun sendPrompt(
        prompt: String,
        continueChat: Boolean,
        settings: ChatSettings,
        attachments: List<Attachment>
    ): Result<Unit> = runCatching {
        val sid = if (continueChat && currentSessionID != null) {
            currentSessionID!!
        } else {
            val created = api.createSession().getOrThrow()
            currentSessionID = created
            controlEvents.tryEmit(StreamEvent.Init(created))
            created
        }
        api.sendPrompt(sid, prompt).getOrThrow()
    }

    override suspend fun newChat(): Result<SessionResponse> = runCatching {
        val sid = api.createSession().getOrThrow()
        currentSessionID = sid
        controlEvents.tryEmit(StreamEvent.Init(sid))
        SessionResponse(
            status = "ok",
            session = SessionData(id = sid, conversationId = sid, title = "Yeni Sohbet", messages = emptyList(), isGenerating = false),
            isGenerating = false
        )
    }

    override suspend fun stopGeneration(): Result<Unit> = Result.success(Unit)

    override suspend fun replyPermission(
        sessionID: String,
        requestID: String,
        allow: Boolean,
        always: Boolean
    ): Result<Unit> {
        val reply = if (!allow) "reject" else if (always) "always" else "once"
        return api.replyPermission(sessionID, requestID, reply)
    }

    override suspend fun replyQuestion(
        sessionID: String,
        requestID: String,
        answers: List<String>
    ): Result<Unit> = api.replyQuestion(sessionID, requestID, answers)

    // ---- çeviriciler ----

    private fun mapMessage(msg: JsonObject): SessionMessage {
        val role = msg.get("role")?.asString ?: "assistant"
        val parts = msg.getAsJsonArray("parts")
        val content = StringBuilder()
        val tools = mutableListOf<ToolCall>()
        parts?.forEach { p ->
            val o = p.asJsonObject
            when (o.get("type")?.asString) {
                "text" -> content.append(o.get("text")?.asString ?: "")
                "tool" -> {
                    val callID = o.get("callID")?.asString ?: ""
                    val idx = toolStepIndex.getOrPut(callID) { toolStepIndex.size }
                    tools.add(
                        ToolCall(
                            stepIndex = idx,
                            name = o.get("tool")?.asString ?: "tool",
                            state = (o.get("state")?.asString ?: "active").uppercase(),
                            parameters = o.getAsJsonObject("metadata")?.toMap(),
                            output = null
                        )
                    )
                }
            }
        }
        return SessionMessage(
            role = role,
            content = content.toString(),
            tools = tools,
            usage = null,
            attachments = emptyList(),
            time = null,
            state = "done"
        )
    }

    private fun mapRaw(event: RawEvent): StreamEvent? {
        val p = event.properties
        return when (event.type) {
            "session.next.text.delta" -> {
                val mid = p.get("assistantMessageID")?.asString ?: return null
                val delta = p.get("delta")?.asString ?: ""
                fullText[mid] = (fullText[mid] ?: "") + delta
                StreamEvent.Chunk(delta, fullText[mid]!!)
            }
            "message.part.updated" -> {
                val part = p.getAsJsonObject("part") ?: return null
                if (part.get("type")?.asString != "tool") return null
                val callID = part.get("callID")?.asString ?: return null
                val idx = toolStepIndex.getOrPut(callID) { toolStepIndex.size }
                StreamEvent.ToolUpdate(
                    ToolCall(
                        stepIndex = idx,
                        name = part.get("tool")?.asString ?: "tool",
                        state = (part.get("state")?.asString ?: "active").uppercase(),
                        parameters = part.getAsJsonObject("metadata")?.toMap(),
                        output = null
                    )
                )
            }
            "session.next.tool.success" -> {
                val callID = p.get("callID")?.asString ?: return null
                val idx = toolStepIndex[callID] ?: return null
                StreamEvent.ToolUpdate(ToolCall(stepIndex = idx, name = "", state = "DONE"))
            }
            "session.next.tool.failed" -> {
                val callID = p.get("callID")?.asString ?: return null
                val idx = toolStepIndex[callID] ?: return null
                StreamEvent.ToolUpdate(ToolCall(stepIndex = idx, name = "", state = "ERROR"))
            }
            "session.next.text.ended" -> StreamEvent.Done(null)
            "session.error" -> StreamEvent.Error(
                p.getAsJsonObject("error")?.get("message")?.asString ?: "Bilinmeyen hata"
            )
            "__error__" -> StreamEvent.Error(
                p.get("message")?.asString ?: "Sunucu bağlantısı kesildi"
            )
            "permission.v2.asked" -> {
                val tool = p.getAsJsonObject("tool")
                StreamEvent.PermissionRequested(
                    PermissionRequestData(
                        id = p.get("id")?.asString ?: "",
                        sessionID = p.get("sessionID")?.asString ?: "",
                        action = p.get("action")?.asString ?: "",
                        resources = p.getAsJsonArray("resources")?.map { it.asString } ?: emptyList(),
                        messageID = tool?.get("messageID")?.asString,
                        callID = tool?.get("callID")?.asString
                    )
                )
            }
            "question.v2.asked" -> {
                val qs = p.getAsJsonArray("questions")?.map { q ->
                    val o = q.asJsonObject
                    o.get("question")?.asString ?: o.get("text")?.asString ?: ""
                } ?: emptyList()
                StreamEvent.QuestionRequested(
                    QuestionRequestData(
                        id = p.get("id")?.asString ?: "",
                        sessionID = p.get("sessionID")?.asString ?: "",
                        questions = qs,
                        tool = p.getAsJsonObject("tool")?.get("name")?.asString
                    )
                )
            }
            else -> null
        }
    }
}

private fun JsonObject.toMap(): Map<String, Any> {
    val out = mutableMapOf<String, Any>()
    for ((k, v) in entrySet()) {
        out[k] = when {
            v.isJsonObject -> v.asJsonObject.toMap()
            v.isJsonArray -> v.asJsonArray.map { it.toString() }
            v.isJsonPrimitive -> v.asString
            else -> v.toString()
        }
    }
    return out
}
