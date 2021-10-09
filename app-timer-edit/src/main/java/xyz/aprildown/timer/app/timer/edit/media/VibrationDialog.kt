package xyz.aprildown.timer.app.timer.edit.media

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.CompoundButton
import android.widget.ImageButton
import androidx.annotation.IdRes
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doOnTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import xyz.aprildown.timer.app.base.media.VibrateHelper
import xyz.aprildown.timer.app.timer.edit.R
import xyz.aprildown.timer.app.timer.edit.databinding.DialogVibrationCountBinding
import xyz.aprildown.timer.app.timer.edit.databinding.DialogVibrationPatternBinding
import xyz.aprildown.timer.domain.entities.VibrationAction
import xyz.aprildown.tools.helper.onImeActionClick
import xyz.aprildown.timer.app.base.R as RBase
import xyz.aprildown.tools.R as RTools

internal class VibrationDialog(private val context: Context) {
    fun showPickPatternDialog(
        pattern: VibrationAction.VibrationPattern,
        onPositive: (VibrationAction.VibrationPattern) -> Unit
    ) {
        val binding = DialogVibrationPatternBinding.inflate(LayoutInflater.from(context))
        var newPattern = pattern

        MaterialAlertDialogBuilder(context)
            .setTitle(RBase.string.vibration_pattern)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                onPositive.invoke(newPattern)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()

        fun getRadioAndButton(@IdRes id: Int): Pair<CompoundButton, ImageButton> {
            val layout = binding.root.findViewById<ViewGroup>(id)
            return layout.getChildAt(0) as CompoundButton to layout.getChildAt(1) as ImageButton
        }

        val (shortRadio, shortButton) = getRadioAndButton(R.id.frameVibrationPatternShort)
        val (normalRadio, normalButton) = getRadioAndButton(R.id.frameVibrationPatternNormal)
        val (longRadio, longButton) = getRadioAndButton(R.id.frameVibrationPatternLong)

        when (pattern) {
            is VibrationAction.VibrationPattern.Short -> shortRadio
            is VibrationAction.VibrationPattern.Normal -> normalRadio
            is VibrationAction.VibrationPattern.Long -> longRadio
        }.isChecked = true

        shortRadio.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                newPattern = VibrationAction.VibrationPattern.Short()
                normalRadio.isChecked = false
                longRadio.isChecked = false
            }
        }
        shortButton.setOnClickListener {
            shortRadio.isChecked = true
            VibrateHelper.start(context, newPattern.twicePattern, repeat = false)
        }
        normalRadio.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                newPattern = VibrationAction.VibrationPattern.Normal()
                shortRadio.isChecked = false
                longRadio.isChecked = false
            }
        }
        normalButton.setOnClickListener {
            normalRadio.isChecked = true
            VibrateHelper.start(context, newPattern.twicePattern, repeat = false)
        }
        longRadio.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                newPattern = VibrationAction.VibrationPattern.Long()
                shortRadio.isChecked = false
                normalRadio.isChecked = false
            }
        }
        longButton.setOnClickListener {
            longRadio.isChecked = true
            VibrateHelper.start(context, newPattern.twicePattern, repeat = false)
        }
    }

    fun showCountDialog(oldCount: Int, func: (Int) -> Unit) {
        val builder = MaterialAlertDialogBuilder(context)
            .setPositiveButton(RTools.string.ok, null)
            .setNegativeButton(RTools.string.cancel, null)

        val binding = DialogVibrationCountBinding.inflate(LayoutInflater.from(context))
        binding.edit.setText(oldCount.toString()) // Put it here to make selectAllOnFocus work
        binding.edit.requestFocus()

        builder.setView(binding.root)

        val dialog = builder.create()
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        dialog.show()
        dialog.setOnDismissListener {
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        }

        binding.textInput.helperText = context.getString(
            RBase.string.vibration_count_desp_template,
            context.getString(RBase.string.vibration_count_desp_0),
            context.getString(RBase.string.vibration_count_desp_range)
        )

        val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

        binding.edit.doOnTextChanged { text, _, _, _ ->
            val currentCount = text?.toString()?.toIntOrNull()
            if (currentCount != null && currentCount in 0..100) {
                binding.textInput.error = null
                positiveButton.isEnabled = true
            } else {
                binding.textInput.error = context.getString(RBase.string.vibration_count_desp_range)
                positiveButton.isEnabled = false
            }
        }
        binding.edit.onImeActionClick(EditorInfo.IME_ACTION_DONE) {
            positiveButton.performClick()
        }

        positiveButton.setOnClickListener {
            val newCount = binding.edit.text?.toString()?.toIntOrNull()
            if (newCount != null && newCount in 0..100) {
                dialog.dismiss()
                func.invoke(newCount)
            } else {
                binding.textInput.error = context.getString(RBase.string.vibration_count_desp_range)
                positiveButton.isEnabled = false
            }
        }
    }
}
