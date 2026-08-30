package com.antigravity.ai.data.api

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class ServerHealth(
    val isOnline: Boolean,
    val uptimeSeconds: Long = 0,
    val pid: Int = 0,
    val latencyMs: Long = 0,
    val errorMessage: String? = null
)

data class HealthResponse(
    @SerializedName("status") val status: String?,
    @SerializedName("uptime") val uptime: Long?,
    @SerializedName("pid") val pid: Int?,
    @SerializedName("version") val version: String?
)

object AgyServerManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(1500, TimeUnit.MILLISECONDS)
        .readTimeout(2000, TimeUnit.MILLISECONDS)
        .build()

    private val gson = Gson()

    suspend fun checkHealth(baseUrl: String = "http://127.0.0.1:8080"): ServerHealth = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/health")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val latency = System.currentTimeMillis() - startTime
            val body = response.body?.string()

            if (response.isSuccessful && body != null) {
                val health = gson.fromJson(body, HealthResponse::class.java)
                ServerHealth(
                    isOnline = true,
                    uptimeSeconds = health.uptime ?: 0,
                    pid = health.pid ?: 0,
                    latencyMs = latency
                )
            } else {
                ServerHealth(isOnline = false, errorMessage = "HTTP ${response.code}")
            }
        } catch (e: Exception) {
            ServerHealth(isOnline = false, errorMessage = e.message ?: "Bağlantı kurulamadı")
        }
    }

    fun startServer(context: Context, onLaunched: ((Boolean, String) -> Unit)? = null) {
        try {
            val intent = Intent().apply {
                setClassName("com.termux", "com.termux.app.RunCommandService")
                action = "com.termux.RUN_COMMAND"
                putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.termux/tasker/agy-web-start.sh")
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf<String>())
                putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home")
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
                putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0")
            }
            context.startService(intent)
            onLaunched?.invoke(true, "Termux'a agy-web başlatma sinyali gönderildi.")
        } catch (e: SecurityException) {
            // Permission not granted yet or signature difference -> fallback to launch Termux app or show prompt
            val launchIntent = context.packageManager.getLaunchIntentForPackage("com.termux.window")
                ?: context.packageManager.getLaunchIntentForPackage("com.termux.float")
                ?: context.packageManager.getLaunchIntentForPackage("com.termux")

            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                copyCommandToClipboard(context, "agy-web start")
                onLaunched?.invoke(false, "Termux açıldı ve 'agy-web start' panoya kopyalandı.")
            } else {
                copyCommandToClipboard(context, "agy-web start")
                onLaunched?.invoke(false, "Termux komutu panoya kopyalandı: agy-web start")
            }
        } catch (e: Exception) {
            copyCommandToClipboard(context, "agy-web start")
            onLaunched?.invoke(false, "Hata: ${e.message}. Komut kopyalandı: agy-web start")
        }
    }

    suspend fun stopServer(context: Context, onStopped: ((Boolean, String) -> Unit)? = null) = withContext(Dispatchers.IO) {
        var httpSuccess = false
        try {
            val request = Request.Builder()
                .url("http://127.0.0.1:8080/api/system/shutdown")
                .post("{}".toRequestBody())
                .build()
            val response = client.newCall(request).execute()
            httpSuccess = response.isSuccessful
        } catch (e: Exception) {
            httpSuccess = false
        }

        try {
            val intent = Intent().apply {
                setClassName("com.termux", "com.termux.app.RunCommandService")
                action = "com.termux.RUN_COMMAND"
                putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.termux/tasker/agy-web-stop.sh")
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf<String>())
                putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home")
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
                putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0")
            }
            context.startService(intent)
        } catch (e: Exception) {}

        withContext(Dispatchers.Main) {
            onStopped?.invoke(true, if (httpSuccess) "Sunucu durduruldu." else "Durdurma sinyali gönderildi.")
        }
    }

    fun restartServer(context: Context, onRestarted: ((Boolean, String) -> Unit)? = null) {
        try {
            val intent = Intent().apply {
                setClassName("com.termux", "com.termux.app.RunCommandService")
                action = "com.termux.RUN_COMMAND"
                putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.termux/tasker/agy-web-restart.sh")
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf<String>())
                putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home")
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
                putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0")
            }
            context.startService(intent)
            onRestarted?.invoke(true, "Yeniden başlatma komutu gönderildi.")
        } catch (e: Exception) {
            copyCommandToClipboard(context, "agy-web restart")
            onRestarted?.invoke(false, "Termux komutu kopyalandı: agy-web restart")
        }
    }

    private fun copyCommandToClipboard(context: Context, text: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Termux Command", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Panoya kopyalandı: $text", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {}
    }
}
