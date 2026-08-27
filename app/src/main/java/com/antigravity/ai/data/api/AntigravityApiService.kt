package com.antigravity.ai.data.api

import com.antigravity.ai.data.model.*
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class StreamEvent {
    data class Init(val conversationId: String) : StreamEvent()
    data class Chunk(val textDelta: String, val fullContent: String) : StreamEvent()
    data class ToolUpdate(val tool: ToolCall) : StreamEvent()
    data class Done(val botMessage: SessionMessage?) : StreamEvent()
    object Stopped : StreamEvent()
    data class Error(val message: String) : StreamEvent()
    data class SessionLoaded(val session: SessionData) : StreamEvent()
    object SessionReset : StreamEvent()
    data class PermissionRequested(val request: PermissionRequestData) : StreamEvent()
    data class QuestionRequested(val request: QuestionRequestData) : StreamEvent()
}

class AntigravityApiService(private val baseUrl: String = "http://127.0.0.1:8080") {

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // For SSE streaming
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val sseFactory = EventSources.createFactory(client)

    suspend fun getConversations(): Result<ConversationsResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/conversations")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(IOException("HTTP ${response.code}"))
                val body = response.body?.string() ?: "{}"
                Result.success(gson.fromJson(body, ConversationsResponse::class.java))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadConversation(id: String): Result<SessionResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/conversations/$id")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(IOException("HTTP ${response.code}"))
                val body = response.body?.string() ?: "{}"
                Result.success(gson.fromJson(body, SessionResponse::class.java))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteConversation(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/conversations/$id")
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Result.success(Unit) else Result.failure(IOException("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getModelsConfig(): Result<ModelsConfigResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/models")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(IOException("HTTP ${response.code}"))
                val body = response.body?.string() ?: "{}"
                Result.success(gson.fromJson(body, ModelsConfigResponse::class.java))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSkills(): Result<SkillsResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/skills")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(IOException("HTTP ${response.code}"))
                val body = response.body?.string() ?: "{}"
                Result.success(gson.fromJson(body, SkillsResponse::class.java))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUsage(): Result<UsageResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/usage")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(IOException("HTTP ${response.code}"))
                val body = response.body?.string() ?: "{}"
                Result.success(gson.fromJson(body, UsageResponse::class.java))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadFile(name: String, base64: String, type: String): Result<UploadResponse> = withContext(Dispatchers.IO) {
        try {
            val json = JsonObject().apply {
                addProperty("name", name)
                addProperty("base64", base64)
                addProperty("type", type)
            }
            val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl/api/upload")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(IOException("Upload failed: ${response.code}"))
                val body = response.body?.string() ?: "{}"
                Result.success(gson.fromJson(body, UploadResponse::class.java))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getVaultFiles(): Result<VaultResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/vault")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(IOException("HTTP ${response.code}"))
                val body = response.body?.string() ?: "{}"
                Result.success(gson.fromJson(body, VaultResponse::class.java))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getVaultFileContent(relPath: String): Result<VaultFileContent> = withContext(Dispatchers.IO) {
        try {
            val encoded = java.net.URLEncoder.encode(relPath, "UTF-8")
            val request = Request.Builder()
                .url("$baseUrl/api/vault/content?path=$encoded")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(IOException("HTTP ${response.code}"))
                val body = response.body?.string() ?: "{}"
                Result.success(gson.fromJson(body, VaultFileContent::class.java))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveVaultNote(relPath: String?, title: String?, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = JsonObject().apply {
                relPath?.let { addProperty("relPath", it) }
                title?.let { addProperty("title", it) }
                addProperty("content", content)
            }
            val request = Request.Builder()
                .url("$baseUrl/api/vault/note")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Result.success(Unit) else Result.failure(IOException("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createVaultFolder(folderPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = JsonObject().apply { addProperty("folderPath", folderPath) }
            val request = Request.Builder()
                .url("$baseUrl/api/vault/folder")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Result.success(Unit) else Result.failure(IOException("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteVaultFile(relPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val encoded = java.net.URLEncoder.encode(relPath, "UTF-8")
            val request = Request.Builder()
                .url("$baseUrl/api/vault/file?path=$encoded")
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Result.success(Unit) else Result.failure(IOException("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSession(): Result<SessionResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/session")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(IOException("HTTP ${response.code}"))
                val body = response.body?.string() ?: "{}"
                Result.success(gson.fromJson(body, SessionResponse::class.java))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPrompt(
        prompt: String,
        continueChat: Boolean = true,
        settings: ChatSettings = ChatSettings(),
        attachments: List<Attachment> = emptyList()
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = JsonObject().apply {
                addProperty("prompt", prompt)
                addProperty("continue", continueChat)
                addProperty("model", settings.model)
                addProperty("effort", settings.effort)
                addProperty("mode", settings.mode)
                addProperty("useVault", settings.useVault)

                if (attachments.isNotEmpty()) {
                    val attArray = JsonArray()
                    attachments.forEach { att ->
                        attArray.add(JsonObject().apply {
                            addProperty("name", att.name)
                            addProperty("path", att.path)
                            addProperty("type", att.type)
                            att.size?.let { addProperty("size", it) }
                        })
                    }
                    add("attachments", attArray)
                }
            }

            val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl/api/chat")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(IOException("Send prompt failed: ${response.code}"))
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun newChat(): Result<SessionResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/new-chat")
                .post("{}".toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "{}"
                    Result.success(gson.fromJson(body, SessionResponse::class.java))
                } else {
                    Result.failure(IOException("New chat failed"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun stopGeneration(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/stop")
                .post("{}".toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Result.success(Unit) else Result.failure(IOException("Stop failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAuthStatus(): Result<AuthStatusResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/auth/status")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(IOException("HTTP ${response.code}"))
                val body = response.body?.string() ?: "{}"
                Result.success(gson.fromJson(body, AuthStatusResponse::class.java))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitAuthToken(token: String): Result<AuthTokenResponse> = withContext(Dispatchers.IO) {
        try {
            val json = JsonObject().apply {
                addProperty("token", token)
            }
            val request = Request.Builder()
                .url("$baseUrl/api/auth/token")
                .post(json.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: "{}"
                Result.success(gson.fromJson(body, AuthTokenResponse::class.java))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeEvents(): Flow<StreamEvent> = callbackFlow {
        var terminated = false
        val request = Request.Builder()
            .url("$baseUrl/api/events")
            .header("Accept", "text/event-stream")
            .build()

        val listener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                try {
                    if (type != "done" && type != "stopped" && type != "error") terminated = false
                    when (type) {
                        "init" -> {
                            terminated = false
                            val json = gson.fromJson(data, JsonObject::class.java)
                            val convId = json.get("conversationId")?.asString ?: ""
                            if (convId.isNotEmpty()) {
                                trySend(StreamEvent.Init(convId))
                            }
                        }
                        "chunk" -> {
                            terminated = false
                            val json = gson.fromJson(data, JsonObject::class.java)
                            val delta = json.get("text_delta")?.asString ?: ""
                            val full = json.get("full_content")?.asString ?: ""
                            trySend(StreamEvent.Chunk(delta, full))
                        }
                        "tool_update" -> {
                            val json = gson.fromJson(data, JsonObject::class.java)
                            val toolObj = json.get("tool")
                            val tool = gson.fromJson(toolObj, ToolCall::class.java)
                            trySend(StreamEvent.ToolUpdate(tool))
                        }
                        "done" -> {
                            terminated = true
                            val json = gson.fromJson(data, JsonObject::class.java)
                            val botMsg = if (json.has("botMessage")) {
                                gson.fromJson(json.get("botMessage"), SessionMessage::class.java)
                            } else null
                            trySend(StreamEvent.Done(botMsg))
                        }
                        "stopped" -> {
                            terminated = true
                            trySend(StreamEvent.Stopped)
                        }
                        "session_loaded" -> {
                            val json = gson.fromJson(data, JsonObject::class.java)
                            if (json.has("session")) {
                                val session = gson.fromJson(json.get("session"), SessionData::class.java)
                                trySend(StreamEvent.SessionLoaded(session))
                            }
                        }
                        "session_reset" -> {
                            trySend(StreamEvent.SessionReset)
                        }
                        "error" -> {
                            terminated = true
                            val json = gson.fromJson(data, JsonObject::class.java)
                            val err = json.get("error")?.asString ?: "Unknown error"
                            trySend(StreamEvent.Error(err))
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                if (!terminated) {
                    terminated = true
                    trySend(
                        StreamEvent.Error(
                            "Sunucu bağlantısı kesildi${t?.message?.let { " ($it)" } ?: ""}. Üretim durdu, yanıt tamamlanamadı."
                        )
                    )
                }
            }

            override fun onClosed(eventSource: EventSource) {
                if (!terminated) {
                    terminated = true
                    trySend(
                        StreamEvent.Error("Sunucu yanıt akışı beklenmedik şekilde kapandı. Üretim tamamlanamadı.")
                    )
                }
            }
        }

        val eventSource = sseFactory.newEventSource(request, listener)

        awaitClose {
            eventSource.cancel()
        }
    }
}
