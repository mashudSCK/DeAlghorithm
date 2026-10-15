package com.mashudsck.dealghorithm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val uri = intent.data ?: return
        val parts = uri.pathSegments
        if (uri.scheme != "dealghorithm" || uri.host != "reminder" || parts.size != 4) return
        val kind = parts[3]
        val revision = parts[2].toLongOrNull() ?: return
        if (kind !in setOf("ALARM", "ADD", "FINISH", "DISMISS") || intent.action != ReminderScheduler.actionName(kind)) return
        val pending = goAsync()
        (context.applicationContext as DeAlghorithmApp).runtime.callback(parts[0], parts[1], revision, kind)
            .invokeOnCompletion { pending?.finish() }
    }
}
