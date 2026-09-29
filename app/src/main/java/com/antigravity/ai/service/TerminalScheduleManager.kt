package com.antigravity.ai.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object TerminalScheduleManager {
    private val ACTION_ID = Regex("^[A-Za-z0-9._:-]{1,96}$")
    fun schedule(context: Context, id: String, triggerAtMillis: Long, actionId: String): Boolean {
        if (!ACTION_ID.matches(actionId) || triggerAtMillis <= System.currentTimeMillis()) return false

        val intent = Intent(context, TerminalScheduleReceiver::class.java)
            .putExtra(TerminalScheduleReceiver.EXTRA_ACTION_ID, actionId)
        val pending = PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(AlarmManager::class.java)

        return runCatching {
            val canUseExactAlarm = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                alarmManager.canScheduleExactAlarms()

            if (canUseExactAlarm) {
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pending
                    )
                } catch (_: SecurityException) {
                    // Permission can be revoked between the capability check and
                    // the call. Fall back instead of losing the scheduled Action.
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pending
                    )
                }
            } else {
                // Actions are shortcuts, not hard real-time alarms. A slightly
                // inexact fallback is preferable to requiring an extra permission
                // or failing the schedule entirely.
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pending
                )
            }
        }.isSuccess
    }
}
