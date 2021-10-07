package xyz.aprildown.timer.workshop

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatDialog
import xyz.aprildown.timer.workshop.databinding.DialogRateBinding

class RateDialog(
    private var onRate: View.OnClickListener? = null,
    private var onLater: View.OnClickListener? = null,
    private var onNever: View.OnClickListener? = null
) {

    fun show(context: Context) {
        // AppCompatDialog + transparent background to make a perfect dialog width
        val binding = DialogRateBinding.inflate(LayoutInflater.from(context))
        val dialog = AppCompatDialog(context)
        dialog.setCancelable(false)
        dialog.setContentView(binding.root)
        binding.btnRate.setOnClickListener {
            onRate?.onClick(it)
            dialog.dismiss()
        }
        binding.btnLeave.setOnClickListener {
            if (binding.checkNever.isChecked) {
                onNever?.onClick(it)
            } else {
                onLater?.onClick(it)
            }
            dialog.dismiss()
        }
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }
}
