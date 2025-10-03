package com.example.budguette

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast

object ReminderScheduler {
    fun scheduleReminder(
        context: Context,
        subName: String,
        subAmount: Double,
        frequency: String,
        triggerAtMillis: Long
    ) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("subName", subName)
            putExtra("subAmount", subAmount)
            putExtra("frequency", frequency)
            putExtra("nextDate", triggerAtMillis)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            subName.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Toast.makeText(
                        context,
                        "Cannot schedule exact alarms. Enable in system settings.",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }
            }

            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
            Toast.makeText(
                context,
                "Failed to schedule reminder: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
