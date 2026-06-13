package com.fomokiller

import android.content.Context
import android.content.SharedPreferences

/**
 * FomoKiller modes:
 * - OFF      : Service is active but lets ALL notifications through
 * - KILL_ALL : Blocks ALL notifications from selected apps
 * - VIP_ONLY : Only lets through "impossible to block" system notifs + user-defined VIP apps
 */
enum class FomoMode {
    OFF,        // No filtering — all notifs pass
    KILL_ALL,   // Block all notifs from blocked apps
    VIP_ONLY    // Only calls, SMS, alarms + VIP apps pass through
}

object AppState {
    private const val PREFS_NAME = "fomokiller_prefs"
    private const val KEY_MODE = "mode"
    private const val KEY_BLOCKED_APPS = "blocked_apps"
    private const val KEY_VIP_APPS = "vip_apps"

    // System packages that are always allowed in VIP_ONLY mode
    // (calls, SMS, alarms, system alerts)
    val ALWAYS_ALLOWED_PACKAGES = setOf(
        "com.android.phone",
        "com.android.incallui",
        "com.android.server.telecom",
        "com.android.dialer",
        "com.google.android.dialer",
        "com.samsung.android.incallui",
        "com.android.mms",
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.android.deskclock",
        "com.google.android.deskclock",
        "com.samsung.android.app.clockpackage",
        "android",
        "com.android.systemui",
        "com.android.settings"
    )

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var currentMode: FomoMode
        get() = FomoMode.valueOf(prefs.getString(KEY_MODE, FomoMode.OFF.name) ?: FomoMode.OFF.name)
        set(value) = prefs.edit().putString(KEY_MODE, value.name).apply()

    // Apps to block in KILL_ALL mode
    var blockedApps: Set<String>
        get() = prefs.getStringSet(KEY_BLOCKED_APPS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_BLOCKED_APPS, value).apply()

    // Apps to allow in VIP_ONLY mode (in addition to ALWAYS_ALLOWED)
    var vipApps: Set<String>
        get() = prefs.getStringSet(KEY_VIP_APPS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_VIP_APPS, value).apply()

    fun shouldBlockNotification(packageName: String): Boolean {
        return when (currentMode) {
            FomoMode.OFF -> false
            FomoMode.KILL_ALL -> blockedApps.contains(packageName)
            FomoMode.VIP_ONLY -> {
                // Block if NOT in always-allowed AND NOT in vip apps
                !ALWAYS_ALLOWED_PACKAGES.contains(packageName) && !vipApps.contains(packageName)
            }
        }
    }
}
