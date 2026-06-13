package com.fomokiller

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class FomoNotificationService : NotificationListenerService() {

    companion object {
        private const val TAG = "FomoKiller"
        const val ACTION_STATE_CHANGED = "com.fomokiller.STATE_CHANGED"
    }

    override fun onCreate() {
        super.onCreate()
        AppState.init(applicationContext)
        Log.d(TAG, "NotificationListenerService created")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Listener connected")
        // Notify UI that service is active
        sendBroadcast(Intent(ACTION_STATE_CHANGED))
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Listener disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        val pkg = sbn.packageName ?: return

        // Don't touch our own notifications
        if (pkg == packageName) return

        if (AppState.shouldBlockNotification(pkg)) {
            Log.d(TAG, "Cancelling notification from $pkg (mode: ${AppState.currentMode})")
            try {
                cancelNotification(sbn.key)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cancel notification: ${e.message}")
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Nothing to do — we're the ones removing them
    }
}
