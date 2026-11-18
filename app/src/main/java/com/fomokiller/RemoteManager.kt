package com.fomokiller

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

data class FomoNote(
    val title: String,
    val description: String,
    val link: String
)

data class FomoUpdateInfo(
    val remoteVersion: String,
    val downloadUrl: String
)

object RemoteManager {

    private const val GIST_NOTES_URL = "https://gist.githubusercontent.com/Tsukiuser/82ceda41adcabf5062525063c61dff67/raw/350d3a94c8e2d7ac13991e12f0f56b85fb946cbd/fomokillernote"
    private const val GIST_UPDATE_URL = "https://gist.githubusercontent.com/Tsukiuser/3ae7d0246fb8eb98aecd4eccac77d63d/raw/5388eafb9541028186e2b01eea40f8632be2e125/fomokillerupdate"

    var cachedNotes: List<FomoNote> = emptyList()
        private set

    var pendingUpdate: FomoUpdateInfo? = null

    fun clearPendingUpdate() {
        pendingUpdate = null
    }

    fun isInternetConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun isWifiConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun fetchNotesIfAvailable(context: Context, onComplete: ((List<FomoNote>) -> Unit)? = null) {
        if (!isInternetConnected(context)) return

        Thread {
            try {
                val url = URL(GIST_NOTES_URL)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    requestMethod = "GET"
                }
                if (connection.responseCode == 200) {
                    val rawText = connection.inputStream.bufferedReader().use { it.readText() }
                    val parsed = parseNotes(rawText)
                    cachedNotes = parsed
                    onComplete?.invoke(parsed)
                }
            } catch (_: Exception) {}
        }.start()
    }

    fun fetchUpdateIfWifi(context: Context, currentVersion: String, onUpdateAvailable: ((FomoUpdateInfo) -> Unit)? = null) {
        if (!isWifiConnected(context)) return

        Thread {
            try {
                val url = URL(GIST_UPDATE_URL)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    requestMethod = "GET"
                }
                if (connection.responseCode == 200) {
                    val rawText = connection.inputStream.bufferedReader().use { it.readText() }
                    val updateInfo = parseUpdate(rawText)
                    if (updateInfo != null && isNewerVersion(updateInfo.remoteVersion, currentVersion)) {
                        pendingUpdate = updateInfo
                        onUpdateAvailable?.invoke(updateInfo)
                    }
                }
            } catch (_: Exception) {}
        }.start()
    }

    fun getRandomNote(): FomoNote? {
        if (cachedNotes.isEmpty()) return null
        return cachedNotes[Random.nextInt(cachedNotes.size)]
    }

    fun parseNotes(content: String): List<FomoNote> {
        val result = mutableListOf<FomoNote>()
        val quoteRegex = Regex("""^"(.*)"$""")

        // Split messages by blank lines or group quoted lines
        val lines = content.lines().map { it.trim() }
        val currentGroup = mutableListOf<String>()

        for (line in lines) {
            if (line.isEmpty()) {
                if (currentGroup.size >= 3) {
                    result.add(FomoNote(currentGroup[0], currentGroup[1], currentGroup[2]))
                    if (result.size >= 10) break
                }
                currentGroup.clear()
            } else {
                val match = quoteRegex.find(line)
                if (match != null) {
                    currentGroup.add(match.groupValues[1])
                }
            }
        }
        if (currentGroup.size >= 3 && result.size < 10) {
            result.add(FomoNote(currentGroup[0], currentGroup[1], currentGroup[2]))
        }
        return result
    }

    fun parseUpdate(content: String): FomoUpdateInfo? {
        val quoteRegex = Regex("""^"(.*)"$""")
        val quotedLines = mutableListOf<String>()

        for (line in content.lines().map { it.trim() }) {
            val match = quoteRegex.find(line)
            if (match != null) {
                quotedLines.add(match.groupValues[1])
                if (quotedLines.size == 2) break
            }
        }

        return if (quotedLines.size >= 2) {
            FomoUpdateInfo(quotedLines[0], quotedLines[1])
        } else null
    }

    fun isNewerVersion(remote: String, current: String): Boolean {
        val rClean = remote.trim()
        val cClean = current.trim()
        if (rClean == cClean) return false
        val rParts = rClean.split(".").mapNotNull { it.toIntOrNull() }
        val cParts = cClean.split(".").mapNotNull { it.toIntOrNull() }

        // Si les deux versions ont pu être parsées en numéros (ex: "1.32.114" vs "1.0")
        if (rParts.isNotEmpty() && cParts.isNotEmpty()) {
            val maxLen = maxOf(rParts.size, cParts.size)
            for (i in 0 until maxLen) {
                val r = rParts.getOrElse(i) { 0 }
                val c = cParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false // Parties numériques identiques (ex: 1.0 vs 1.0.0)
        }
        return rClean != cClean
    }
}

