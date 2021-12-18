package xyz.aprildown.timer.flavor.google

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import xyz.aprildown.timer.app.base.R as RBase

internal fun Context.showErrorDialog(
    error: BillingSupervisor.Error,
    onDismiss: (() -> Unit)? = null
) {
    MaterialAlertDialogBuilder(this)
        .setMessage(
            buildString {
                when (error) {
                    is BillingSupervisor.Error.SubscriptionNotSupported -> {
                        append(getString(RBase.string.billing_not_supported))
                    }
                    is BillingSupervisor.Error.Message -> {
                        append(getString(RBase.string.billing_connect_issue))
                        val errorContent = error.content
                        if (errorContent.isNotBlank()) {
                            append("\n\n${error.code}: $errorContent")
                        }
                    }
                }
            }
        )
        .setPositiveButton(android.R.string.ok, null)
        .setOnDismissListener {
            onDismiss?.invoke()
        }
        .show()
}
