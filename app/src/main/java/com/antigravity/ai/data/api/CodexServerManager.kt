package com.antigravity.ai.data.api

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object CodexServerManager {
    private const val READY_URL = "http://127.0.0.1:4500/readyz"
    private val client = OkHttpClient.Builder()
        .connectTimeout(1, TimeUnit.SECONDS)
        .readTimeout(1, TimeUnit.SECONDS)
        .build()

    suspend fun ensureStarted(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (isReady()) return@withContext true
        launch(context)
        repeat(12) {
            delay(500)
            if (isReady()) return@withContext true
        }
        false
    }

    private fun isReady(): Boolean = runCatching {
        client.newCall(Request.Builder().url(READY_URL).get().build()).execute().use { it.isSuccessful }
    }.getOrDefault(false)

    private fun launch(context: Context) {
        val intent = Intent().apply {
            setClassName("com.termux", "com.termux.app.RunCommandService")
            action = "com.termux.RUN_COMMAND"
            putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/taskset")
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf(
                "-c", "0-5",
                "/data/data/com.termux/files/usr/bin/nice", "-n", "15",
                "/data/data/com.termux/files/usr/bin/codex", "app-server",
                "--listen", "ws://127.0.0.1:4500"
            ))
            putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home")
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
            putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0")
            putExtra("com.termux.RUN_COMMAND_WAKE_LOCK", true)
            putExtra("com.termux.RUN_COMMAND_KEEP_ALIVE", true)
        }
        runCatching {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }.recoverCatching { context.startService(intent) }
    }
}
