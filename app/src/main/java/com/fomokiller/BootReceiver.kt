package com.fomokiller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            // AppState is loaded from SharedPreferences automatically on init
            // The NotificationListenerService will be reconnected by Android automatically
            context?.let { AppState.init(it) }
        }
    }
}
