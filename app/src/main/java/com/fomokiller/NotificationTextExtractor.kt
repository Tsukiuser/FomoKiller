package com.fomokiller

import android.app.Notification
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification

object NotificationTextExtractor {

    fun extractFullText(sbn: StatusBarNotification): Pair<String, String> {
        val extras = sbn.notification.extras ?: return Pair("", "")
        return extractFullText(extras)
    }

    fun extractFullText(extras: Bundle): Pair<String, String> {
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val textBuilder = StringBuilder()

        // 1. EXTRA_TEXT standard
        extras.getCharSequence(Notification.EXTRA_TEXT)?.let {
            textBuilder.append(it).append(" ")
        }

        // 2. EXTRA_BIG_TEXT (BigTextStyle)
        extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.let {
            textBuilder.append(it).append(" ")
        }

        // 3. EXTRA_SUMMARY_TEXT (résumé / sous-texte)
        extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.let {
            textBuilder.append(it).append(" ")
        }

        // 4. EXTRA_SUB_TEXT
        extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.let {
            textBuilder.append(it).append(" ")
        }

        // 5. EXTRA_TEXT_LINES (InboxStyle)
        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        if (textLines != null) {
            for (line in textLines) {
                textBuilder.append(line).append(" ")
            }
        }

        // 6. EXTRA_MESSAGES (MessagingStyle)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            if (messages != null) {
                for (msg in messages) {
                    if (msg is Bundle) {
                        msg.getCharSequence("text")?.let { textBuilder.append(it).append(" ") }
                        msg.getCharSequence("sender")?.let { textBuilder.append(it).append(" ") }
                    }
                }
            }
        }

        return Pair(title.trim(), textBuilder.toString().trim())
    }
}
