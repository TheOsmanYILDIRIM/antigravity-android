package com.antigravity.ai.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Wakes Termux and delegates the selected id to the external action registry. */
class TerminalScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val actionId = intent.getStringExtra(EXTRA_ACTION_ID) ?: return
        if (!ACTION_ID.matches(actionId)) return

        val run = Intent().apply {
            setClassName("com.termux", "com.termux.app.RunCommandService")
            action = "com.termux.RUN_COMMAND"
            putExtra("com.termux.RUN_COMMAND_PATH", TERMUX_BASH)
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf(GENERIC_ACTION_RUNNER, actionId))
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
            putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0")
            putExtra("com.termux.RUN_COMMAND_WAKE_LOCK", true)
            putExtra("com.termux.RUN_COMMAND_KEEP_ALIVE", true)
        }

        runCatching {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(run)
            } else {
                context.startService(run)
            }
        }.recoverCatching {
            context.startService(run)
        }
    }

    companion object {
        const val EXTRA_ACTION_ID = "actionId"
        private const val TERMUX_BASH = "/data/data/com.termux/files/usr/bin/bash"
        private const val GENERIC_ACTION_RUNNER =
            "/data/data/com.termux/files/home/antigravity-termux-server/bin/agy-action-run"
        private val ACTION_ID = Regex("^[A-Za-z0-9._:-]{1,96}$")
    }
}
