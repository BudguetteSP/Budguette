package com.example.budguette

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.*

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val subName = intent.getStringExtra("subName") ?: "Subscription"
        val subAmount = intent.getDoubleExtra("subAmount", 0.0)
        val frequency = intent.getStringExtra("frequency") ?: "One-Time"
        val nextDateMillis = intent.getLongExtra("nextDate", 0L)

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val builder = NotificationCompat.Builder(context, "subscription_reminders")
            .setSmallIcon(R.drawable.baseline_notifications_24)
            .setContentTitle("Upcoming Subscription")
            .setContentText("$subName is due soon: $$subAmount")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        NotificationManagerCompat.from(context)
            .notify(System.currentTimeMillis().toInt(), builder.build())

        // 🔄 Reschedule recurring subscriptions
        if (frequency != "One-Time" && nextDateMillis > 0) {
            val cal = Calendar.getInstance().apply { timeInMillis = nextDateMillis }
            when (frequency) {
                "Daily" -> cal.add(Calendar.DAY_OF_YEAR, 1)
                "Weekly" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                "Monthly" -> cal.add(Calendar.MONTH, 1)
                "Yearly" -> cal.add(Calendar.YEAR, 1)
            }
            if (cal.timeInMillis > System.currentTimeMillis()) {
                ReminderScheduler.scheduleReminder(
                    context, subName, subAmount, frequency, cal.timeInMillis
                )
            }
        }
    }
}



