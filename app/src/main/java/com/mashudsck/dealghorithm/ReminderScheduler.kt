package com.mashudsck.dealghorithm

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.SystemClock

class ReminderScheduler(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val notifications = context.getSystemService(NotificationManager::class.java)

    init { notifications.createNotificationChannel(NotificationChannel(CHANNEL, "Intention reminders", NotificationManager.IMPORTANCE_DEFAULT)) }

    fun allowed(): Boolean = (Build.VERSION.SDK_INT < 33 ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
        notifications.areNotificationsEnabled() && notifications.getNotificationChannel(CHANNEL)?.importance?.let { it != NotificationManager.IMPORTANCE_NONE } == true

    fun schedule(intent: IntentCheckpoint?) {
        if (intent?.status != IntentStatus.RUNNING || intent.mode != SessionMode.MANUAL) return
        alarms.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + intent.remainingMillis,
            broadcast(intent, "ALARM", PendingIntent.FLAG_UPDATE_CURRENT)!!)
    }

    fun cancelTimer(intent: IntentCheckpoint?) {
        if (intent == null) return
        broadcast(intent, "ALARM", PendingIntent.FLAG_NO_CREATE)?.let { alarms.cancel(it); it.cancel() }
    }

    fun cancelAll(intent: IntentCheckpoint?) {
        cancelTimer(intent)
        notifications.cancel(NOTIFICATION_ID)
        if (intent != null) for (kind in listOf("ADD", "FINISH", "DISMISS")) {
            broadcast(intent, kind, PendingIntent.FLAG_NO_CREATE)?.cancel()
        }
    }

    fun postBudget(intent: IntentCheckpoint): Boolean {
        if (!allowed()) return false
        val open = Intent(context, MainActivity::class.java).setData(identity(intent, "OPEN"))
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val choose = Intent(context, MainActivity::class.java).setData(identity(intent, "CHOOSE"))
            .putExtra("page", "activities").addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val notice = Notification.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Your manual timer has ended")
            .setContentText("Finish, add five minutes, or choose an activity.")
            .setContentIntent(PendingIntent.getActivity(context, 0, open, IMMUTABLE_UPDATE))
            .setDeleteIntent(broadcast(intent, "DISMISS", PendingIntent.FLAG_UPDATE_CURRENT))
            .addAction(Notification.Action.Builder(null, "Finish", broadcast(intent, "FINISH", PendingIntent.FLAG_UPDATE_CURRENT)).build())
            .addAction(Notification.Action.Builder(null, "Add 5 minutes", broadcast(intent, "ADD", PendingIntent.FLAG_UPDATE_CURRENT)).build())
            .addAction(Notification.Action.Builder(null, "Choose an activity", PendingIntent.getActivity(context, 0, choose, IMMUTABLE_UPDATE)).build())
            .setOnlyAlertOnce(true).build()
        return try { notifications.notify(NOTIFICATION_ID, notice); true } catch (_: SecurityException) { false }
    }

    private fun broadcast(intent: IntentCheckpoint, kind: String, flags: Int): PendingIntent? = PendingIntent.getBroadcast(
        context, 0, Intent(context, ReminderReceiver::class.java).setAction(actionName(kind)).setData(identity(intent, kind)),
        flags or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        private const val CHANNEL = "intentions"
        private const val NOTIFICATION_ID = 1
        private const val IMMUTABLE_UPDATE = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        fun actionName(kind: String) = "com.mashudsck.dealghorithm.reminder.$kind"
        fun identity(intent: IntentCheckpoint, kind: String): Uri = Uri.Builder().scheme("dealghorithm").authority("reminder")
            .appendPath(intent.generation).appendPath(intent.id).appendPath(intent.revision.toString()).appendPath(kind).build()
    }
}
