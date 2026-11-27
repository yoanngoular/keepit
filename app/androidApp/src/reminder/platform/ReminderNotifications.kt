package com.ygoular.keepit.reminder.platform

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.ygoular.keepit.MainActivity
import com.ygoular.keepit.R
import com.ygoular.keepit.reminder.domain.ReminderNote

internal class ReminderNotifications(private val context: Context) {
    companion object { const val CHANNEL = "reminders" }

    private val manager = checkNotNull(context.getSystemService(NotificationManager::class.java))

    fun createChannel() {
        manager.createNotificationChannel(NotificationChannel(
            CHANNEL, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_HIGH
        ))
    }

    fun enabled() = manager.areNotificationsEnabled() &&
        manager.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE

    fun show(note: ReminderNote) {
        createChannel()
        if (!enabled()) return
        val openNote = Intent(context, MainActivity::class.java).setData(ReminderRoute.uri(note.id))
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val tap = PendingIntent.getActivity(context, 0, openNote,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(note.title.ifBlank { context.getString(R.string.reminder_channel) })
            .setContentText(note.description)
            .setContentIntent(tap)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .build()
        manager.notify(note.id, 0, notification)
    }

    fun cancel(id: String) = manager.cancel(id, 0)
}
