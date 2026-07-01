package com.fomokiller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class FomoActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val modeStr = intent.getStringExtra("mode") ?: return
        
        AppState.init(context)
        val mode = try {
            FomoMode.valueOf(modeStr)
        } catch (e: Exception) {
            return
        }

        AppState.currentMode = mode
        
        // Appliquer le mode au service
        val service = FomoNotificationService.instance
        service?.applyCurrentMode()
        
        // Mettre à jour la tuile (Quick Settings)
        val tileIntent = Intent(context, FomoTileService::class.java)
        context.startService(tileIntent)

        // Notification UI
        context.sendBroadcast(Intent(FomoNotificationService.ACTION_STATE_CHANGED))

        // Message bulle (Toast)
        val message = when (mode) {
            FomoMode.OFF -> context.getString(R.string.mode_off)
            FomoMode.KILL_ALL -> context.getString(R.string.mode_kill_all)
            FomoMode.VIP_ONLY -> context.getString(R.string.mode_vip_only)
        }
        Toast.makeText(context, "$message : OK", Toast.LENGTH_SHORT).show()
    }
}
