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

class ClineApiService(private val baseUrl: String = "http://127.0.0.1:5115") {

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val sseClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val sseFactory = EventSources.createFactory(sseClient)

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

    suspend fun sendPrompt(
        prompt: String,
        conversationId: String? = null,
        continueChat: Boolean = true,
        settings: ChatSettings = ChatSettings(),
        attachments: List<Attachment> = emptyList()
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = JsonObject().apply {
                addProperty("prompt", prompt)
                conversationId?.let { addProperty("conversationId", it) }
                addProperty("continueChat", continueChat)
                addProperty("model", settings.model)
                addProperty("effort", settings.effort)
                addProperty("mode", settings.mode)
            }

            val request = Request.Builder()
                .url("$baseUrl/api/chat")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Result.success(Unit) else Result.failure(IOException("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun newChat(): Result<SessionResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/session/reset")
                .post("{}".toRequestBody("application/json".toMediaType()))
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

    suspend fun stopGeneration(conversationId: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = JsonObject().apply {
                conversationId?.let { addProperty("conversationId", it) }
            }
            val request = Request.Builder()
                .url("$baseUrl/api/stop")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Result.success(Unit) else Result.failure(IOException("HTTP ${response.code}"))
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
                    when (type) {
                        "init" -> {
                            terminated = false
                            val json = gson.fromJson(data, JsonObject::class.java)
                            val convId = json.get("conversationId")?.asString ?: ""
                            if (convId.isNotEmpty()) {
                                trySend(StreamEvent.Init(convId))
                            }
                        }
                        "generating_start" -> {
                            terminated = false
                            val json = gson.fromJson(data, JsonObject::class.java)
                            val convId = json.get("conversationId")?.asString
                            trySend(StreamEvent.GeneratingStatus(convId, true))
                        }
                        "generating_done" -> {
                            val json = gson.fromJson(data, JsonObject::class.java)
                            val convId = json.get("conversationId")?.asString
                            trySend(StreamEvent.GeneratingStatus(convId, false))
                        }
                        "chunk" -> {
                            val json = gson.fromJson(data, JsonObject::class.java)
                            val delta = json.get("text_delta")?.asString ?: ""
                            val full = json.get("full_content")?.asString ?: ""
                            val convId = json.get("conversationId")?.asString
                            trySend(StreamEvent.Chunk(delta, full, convId))
                        }
                        "tool_update" -> {
                            val json = gson.fromJson(data, JsonObject::class.java)
                            val toolObj = json.get("tool")
                            val tool = gson.fromJson(toolObj, ToolCall::class.java)
                            val convId = json.get("conversationId")?.asString
                            trySend(StreamEvent.ToolUpdate(tool, convId))
                        }
                        "done" -> {
                            terminated = true
                            val json = gson.fromJson(data, JsonObject::class.java)
                            val botMsg = if (json.has("botMessage")) {
                                gson.fromJson(json.get("botMessage"), SessionMessage::class.java)
                            } else null
                            val convId = json.get("conversationId")?.asString
                            trySend(StreamEvent.Done(botMsg, convId))
                        }
                        "stopped" -> {
                            terminated = true
                            val json = gson.fromJson(data, JsonObject::class.java)
                            val convId = json.get("conversationId")?.asString
                            trySend(StreamEvent.Stopped(convId))
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
                            val convId = json.get("conversationId")?.asString
                            trySend(StreamEvent.Error(err, convId))
                        }
                        "stderr" -> {
                            val json = gson.fromJson(data, JsonObject::class.java)
                            val txt = json.get("text")?.asString ?: ""
                            val convId = json.get("conversationId")?.asString
                            if (txt.isNotBlank()) trySend(StreamEvent.Stderr(txt.trim(), convId))
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
                            "Cline sunucu bağlantısı kesildi${t?.message?.let { " ($it)" } ?: ""}. Üretim durdu."
                        )
                    )
                }
            }

            override fun onClosed(eventSource: EventSource) {
                if (!terminated) {
                    terminated = true
                    trySend(
                        StreamEvent.Error("Cline yanıt akışı kapandı.")
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
