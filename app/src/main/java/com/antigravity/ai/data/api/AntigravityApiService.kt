package com.antigravity.ai.data.api

import com.antigravity.ai.data.model.*
import com.google.gson.Gson
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
    data class Chunk(val textDelta: String, val fullContent: String) : StreamEvent()
    data class ToolUpdate(val tool: ToolCall) : StreamEvent()
    data class Done(val botMessage: SessionMessage?) : StreamEvent()
    data class Error(val message: String) : StreamEvent()
    object SessionReset : StreamEvent()
}

class AntigravityApiService(private val baseUrl: String = "http://127.0.0.1:8080") {

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // For SSE streaming
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val sseFactory = EventSources.createFactory(client)

    suspend fun getSession(): Result<SessionResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/session")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Server error: ${response.code}"))
                }
                val body = response.body?.string() ?: "{}"
                val sessionRes = gson.fromJson(body, SessionResponse::class.java)
                Result.success(sessionRes)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPrompt(prompt: String, continueChat: Boolean = true): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = JsonObject().apply {
                addProperty("prompt", prompt)
                addProperty("continue", continueChat)
            }
            val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl/api/chat")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Send prompt failed: ${response.code}"))
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun newChat(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/new-chat")
                .post("{}".toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Result.success(Unit) else Result.failure(IOException("New chat failed"))
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

    fun observeEvents(): Flow<StreamEvent> = callbackFlow {
        val request = Request.Builder()
            .url("$baseUrl/api/events")
            .header("Accept", "text/event-stream")
            .build()

        val listener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                try {
                    when (type) {
                        "chunk" -> {
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
                            val json = gson.fromJson(data, JsonObject::class.java)
                            val botMsg = if (json.has("botMessage")) {
                                gson.fromJson(json.get("botMessage"), SessionMessage::class.java)
                            } else null
                            trySend(StreamEvent.Done(botMsg))
                        }
                        "session_reset" -> {
                            trySend(StreamEvent.SessionReset)
                        }
                        "error" -> {
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
                // Keep channel open or retry silently
            }

            override fun onClosed(eventSource: EventSource) {}
        }

        val eventSource = sseFactory.newEventSource(request, listener)

        awaitClose {
            eventSource.cancel()
        }
    }
}
