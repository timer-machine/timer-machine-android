package xyz.aprildown.timer.app.backup

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.DocumentsContract
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import com.github.deweyreed.tools.anko.longSnackbar
import xyz.aprildown.timer.app.base.data.PreferenceData.lastBackupUri
import xyz.aprildown.timer.domain.utils.AppTracker
import xyz.aprildown.timer.app.base.R as RBase

internal class SafIntentSafeBelt(
    private val context: Context,
    private val appTracker: AppTracker,
    private val viewForSnackbar: View
) {
    fun drive(launcher: ActivityResultLauncher<Intent>, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.putExtra(
                DocumentsContract.EXTRA_INITIAL_URI,
                context.lastBackupUri
            )
        }
        try {
            launcher.launch(intent)
        } catch (e: Exception) {
            appTracker.trackError(e)
            onCrash()
        }
    }

    /**
     * On some devices(Mi, etc.), SAF may not work.
     */
    private fun onCrash() {
        viewForSnackbar.longSnackbar(RBase.string.backup_wrong_saf)
    }
}
