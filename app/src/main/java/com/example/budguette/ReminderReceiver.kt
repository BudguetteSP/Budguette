package com.example.budguette

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val subName = intent.getStringExtra("subName") ?: "Subscription"
        val subAmount = intent.getDoubleExtra("subAmount", 0.0)
        val frequency = intent.getStringExtra("frequency") ?: "One-Time"
        val nextDateMillis = intent.getLongExtra("nextDate", 0L)

        // ✅ Build notification
        val builder = NotificationCompat.Builder(context, "subscription_reminders")
            .setSmallIcon(R.drawable.baseline_notifications_24) // make sure this exists
            .setContentTitle("Upcoming Subscription")
            .setContentText("$subName is due soon: $$subAmount")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // ✅ Check permission before posting
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            with(NotificationManagerCompat.from(context)) {
                notify(System.currentTimeMillis().toInt(), builder.build())
            }
        } else {
            // Permission denied → do nothing (or log / fallback)
        }

        // 🔄 Reschedule if recurring
        if (frequency != "One-Time" && nextDateMillis > 0) {
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = nextDateMillis }

            when (frequency) {
                "Daily" -> cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                "Weekly" -> cal.add(java.util.Calendar.WEEK_OF_YEAR, 1)
                "Monthly" -> cal.add(java.util.Calendar.MONTH, 1)
                "Yearly" -> cal.add(java.util.Calendar.YEAR, 1)
            }

            ReminderScheduler.scheduleReminder(
                context,
                subName,
                subAmount,
                frequency,
                cal.timeInMillis
            )
        }
    }
}

