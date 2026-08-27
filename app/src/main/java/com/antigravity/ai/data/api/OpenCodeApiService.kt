package com.antigravity.ai.data.api

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
import java.util.concurrent.TimeUnit

/** Ham opencode sunucu olayı (SSE `data` satırından çözülmüş). */
data class RawEvent(val type: String, val properties: JsonObject)

/**
 * opencode `serve` sunucusuna bağlanan düşük seviyeli istemci.
 * Sadece ham REST çağrılarını ve SSE akışını sağlar; protokol->model çevirisi
 * [OpenCodeBackend]'te yapılır.
 *
 * Varsayılan: http://127.0.0.1:4096 (opencode serve --port 4096)
 */
class OpenCodeApiService(
    private val baseUrl: String = "http://127.0.0.1:4096",
    private val password: String? = null
) {
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val sseFactory = EventSources.createFactory(client)

    private fun Request.Builder.auth(): Request.Builder {
        if (!password.isNullOrBlank()) {
            val creds = "opencode:$password"
            val encoded = android.util.Base64.encodeToString(creds.toByteArray(), android.util.Base64.NO_WRAP)
            header("Authorization", "Basic $encoded")
        }
        return this
    }

    internal suspend fun getJson(path: String): Result<JsonObject> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url("$baseUrl$path").get().auth().build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                gson.fromJson(resp.body?.string() ?: "{}", JsonObject::class.java)
            }
        }
    }

    private suspend fun postJson(path: String, body: JsonObject): Result<JsonObject> = withContext(Dispatchers.IO) {
        runCatching {
            val rb = body.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val req = Request.Builder().url("$baseUrl$path").post(rb).auth().build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                gson.fromJson(resp.body?.string() ?: "{}", JsonObject::class.java)
            }
        }
    }

    /** Yeni oturum oluşturur, oturum id'sini döndürür. */
    suspend fun createSession(): Result<String> = postJson("/api/session", JsonObject()).mapCatching {
        it.get("id")?.asString ?: error("session id yok")
    }

    /** Tüm oturumları (Session listesi) döndürür. */
    suspend fun listSessions(): Result<List<JsonObject>> = getJson("/api/session").mapCatching { root ->
        root.getAsJsonArray("sessions")?.map { it.asJsonObject } ?: emptyList()
    }

    /** Bir oturumdaki mesajları döndürür. */
    suspend fun getMessages(sessionID: String): Result<List<JsonObject>> =
        getJson("/api/session/${sessionID}/message").mapCatching { root ->
            root.getAsJsonArray("messages")?.map { it.asJsonObject } ?: emptyList()
        }

    /** Oturuma prompt gönderir (generate tetikler). */
    suspend fun sendPrompt(sessionID: String, text: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val prompt = JsonObject().apply { addProperty("text", text) }
            val body = JsonObject().apply { add("prompt", prompt); addProperty("delivery", "queue") }
            postJson("/api/session/$sessionID/prompt", body).getOrThrow()
            Unit
        }
    }

    /** Oturumu siler (sunucu destekliyorsa). */
    suspend fun deleteSession(sessionID: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url("$baseUrl/api/session/$sessionID").delete().auth().build()
            client.newCall(req).execute().use { }
        }
    }

    /** İzin isteğine yanıt verir (once | always | reject). */
    suspend fun replyPermission(sessionID: String, requestID: String, reply: String): Result<Unit> =
        postJson(
            "/api/session/$sessionID/permission/$requestID/reply",
            JsonObject().apply { addProperty("reply", reply) }
        ).mapCatching {}

    /** Kullanıcı sorusuna yanıt verir. */
    suspend fun replyQuestion(sessionID: String, requestID: String, answers: List<String>): Result<Unit> {
        val arr = com.google.gson.JsonArray().also { answers.forEach { a -> it.add(a) } }
        return postJson(
            "/api/session/$sessionID/question/$requestID/reply",
            JsonObject().apply { add("answers", arr) }
        ).mapCatching {}
    }

    /** Sunucu olay akışı (SSE). Ham olaylar [RawEvent] olarak yayılır. */
    fun observeRawEvents(): Flow<RawEvent> = callbackFlow {
        var terminated = false
        val req = Request.Builder()
            .url("$baseUrl/api/event")
            .header("Accept", "text/event-stream")
            .auth()
            .build()

        val listener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                try {
                    val json = gson.fromJson(data, JsonObject::class.java) ?: return
                    val t = json.get("type")?.asString ?: return
                    if (t == "session.next.text.ended" || t == "session.error") terminated = true
                    if (t != "session.next.text.ended" && t != "session.error") terminated = false
                    val props = json.getAsJsonObject("properties") ?: JsonObject()
                    trySend(RawEvent(t, props))
                } catch (_: Exception) {
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                if (!terminated) {
                    terminated = true
                    trySend(
                        RawEvent(
                            "__error__",
                            JsonObject().apply {
                                addProperty(
                                    "message",
                                    "OpenCode sunucu bağlantısı kesildi${t?.message?.let { " ($it)" } ?: ""}. Üretim durdu."
                                )
                            }
                        )
                    )
                }
            }

            override fun onClosed(eventSource: EventSource) {
                if (!terminated) {
                    terminated = true
                    trySend(
                        RawEvent(
                            "__error__",
                            JsonObject().apply {
                                addProperty("message", "OpenCode yanıt akışı beklenmedik şekilde kapandı. Üretim tamamlanamadı.")
                            }
                        )
                    )
                }
            }
        }

        val es = sseFactory.newEventSource(req, listener)
        awaitClose { es.cancel() }
    }
}
