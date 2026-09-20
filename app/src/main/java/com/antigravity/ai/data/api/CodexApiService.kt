package com.antigravity.ai.data.api

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

data class CodexRpcMessage(val method: String, val params: JsonObject, val requestId: Long? = null)

/** JSON-RPC client for `codex app-server --listen ws://127.0.0.1:4500`. */
class CodexApiService(
    private val endpoint: String = "ws://127.0.0.1:4500"
) {
    private val gson = Gson()
    private val ids = AtomicLong(1)
    private val connectMutex = Mutex()
    private val pending = ConcurrentHashMap<Long, CompletableDeferred<JsonObject>>()
    private val events = MutableSharedFlow<CodexRpcMessage>(extraBufferCapacity = 256)
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .build()

    @Volatile private var socket: WebSocket? = null
    @Volatile private var ready: CompletableDeferred<Unit>? = null

    fun observeMessages(): SharedFlow<CodexRpcMessage> = events

    suspend fun request(method: String, params: JsonObject = JsonObject()): Result<JsonObject> =
        withContext(Dispatchers.IO) {
            runCatching {
                ensureConnected()
                requestOnSocket(method, params)
            }
        }

    fun respond(requestId: Long, result: JsonObject): Result<Unit> = runCatching {
        val response = JsonObject().apply {
            addProperty("id", requestId)
            add("result", result)
        }
        check(socket?.send(response.toString()) == true) { "Codex onay yanıtı gönderilemedi" }
    }

    private suspend fun ensureConnected() {
        val current = ready
        if (socket != null && current != null) {
            withTimeout(10_000) { current.await() }
            return
        }

        connectMutex.withLock {
            if (socket == null) {
                val opened = CompletableDeferred<Unit>()
                ready = opened
                socket = client.newWebSocket(Request.Builder().url(endpoint).build(), Listener(opened))
            }
        }
        withTimeout(10_000) { ready?.await() ?: error("Codex bağlantısı başlatılamadı") }
    }

    private suspend fun requestOnSocket(method: String, params: JsonObject): JsonObject {
        val id = ids.getAndIncrement()
        val response = CompletableDeferred<JsonObject>()
        pending[id] = response
        val message = JsonObject().apply {
            addProperty("method", method)
            addProperty("id", id)
            add("params", params)
        }
        if (socket?.send(message.toString()) != true) {
            pending.remove(id)
            error("Codex WebSocket mesajı gönderilemedi")
        }
        return try {
            withTimeout(30_000) { response.await() }
        } finally {
            pending.remove(id, response)
        }
    }

    private inner class Listener(private val opened: CompletableDeferred<Unit>) : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            val initId = ids.getAndIncrement()
            val initResponse = CompletableDeferred<JsonObject>()
            pending[initId] = initResponse
            val init = JsonObject().apply {
                addProperty("method", "initialize")
                addProperty("id", initId)
                add("params", JsonObject().apply {
                    add("clientInfo", JsonObject().apply {
                        addProperty("name", "antigravity_android")
                        addProperty("title", "Antigravity Android")
                        addProperty("version", "1.5.0")
                    })
                })
            }
            if (!webSocket.send(init.toString())) {
                opened.completeExceptionally(IOException("Codex initialize gönderilemedi"))
                return
            }
            initResponse.invokeOnCompletion { cause ->
                if (cause != null) {
                    opened.completeExceptionally(cause)
                } else {
                    webSocket.send(JsonObject().apply {
                        addProperty("method", "initialized")
                        add("params", JsonObject())
                    }.toString())
                    opened.complete(Unit)
                }
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val json = runCatching { gson.fromJson(text, JsonObject::class.java) }.getOrNull() ?: return
            val id = json.get("id")?.takeUnless { it.isJsonNull }?.asLong
            if (id != null && (json.has("result") || json.has("error"))) {
                val deferred = pending.remove(id) ?: return
                val error = json.getAsJsonObject("error")
                if (error != null) {
                    deferred.completeExceptionally(IOException(error.get("message")?.asString ?: "Codex RPC hatası"))
                } else {
                    deferred.complete(json.getAsJsonObject("result") ?: JsonObject())
                }
                return
            }
            val method = json.get("method")?.asString ?: return
            events.tryEmit(CodexRpcMessage(method, json.getAsJsonObject("params") ?: JsonObject(), id))
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            socket = null
            ready = null
            pending.values.forEach { it.completeExceptionally(t) }
            pending.clear()
            events.tryEmit(CodexRpcMessage("__error__", JsonObject().apply {
                addProperty("message", t.message ?: "Codex app-server bağlantısı kesildi")
            }))
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            socket = null
            ready = null
            val error = IOException("Codex app-server bağlantısı kapandı ($code): $reason")
            pending.values.forEach { it.completeExceptionally(error) }
            pending.clear()
            events.tryEmit(CodexRpcMessage("__error__", JsonObject().apply {
                addProperty("message", error.message)
            }))
        }
    }
}
