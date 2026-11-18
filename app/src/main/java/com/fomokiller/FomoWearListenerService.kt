package com.fomokiller

import android.content.ComponentName
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService

class FomoWearListenerService : WearableListenerService() {

    companion object {
        private const val TAG = "FomoWearListener"
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        AppState.init(applicationContext)

        when (messageEvent.path) {
            WearSyncHelper.PATH_SET_MODE -> {
                val modeStr = String(messageEvent.data, Charsets.UTF_8)
                try {
                    val mode = FomoMode.valueOf(modeStr)
                    applyNewMode(mode)

                    // Répondre immédiatement au nœud émetteur
                    Wearable.getMessageClient(this).sendMessage(
                        messageEvent.sourceNodeId,
                        WearSyncHelper.PATH_MODE_UPDATED,
                        mode.name.toByteArray(Charsets.UTF_8)
                    )

                    // Synchroniser également avec tous les nœuds et le DataClient
                    WearSyncHelper.broadcastModeToWear(applicationContext, mode)
                    Log.d(TAG, "Mode updated via Wear OS message: $mode from ${messageEvent.sourceNodeId}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse mode from Wear OS: ${e.message}")
                }
            }
            WearSyncHelper.PATH_REQUEST_MODE -> {
                val currentMode = AppState.currentMode
                // Répondre immédiatement au nœud demandeur
                Wearable.getMessageClient(this).sendMessage(
                    messageEvent.sourceNodeId,
                    WearSyncHelper.PATH_MODE_UPDATED,
                    currentMode.name.toByteArray(Charsets.UTF_8)
                )
                WearSyncHelper.broadcastModeToWear(applicationContext, currentMode)
                Log.d(TAG, "Sent current mode ($currentMode) to requesting Wear node ${messageEvent.sourceNodeId}")
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        AppState.init(applicationContext)

        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val path = event.dataItem.uri.path
                if (path == WearSyncHelper.DATA_PATH_REQUEST) {
                    try {
                        val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                        val modeStr = dataMap.getString(WearSyncHelper.KEY_MODE)
                        if (modeStr != null) {
                            val mode = FomoMode.valueOf(modeStr)
                            applyNewMode(mode)
                            WearSyncHelper.broadcastModeToWear(applicationContext, mode)
                            Log.d(TAG, "Mode updated via DataClient: $mode")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing DataClient change: ${e.message}")
                    }
                }
            }
        }
    }

    private fun applyNewMode(mode: FomoMode) {
        if (AppState.currentMode == mode) return
        AppState.currentMode = mode

        val service = FomoNotificationService.instance
        if (service != null) {
            service.applyCurrentMode()
        } else {
            val componentName = ComponentName(this, FomoNotificationService::class.java)
            NotificationListenerService.requestRebind(componentName)
        }

        // Notifier l'activité principale pour mettre à jour l'UI téléphone si ouverte
        sendBroadcast(Intent(FomoNotificationService.ACTION_STATE_CHANGED).apply {
            setPackage(packageName)
        })
    }
}
