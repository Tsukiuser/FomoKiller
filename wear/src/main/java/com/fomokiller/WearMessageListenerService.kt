package com.fomokiller

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class WearMessageListenerService : WearableListenerService() {

    companion object {
        private const val TAG = "WearMsgListener"
        const val PREFS_NAME = "fomo_wear_prefs"
        const val KEY_ACTIVE_MODE = "active_mode"
        const val ACTION_WEAR_MODE_CHANGED = "com.fomokiller.wear.MODE_CHANGED"
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        if (messageEvent.path == "/mode_updated") {
            val modeStr = String(messageEvent.data, Charsets.UTF_8)
            handleModeReceived(modeStr)
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/fomo_mode") {
                try {
                    val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                    val modeStr = dataMap.getString("mode")
                    if (modeStr != null) {
                        handleModeReceived(modeStr)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in onDataChanged: ${e.message}")
                }
            }
        }
    }

    private fun handleModeReceived(modeStr: String) {
        try {
            val mode = FomoMode.valueOf(modeStr)
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_ACTIVE_MODE, mode.name).apply()

            // Notifier l'activité si elle est ouverte
            sendBroadcast(Intent(ACTION_WEAR_MODE_CHANGED).apply {
                setPackage(packageName)
                putExtra("mode", mode.name)
            })
            Log.d(TAG, "Mode updated in wear prefs: $mode")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse mode: ${e.message}")
        }
    }
}
