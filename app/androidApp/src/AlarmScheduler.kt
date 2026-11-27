package com.ygoular.keepit

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ygoular.keepit.reminder.ReminderOccurrence

internal class AlarmScheduler(private val context: Context) {
    companion object {
        const val ACTION = "com.ygoular.keepit.REMINDER"
        const val REVISION = "revision"
        const val NUMBER = "number"
    }

    private val alarms = checkNotNull(context.getSystemService(AlarmManager::class.java))
    fun exactAllowed() = Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()

    private fun intent(id: String) = Intent(context, ReminderReceiver::class.java)
        .setAction(ACTION).setData(ReminderRoute.uri(id))

    fun cancel(id: String) {
        val token = PendingIntent.getBroadcast(context, 0, intent(id),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE) ?: return
        alarms.cancel(token)
        token.cancel()
    }

    fun schedule(id: String, revision: String, occurrence: ReminderOccurrence) {
        val message = intent(id).putExtra(REVISION, revision).putExtra(NUMBER, occurrence.number)
        val token = PendingIntent.getBroadcast(context, 0, message,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val at = occurrence.at.toEpochMilliseconds()
        if (exactAllowed()) {
            try {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, token)
                return
            } catch (e: SecurityException) {
                if (exactAllowed()) throw e
            }
        }
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, token)
    }
}
