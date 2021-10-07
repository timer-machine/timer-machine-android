package xyz.aprildown.timer.flavor.google.utils

import android.content.Context
import android.view.LayoutInflater
import androidx.annotation.StringRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import xyz.aprildown.timer.flavor.google.databinding.DialogTitleSubscriptionBinding

internal class IapPromotionDialog(private val context: Context) {
    fun show(
        title: CharSequence,
        message: CharSequence,
        @StringRes positiveButtonTextRes: Int,
        onSubscribe: () -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setCustomTitle(
                DialogTitleSubscriptionBinding.inflate(LayoutInflater.from(context)).apply {
                    textTitle.text = title
                }.root
            )
            .setMessage(message)
            .setPositiveButton(positiveButtonTextRes) { _, _ ->
                onSubscribe.invoke()
            }
            .show()
    }
}
