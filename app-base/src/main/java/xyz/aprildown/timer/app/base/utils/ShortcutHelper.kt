package xyz.aprildown.timer.app.base.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutManager
import android.os.Build
import androidx.core.content.getSystemService
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import xyz.aprildown.timer.app.base.R
import xyz.aprildown.tools.anko.longToast
import xyz.aprildown.tools.helper.pendingActivityIntent

object ShortcutHelper {
    fun addTimerShortcut(
        timerId: Int,
        context: Context,
        shortcutName: String,
        intent: Intent,
        shortcutCreatedIntent: Intent
    ) {
        if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            try {
                ShortcutManagerCompat.requestPinShortcut(
                    context,
                    ShortcutInfoCompat.Builder(context, timerId.toString())
                        .setShortLabel(shortcutName)
                        .setLongLabel(shortcutName)
                        .setDisabledMessage(context.getString(R.string.shortcut_disabled))
                        .setIntent(intent)
                        .setIcon(IconCompat.createWithResource(context, R.drawable.shortcut_timer))
                        .build(),
                    context.pendingActivityIntent(shortcutCreatedIntent, timerId).intentSender
                )
            } catch (_: IllegalArgumentException) {
                // if a shortcut with the same ID exists and is disabled.
                context.longToast(R.string.shortcut_remove_old)
            }
        } else {
            context.longToast(R.string.shortcut_not_support)
        }
    }

    fun disableTimerShortcut(context: Context, id: Int): Unit = with(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val shortcutId = id.toString()
            val sm = getSystemService<ShortcutManager>() ?: return@with
            if (sm.pinnedShortcuts.any { it.id == shortcutId }) {
                sm.disableShortcuts(listOf(shortcutId))
            }
        }
    }
}
