package xyz.aprildown.timer.app.timer.edit.media

import android.content.Context
import android.view.LayoutInflater
import android.widget.CompoundButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import xyz.aprildown.timer.app.timer.edit.databinding.DialogHalfOptionBinding
import xyz.aprildown.timer.domain.entities.HalfAction
import xyz.aprildown.timer.app.base.R as RBase

internal class HalfDialog(private val context: Context) {
    fun showOptionDialog(oldOption: Int, onOption: (Int) -> Unit) {
        val binding = DialogHalfOptionBinding.inflate(LayoutInflater.from(context))
        var newOption = oldOption
        MaterialAlertDialogBuilder(context)
            .setTitle(RBase.string.half_option)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                onOption.invoke(newOption)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()

        val radioVoice = binding.itemHalfOptionVoice.getLayoutView<CompoundButton>().apply {
            tag = HalfAction.OPTION_VOICE
        }
        val radioMusic = binding.itemHalfOptionMusic.getLayoutView<CompoundButton>().apply {
            tag = HalfAction.OPTION_MUSIC
        }
        val radioVibration = binding.itemHalfOptionVibration.getLayoutView<CompoundButton>().apply {
            tag = HalfAction.OPTION_VIBRATION
        }

        val allRadio = listOf(radioVoice, radioMusic, radioVibration)

        val listener = CompoundButton.OnCheckedChangeListener { checkedView, isChecked ->
            if (isChecked) {
                newOption = checkedView.tag as Int
                allRadio.forEach { view ->
                    if (view !== checkedView && view.isChecked) {
                        view.isChecked = false
                    }
                }
            }
        }
        allRadio.forEach {
            if (it.tag == oldOption) {
                it.isChecked = true
            }
            it.setOnCheckedChangeListener(listener)
        }
    }
}
