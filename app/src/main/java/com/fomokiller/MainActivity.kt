package com.fomokiller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.fomokiller.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val serviceStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppState.init(applicationContext)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupButtons()
        updateUI()
    }

    override fun onResume() {
        super.onResume()
        // Register receiver for service connection events
        val filter = IntentFilter(FomoNotificationService.ACTION_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(serviceStateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(serviceStateReceiver, filter)
        }
        updateUI()
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(serviceStateReceiver) } catch (_: Exception) {}
    }

    private fun setupButtons() {
        // Mode OFF button
        binding.btnOff.setOnClickListener {
            if (!isNotificationListenerEnabled()) {
                requestNotificationAccess()
                return@setOnClickListener
            }
            AppState.currentMode = FomoMode.OFF
            updateUI()
        }

        // Mode KILL ALL button
        binding.btnKillAll.setOnClickListener {
            if (!isNotificationListenerEnabled()) {
                requestNotificationAccess()
                return@setOnClickListener
            }
            AppState.currentMode = FomoMode.KILL_ALL
            updateUI()
        }

        // Mode VIP ONLY button
        binding.btnVipOnly.setOnClickListener {
            if (!isNotificationListenerEnabled()) {
                requestNotificationAccess()
                return@setOnClickListener
            }
            AppState.currentMode = FomoMode.VIP_ONLY
            updateUI()
        }

        // Long press KILL ALL to edit blocked apps
        binding.btnKillAll.setOnLongClickListener {
            openAppPicker(mode = "blocked")
            true
        }

        // Long press VIP ONLY to edit VIP apps
        binding.btnVipOnly.setOnLongClickListener {
            openAppPicker(mode = "vip")
            true
        }
    }

    private fun updateUI() {
        val listenerEnabled = isNotificationListenerEnabled()
        val mode = AppState.currentMode

        // Show/hide permission banner
        binding.permissionBanner.visibility = if (!listenerEnabled) View.VISIBLE else View.GONE

        // Update active state for all buttons
        binding.btnOff.isSelected = (mode == FomoMode.OFF)
        binding.btnKillAll.isSelected = (mode == FomoMode.KILL_ALL)
        binding.btnVipOnly.isSelected = (mode == FomoMode.VIP_ONLY)

        // Update blocked/VIP app count hints
        val blockedCount = AppState.blockedApps.size
        val vipCount = AppState.vipApps.size

        binding.labelKillAll.text = if (blockedCount > 0)
            "$blockedCount app${if (blockedCount > 1) "s" else ""} bloquée${if (blockedCount > 1) "s" else ""}"
        else
            "Appui long pour choisir les apps"

        binding.labelVipOnly.text = if (vipCount > 0)
            "$vipCount app${if (vipCount > 1) "s" else ""} autorisée${if (vipCount > 1) "s" else ""} + système"
        else
            "Appui long pour ajouter des apps VIP"
    }

    private fun openAppPicker(mode: String) {
        val intent = Intent(this, AppPickerActivity::class.java)
        intent.putExtra("mode", mode)
        startActivity(intent)
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(pkgName)
    }

    private fun requestNotificationAccess() {
        Toast.makeText(this, "Autorisez l'accès aux notifications", Toast.LENGTH_LONG).show()
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }
}
