package com.antigravity.ai.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Wakes Termux only for the small, locally-approved schedule set. */
class TerminalScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val actionId = intent.getStringExtra(EXTRA_ACTION_ID) ?: return
        if (actionId !in ALLOWED_ACTIONS) return
        val run = Intent().apply {
            setClassName("com.termux", "com.termux.app.RunCommandService")
            action = "com.termux.RUN_COMMAND"
            putExtra("com.termux.RUN_COMMAND_PATH", commandFor(actionId))
            if (actionId != "agy-start") putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("start"))
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
            putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0")
            putExtra("com.termux.RUN_COMMAND_WAKE_LOCK", true)
            putExtra("com.termux.RUN_COMMAND_KEEP_ALIVE", true)
        }
        runCatching {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(run)
            } else context.startService(run)
        }.recoverCatching { context.startService(run) }
    }

    companion object {
        const val EXTRA_ACTION_ID = "actionId"
        private val COMMANDS = mapOf(
            "agy-start" to "/data/data/com.termux/files/home/.termux/tasker/agy-web-start.sh",
            "codex-start" to "/data/data/com.termux/files/home/antigravity-termux-server/bin/codex-web",
            "opencode-start" to "/data/data/com.termux/files/home/antigravity-termux-server/bin/opencode-web",
            "cline-start" to "/data/data/com.termux/files/home/antigravity-termux-server/bin/cline-web"
        )
        val ALLOWED_ACTIONS = COMMANDS.keys
        private fun commandFor(actionId: String): String = COMMANDS[actionId]!!
    }
}
