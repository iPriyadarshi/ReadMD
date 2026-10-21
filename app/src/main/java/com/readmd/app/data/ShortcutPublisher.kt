package com.readmd.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.readmd.app.MainActivity
import com.readmd.app.R
import com.readmd.app.data.recent.RecentEntry
import com.readmd.app.domain.ShortcutLabels

/**
 * Publishes the most recent documents as launcher shortcuts (long-press the app icon).
 * Each shortcut opens its document through the same "view" path as "Open with ReadMD".
 */
class ShortcutPublisher(private val context: Context) {

    /** Replaces the app's dynamic shortcuts with [recents], newest first. */
    fun publish(recents: List<RecentEntry>) {
        // Failures (rate limiting, a locked device) are not worth surfacing; the next change retries.
        runCatching {
            val limit = minOf(ShortcutLabels.MAX_SHORTCUTS, ShortcutManagerCompat.getMaxShortcutCountPerActivity(context))
            val shortcuts = recents.take(limit).map { recent ->
                val open = Intent(context, MainActivity::class.java)
                    .setAction(Intent.ACTION_VIEW)
                    .setData(Uri.parse(recent.id))
                ShortcutInfoCompat.Builder(context, ShortcutLabels.id(recent.id))
                    .setShortLabel(ShortcutLabels.label(recent.name))
                    .setLongLabel(recent.name.ifBlank { ShortcutLabels.label(recent.name) })
                    .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_document))
                    .setIntent(open)
                    .build()
            }
            if (shortcuts.isEmpty()) {
                ShortcutManagerCompat.removeAllDynamicShortcuts(context)
            } else {
                ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
            }
        }
    }
}
