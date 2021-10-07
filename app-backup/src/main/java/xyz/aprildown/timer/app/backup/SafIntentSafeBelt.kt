package xyz.aprildown.timer.app.backup

import android.content.Intent
import android.os.Build
import android.provider.DocumentsContract
import android.view.View
import androidx.fragment.app.Fragment
import xyz.aprildown.timer.app.base.data.PreferenceData.lastBackupUri
import xyz.aprildown.timer.domain.utils.AppTracker
import xyz.aprildown.tools.anko.longSnackbar

internal class SafIntentSafeBelt(
    private val fragment: Fragment,
    private val appTracker: AppTracker,
    private val viewForSnackbar: View
) {
    fun drive(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.putExtra(
                DocumentsContract.EXTRA_INITIAL_URI,
                fragment.requireContext().lastBackupUri
            )
        }
        try {
            fragment.startActivityForResult(intent, 0)
        } catch (e: Exception) {
            appTracker.trackError(e)
            onCrash()
        }
    }

    /**
     * On some devices(Mi, etc.), SAF may not work.
     */
    private fun onCrash() {
        viewForSnackbar.longSnackbar(R.string.backup_wrong_saf)
    }
}
