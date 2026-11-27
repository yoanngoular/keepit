package com.ygoular.keepit

import android.content.Intent
import android.Manifest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.ygoular.keepit.reminder.platform.DeviceReminders
import com.ygoular.keepit.reminder.platform.ReminderNotifications
import com.ygoular.keepit.reminder.platform.ReminderRoute
import com.ygoular.keepit.reminder.presentation.ReminderApp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private companion object { const val OPENING = "note_open_request" }

    private val reminders by lazy { DeviceReminders.get(applicationContext) }
    private var notificationStatus by mutableStateOf<String?>(null)
    private var incomingNote by mutableStateOf<String?>(null)
    private var opening by mutableIntStateOf(0)
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        reminders.notifications.createChannel()
        opening = savedInstanceState?.getInt(OPENING) ?: 0
        incomingNote = ReminderRoute.noteId(intent.data)
        setContent {
            val schedulingFailed by reminders.schedulerUnavailable.collectAsState()
            ReminderApp(
                store = reminders,
                openedNoteId = incomingNote,
                openRequest = opening,
                notificationStatus = if (schedulingFailed) getString(R.string.scheduling_unavailable) else notificationStatus,
                requestNotifications = if (!schedulingFailed && notificationStatus != null) ::configureNotifications else null,
                systemBack = { enabled, onBack -> BackHandler(enabled = enabled, onBack = onBack) },
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(OPENING, opening)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        ReminderRoute.noteId(intent.data)?.let {
            incomingNote = it
            opening++
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        lifecycleScope.launch { reminders.reconcile() }
    }

    private fun refreshStatus() {
        notificationStatus = when {
            !reminders.notifications.enabled() -> getString(R.string.notifications_off)
            !reminders.alarms.exactAllowed() -> getString(R.string.alarms_approximate)
            else -> null
        }
    }

    private fun configureNotifications() {
        when {
            Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED &&
                (!getPreferences(MODE_PRIVATE).getBoolean("requested_notifications", false) ||
                    shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) -> {
                getPreferences(MODE_PRIVATE).edit().putBoolean("requested_notifications", true).apply()
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            !reminders.notifications.enabled() -> {
                val manager = checkNotNull(getSystemService(android.app.NotificationManager::class.java))
                val settings = if (manager.areNotificationsEnabled()) {
                    Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_CHANNEL_ID, ReminderNotifications.CHANNEL)
                } else Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                startActivity(settings.putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
            }
            Build.VERSION.SDK_INT >= 31 && !reminders.alarms.exactAllowed() ->
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
        }
    }
}
