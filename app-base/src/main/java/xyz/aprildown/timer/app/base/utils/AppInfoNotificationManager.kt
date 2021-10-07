package xyz.aprildown.timer.app.base.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import xyz.aprildown.timer.app.base.R
import xyz.aprildown.timer.domain.utils.Constants

class AppInfoNotificationManager(private val context: Context) {

    private val nm = NotificationManagerCompat.from(context)

    fun notify(
        @StringRes titleRes: Int,
        @StringRes despRes: Int,
        pendingIntent: PendingIntent
    ) {
        createChannelIfNecessary()

        nm.notify(
            Constants.NOTIF_ID_APP_INFO,
            NotificationCompat.Builder(context, Constants.CHANNEL_APP_INFO_NOTIFICATION)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(titleRes))
                .setContentText(context.getString(despRes))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
        )
    }

    private fun createChannelIfNecessary() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (nm.getNotificationChannel(Constants.CHANNEL_APP_INFO_NOTIFICATION) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        Constants.CHANNEL_APP_INFO_NOTIFICATION,
                        context.getString(R.string.notif_channel_app_info_title),
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = context.getString(R.string.notif_channel_app_info_desp)
                    }
                )
            }
        }
    }
}
