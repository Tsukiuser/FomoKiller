package com.fomokiller

import android.content.Context
import android.os.Parcel
import android.service.notification.StatusBarNotification
import android.util.Base64
import android.util.Log
import java.io.File

object NotificationStorage {
    private const val TAG = "NotificationStorage"
    private const val DIR_NAME = "held_notifications"

    private fun getStorageDir(context: Context): File {
        val dir = File(context.filesDir, DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun sanitizeKey(key: String): String {
        return Base64.encodeToString(key.toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)
    }

    fun save(context: Context, sbn: StatusBarNotification) {
        try {
            val dir = getStorageDir(context)
            val file = File(dir, sanitizeKey(sbn.key))
            val parcel = Parcel.obtain()
            try {
                sbn.writeToParcel(parcel, 0)
                val bytes = parcel.marshall()
                file.writeBytes(bytes)
            } finally {
                parcel.recycle()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save notification ${sbn.key}: ${e.message}")
        }
    }

    fun remove(context: Context, key: String) {
        try {
            val dir = getStorageDir(context)
            val file = File(dir, sanitizeKey(key))
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove notification $key: ${e.message}")
        }
    }

    fun clear(context: Context) {
        try {
            val dir = getStorageDir(context)
            dir.listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear notifications: ${e.message}")
        }
    }

    fun loadAll(context: Context): List<StatusBarNotification> {
        val result = mutableListOf<StatusBarNotification>()
        try {
            val dir = getStorageDir(context)
            val files = dir.listFiles() ?: return result
            for (file in files) {
                try {
                    val bytes = file.readBytes()
                    val parcel = Parcel.obtain()
                    try {
                        parcel.unmarshall(bytes, 0, bytes.size)
                        parcel.setDataPosition(0)
                        val sbn = StatusBarNotification(parcel)
                        result.add(sbn)
                    } finally {
                        parcel.recycle()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to read/unparcel file ${file.name}: ${e.message}")
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load notifications: ${e.message}")
        }
        return result
    }
}
