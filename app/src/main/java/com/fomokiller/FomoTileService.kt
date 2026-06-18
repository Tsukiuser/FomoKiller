package com.fomokiller

import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class FomoTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        AppState.init(this)
        val currentMode = AppState.currentMode
        
        tile.state = if (currentMode != FomoMode.OFF) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val modeText = when (currentMode) {
                FomoMode.KILL_ALL -> getString(R.string.mode_kill_all)
                FomoMode.VIP_ONLY -> getString(R.string.mode_vip_only)
                else -> getString(R.string.tile_subtitle_off)
            }
            tile.subtitle = if (currentMode != FomoMode.OFF) {
                getString(R.string.tile_subtitle_active, modeText)
            } else {
                getString(R.string.tile_subtitle_off)
            }
        }
        
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        AppState.init(this)
        val currentMode = AppState.currentMode
        
        // On récupère le mode cible configuré dans les réglages
        val prefs = getSharedPreferences("fomokiller_prefs", Context.MODE_PRIVATE)
        val targetModeStr = prefs.getString("tile_target_mode", "KILL_ALL") ?: "KILL_ALL"
        val targetMode = FomoMode.valueOf(targetModeStr)

        val newMode = if (currentMode == FomoMode.OFF) targetMode else FomoMode.OFF
        
        AppState.currentMode = newMode
        
        // Notifier le service de notification
        val service = FomoNotificationService.instance
        if (service != null) {
            service.applyCurrentMode()
        }
        
        // Diffuser un broadcast pour l'UI (MainActivity)
        sendBroadcast(Intent(FomoNotificationService.ACTION_STATE_CHANGED))
        
        updateTile()
    }
}
