package com.antigravity.ai.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object TerminalScheduleManager {
    fun schedule(context: Context, id: String, triggerAtMillis: Long, actionId: String): Boolean {
        if (actionId !in TerminalScheduleReceiver.ALLOWED_ACTIONS || triggerAtMillis <= System.currentTimeMillis()) return false
        val intent = Intent(context, TerminalScheduleReceiver::class.java)
            .putExtra(TerminalScheduleReceiver.EXTRA_ACTION_ID, actionId)
        val pending = PendingIntent.getBroadcast(
            context, id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return runCatching {
            context.getSystemService(AlarmManager::class.java)
                .setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
        }.isSuccess
    }
}
