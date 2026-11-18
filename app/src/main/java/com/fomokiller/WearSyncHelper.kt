package com.fomokiller

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

object WearSyncHelper {
    private const val TAG = "WearSyncHelper"
    const val PATH_SET_MODE = "/set_mode"
    const val PATH_MODE_UPDATED = "/mode_updated"
    const val PATH_REQUEST_MODE = "/request_mode"
    const val DATA_PATH_MODE = "/fomo_mode"
    const val DATA_PATH_REQUEST = "/fomo_request_mode"
    const val KEY_MODE = "mode"
    const val KEY_TIMESTAMP = "timestamp"

    fun broadcastModeToWear(context: Context, mode: FomoMode) {
        val payload = mode.name.toByteArray(Charsets.UTF_8)

        // 1. Synchronisation via DataClient (persistant et automatique via Google Play Services)
        try {
            val putDataMapReq = PutDataMapRequest.create(DATA_PATH_MODE).apply {
                dataMap.putString(KEY_MODE, mode.name)
                dataMap.putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            }
            val request = putDataMapReq.asPutDataRequest().setUrgent()
            Wearable.getDataClient(context).putDataItem(request)
                .addOnSuccessListener {
                    Log.d(TAG, "Mode synchronized to Wear via DataClient: $mode")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "DataClient putDataItem warning: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in DataClient sync: ${e.message}")
        }

        // 2. Diffusion directe via MessageClient à tous les nœuds connectés
        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                Log.d(TAG, "Found ${nodes.size} connected nodes")
                for (node in nodes) {
                    Wearable.getMessageClient(context)
                        .sendMessage(node.id, PATH_MODE_UPDATED, payload)
                        .addOnSuccessListener {
                            Log.d(TAG, "Sent mode update to ${node.displayName} (${node.id})")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to send mode to ${node.displayName}: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to get connected nodes: ${e.message}")
            }
    }
}
