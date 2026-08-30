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
        var launched = false

        // 1. Try Termux RUN_COMMAND IPC Service
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
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                try {
                    context.startForegroundService(intent)
                } catch (e: Exception) {
                    context.startService(intent)
                }
            } else {
                context.startService(intent)
            }
            launched = true
        } catch (e: Exception) {}

        // 2. Try Termux:Float Service trigger (com.termux.window.TermuxFloatService)
        try {
            val floatServiceIntent = Intent().apply {
                setClassName("com.termux.window", "com.termux.window.TermuxFloatService")
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                try {
                    context.startForegroundService(floatServiceIntent)
                } catch (e: Exception) {
                    context.startService(floatServiceIntent)
                }
            } else {
                context.startService(floatServiceIntent)
            }
            launched = true
        } catch (e: Exception) {}

        // 3. Fallback: Launch Termux/Float Activity silently if background services were restricted
        if (!launched) {
            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage("com.termux.window")
                    ?: context.packageManager.getLaunchIntentForPackage("com.termux.float")
                    ?: context.packageManager.getLaunchIntentForPackage("com.termux")

                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    context.startActivity(launchIntent)
                    launched = true

                    // Bring Antigravity back to foreground seamlessly
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        try {
                            val bringBack = Intent(context, com.antigravity.ai.MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            }
                            context.startActivity(bringBack)
                        } catch (e: Exception) {}
                    }, 400)
                }
            } catch (e: Exception) {}
        }

        onLaunched?.invoke(launched, if (launched) "Termux agy-web arka planda başlatıldı." else "Başlatma sinyali gönderildi.")
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
        startServer(context) { success, msg ->
            onRestarted?.invoke(success, if (success) "Yeniden başlatıldı." else msg)
        }
    }
}
