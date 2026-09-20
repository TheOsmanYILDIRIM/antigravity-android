package com.antigravity.ai.data.api

import com.antigravity.ai.data.model.*
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

/** Adapts the official Codex app-server protocol to the app's shared chat model. */
class CodexBackend(
    private val api: CodexApiService = CodexApiService()
) : ChatBackend {
    private var currentThreadId: String? = null
    private var currentTurnId: String? = null
    private val fullText = mutableMapOf<String, String>()
    private val toolSteps = mutableMapOf<String, Int>()
    private val toolOutputs = mutableMapOf<String, String>()
    private val agyApi = AntigravityApiService()

    override fun observeEvents(): Flow<StreamEvent> = api.observeMessages().mapNotNull(::mapEvent)

    override suspend fun getConversations(): Result<ConversationsResponse> =
        api.request("thread/list", JsonObject().apply { addProperty("limit", 100) }).mapCatching { result ->
            val threads = result.array("data", "threads")
            ConversationsResponse("ok", currentThreadId, threads.map { element ->
                val thread = element.asJsonObject
                ConversationMeta(
                    id = thread.string("id"),
                    title = thread.stringOrNull("name") ?: thread.stringOrNull("preview")?.take(60) ?: "Codex Sohbeti",
                    createdAt = thread.stringOrNull("createdAt")
                )
            })
        }

    override suspend fun loadConversation(id: String): Result<SessionResponse> =
        api.request("thread/read", JsonObject().apply {
            addProperty("threadId", id)
            addProperty("includeTurns", true)
        }).mapCatching { result ->
            currentThreadId = id
            val thread = result.getAsJsonObject("thread") ?: result
            SessionResponse("ok", SessionData(
                id = id,
                conversationId = id,
                title = thread.stringOrNull("name") ?: "Codex Sohbeti",
                messages = mapThreadMessages(thread),
                isGenerating = false
            ))
        }

    override suspend fun deleteConversation(id: String): Result<Unit> =
        api.request("thread/archive", JsonObject().apply { addProperty("threadId", id) }).mapCatching { Unit }

    override suspend fun getModelsConfig(): Result<ModelsConfigResponse> =
        api.request("model/list", JsonObject().apply {
            addProperty("limit", 50)
            addProperty("includeHidden", false)
        }).mapCatching { result ->
            val rows = result.array("data", "models")
            val models = rows.map { element ->
                val model = element.asJsonObject
                ModelItem(
                    id = model.stringOrNull("model") ?: model.string("id"),
                    name = model.stringOrNull("displayName") ?: model.stringOrNull("model") ?: model.string("id")
                )
            }
            ModelsConfigResponse(
                "ok",
                listOf(ModelItem("default", "Codex varsayılan modeli")) + models,
                listOf(
                    EffortItem("default", "Varsayılan"),
                    EffortItem("low", "Düşük"),
                    EffortItem("medium", "Orta"),
                    EffortItem("high", "Yüksek")
                ),
                emptyList()
            )
        }

    override suspend fun getSkills(): Result<SkillsResponse> = Result.success(SkillsResponse("ok", 0, emptyList()))
    override suspend fun getUsage(): Result<UsageResponse> = Result.success(UsageResponse("ok", null))

    override suspend fun sendPrompt(
        prompt: String,
        conversationId: String?,
        continueChat: Boolean,
        settings: ChatSettings,
        attachments: List<Attachment>
    ): Result<Unit> = runCatching {
        val target = conversationId ?: currentThreadId
        val threadId = if (continueChat && target != null) {
            api.request("thread/resume", JsonObject().apply { addProperty("threadId", target) }).getOrThrow()
            target
        } else {
            val result = api.request("thread/start", JsonObject().apply {
                addProperty("cwd", "/data/data/com.termux/files/home")
                addProperty("approvalPolicy", "unlessTrusted")
                addProperty("serviceName", "antigravity_android")
            }).getOrThrow()
            result.getAsJsonObject("thread")?.string("id") ?: error("Codex thread id dönmedi")
        }
        currentThreadId = threadId
        val text = buildString {
            append(prompt)
            attachments.mapNotNull { it.path ?: it.relPath }.forEach { append("\n[Ek: ").append(it).append(']') }
        }
        val input = JsonArray().apply { add(JsonObject().apply {
            addProperty("type", "text")
            addProperty("text", text)
        }) }
        api.request("turn/start", JsonObject().apply {
            addProperty("threadId", threadId)
            add("input", input)
            settings.model.takeUnless { it == "default" }?.let { addProperty("model", it) }
            settings.effort.takeUnless { it == "default" }?.let { addProperty("effort", it) }
        }).getOrThrow().getAsJsonObject("turn")?.stringOrNull("id")?.let { currentTurnId = it }
    }

    override suspend fun newChat(): Result<SessionResponse> =
        api.request("thread/start", JsonObject().apply {
            addProperty("cwd", "/data/data/com.termux/files/home")
            addProperty("approvalPolicy", "unlessTrusted")
            addProperty("serviceName", "antigravity_android")
        }).mapCatching { result ->
            val id = result.getAsJsonObject("thread")?.string("id") ?: error("Codex thread id dönmedi")
            currentThreadId = id
            SessionResponse("ok", SessionData(id, id, "Yeni Codex Sohbeti", messages = emptyList()))
        }

    override suspend fun stopGeneration(): Result<Unit> {
        val threadId = currentThreadId ?: return Result.success(Unit)
        val turnId = currentTurnId ?: return Result.success(Unit)
        return api.request("turn/interrupt", JsonObject().apply {
            addProperty("threadId", threadId)
            addProperty("turnId", turnId)
        }).mapCatching { Unit }
    }

    override suspend fun replyPermission(
        sessionID: String,
        requestID: String,
        allow: Boolean,
        always: Boolean
    ): Result<Unit> {
        val rpcId = requestID.toLongOrNull()
            ?: return Result.failure(IllegalArgumentException("Geçersiz Codex onay kimliği"))
        val decision = when {
            !allow -> "decline"
            always -> "acceptForSession"
            else -> "accept"
        }
        return api.respond(rpcId, JsonObject().apply { addProperty("decision", decision) })
    }

    override suspend fun uploadFile(name: String, base64: String, type: String) = agyApi.uploadFile(name, base64, type)
    override suspend fun fetchVaultFiles() = agyApi.getVaultFiles()
    override suspend fun fetchVaultFileContent(relPath: String) = agyApi.getVaultFileContent(relPath)
    override suspend fun saveVaultNote(relPath: String?, title: String?, content: String) = agyApi.saveVaultNote(relPath, title, content)
    override suspend fun createVaultFolder(folderPath: String) = agyApi.createVaultFolder(folderPath)
    override suspend fun deleteVaultFile(relPath: String) = agyApi.deleteVaultFile(relPath)
    override suspend fun getFsList(dir: String?) = agyApi.getFsList(dir)
    override suspend fun getFsProjects() = agyApi.getFsProjects()
    override suspend fun getFsContent(path: String) = agyApi.getFsContent(path)
    override suspend fun saveFsFile(path: String, content: String) = agyApi.saveFsFile(path, content)

    private fun mapEvent(message: CodexRpcMessage): StreamEvent? {
        val p = message.params
        val threadId = p.stringOrNull("threadId") ?: currentThreadId
        return when (message.method) {
            "thread/started" -> p.getAsJsonObject("thread")?.stringOrNull("id")?.let {
                currentThreadId = it
                StreamEvent.Init(it)
            }
            "turn/started" -> {
                currentTurnId = p.getAsJsonObject("turn")?.stringOrNull("id")
                StreamEvent.GeneratingStatus(threadId, true)
            }
            "item/agentMessage/delta" -> {
                val itemId = p.stringOrNull("itemId") ?: "agent"
                val delta = p.stringOrNull("delta") ?: ""
                fullText[itemId] = (fullText[itemId] ?: "") + delta
                StreamEvent.Chunk(delta, fullText[itemId].orEmpty(), threadId)
            }
            "item/commandExecution/outputDelta" -> mapToolDelta(p, threadId)
            "item/commandExecution/requestApproval", "item/fileChange/requestApproval" -> {
                val requestId = message.requestId ?: return null
                val resources = buildList {
                    val commandValue = p.get("command")
                    val command = when {
                        commandValue == null || commandValue.isJsonNull -> null
                        commandValue.isJsonArray -> commandValue.asJsonArray.joinToString(" ") { it.asString }
                        else -> commandValue.asString
                    }
                    command?.let(::add)
                    p.stringOrNull("cwd")?.let(::add)
                    p.stringOrNull("reason")?.let(::add)
                }
                StreamEvent.PermissionRequested(PermissionRequestData(
                    id = requestId.toString(),
                    sessionID = threadId.orEmpty(),
                    action = if (message.method.contains("fileChange")) "Dosya değişikliği" else "Komut çalıştırma",
                    resources = resources
                ))
            }
            "item/started", "item/completed" -> mapItem(p, threadId, message.method == "item/completed")
            "turn/completed" -> {
                currentTurnId = null
                StreamEvent.Done(null, threadId)
            }
            "error" -> StreamEvent.Error(
                p.getAsJsonObject("error")?.stringOrNull("message") ?: "Codex çalıştırma hatası",
                threadId
            )
            "__error__" -> StreamEvent.Error(p.stringOrNull("message") ?: "Codex bağlantı hatası", threadId)
            else -> null
        }
    }

    private fun mapToolDelta(p: JsonObject, threadId: String?): StreamEvent? {
        val itemId = p.stringOrNull("itemId") ?: return null
        val idx = toolSteps.getOrPut(itemId) { toolSteps.size }
        toolOutputs[itemId] = (toolOutputs[itemId] ?: "") + (p.stringOrNull("delta") ?: "")
        return StreamEvent.ToolUpdate(ToolCall(idx, "terminal", "ACTIVE", output = toolOutputs[itemId]), threadId)
    }

    private fun mapItem(p: JsonObject, threadId: String?, completed: Boolean): StreamEvent? {
        val item = p.getAsJsonObject("item") ?: return null
        if (item.stringOrNull("type") == "agentMessage" && completed) {
            item.stringOrNull("id")?.let(fullText::remove)
            return null
        }
        if (item.stringOrNull("type") != "commandExecution") return null
        val itemId = item.stringOrNull("id") ?: return null
        val idx = toolSteps.getOrPut(itemId) { toolSteps.size }
        val commandValue = item.get("command")
        val command = when {
            commandValue == null || commandValue.isJsonNull -> "terminal"
            commandValue.isJsonArray -> commandValue.asJsonArray.joinToString(" ") { it.asString }
            else -> commandValue.asString
        }
        val output = item.stringOrNull("aggregatedOutput") ?: toolOutputs[itemId]
        if (completed) toolOutputs.remove(itemId)
        return StreamEvent.ToolUpdate(
            ToolCall(idx, command, if (completed) "DONE" else "ACTIVE", output = output),
            threadId
        )
    }

    private fun mapThreadMessages(thread: JsonObject): List<SessionMessage> {
        val messages = mutableListOf<SessionMessage>()
        thread.getAsJsonArray("turns")?.forEach { turnElement ->
            turnElement.asJsonObject.getAsJsonArray("items")?.forEach { itemElement ->
                val item = itemElement.asJsonObject
                when (item.stringOrNull("type")) {
                    "userMessage" -> messages += SessionMessage("user", item.contentText(), emptyList(), null, emptyList(), null, "done")
                    "agentMessage" -> messages += SessionMessage("assistant", item.stringOrNull("text") ?: item.contentText(), emptyList(), null, emptyList(), null, "done")
                }
            }
        }
        return messages
    }
}

private fun JsonObject.string(name: String): String = get(name)?.asString ?: error("$name yok")
private fun JsonObject.stringOrNull(name: String): String? = get(name)?.takeUnless { it.isJsonNull }?.asString
private fun JsonObject.array(vararg names: String): List<JsonElement> {
    for (name in names) getAsJsonArray(name)?.let { return it.toList() }
    return emptyList()
}
private fun JsonObject.contentText(): String = getAsJsonArray("content")
    ?.mapNotNull { it.asJsonObject.stringOrNull("text") }
    ?.joinToString("")
    .orEmpty()
