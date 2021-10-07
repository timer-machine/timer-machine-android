package xyz.aprildown.timer.app.timer.edit.voice

import android.content.Context
import android.view.LayoutInflater
import android.view.WindowManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import xyz.aprildown.timer.app.base.data.PreferenceData.useVoiceContent2
import xyz.aprildown.timer.app.timer.edit.R
import xyz.aprildown.timer.app.timer.edit.databinding.DialogVoiceVariableBinding
import xyz.aprildown.timer.app.timer.edit.media.VoiceDialog
import xyz.aprildown.timer.domain.entities.VoiceAction
import xyz.aprildown.tools.helper.safeSharedPreference
import xyz.aprildown.tools.helper.setTextAndSelectEnd

internal class VoiceVariableDialog(private val context: Context) {
    fun show(
        initialAction: VoiceAction,
        onGet: (String) -> Unit,
        onGet2: (String) -> Unit,
    ) {
        val builder = MaterialAlertDialogBuilder(
            context,
            R.style.Widget_AppTheme_MaterialAlertDialog_VoiceVariable
        )
            .setCancelable(false)

        val binding = DialogVoiceVariableBinding.inflate(LayoutInflater.from(context))
        val edit = binding.content.binding.edit
        edit.requestFocus()

        fun getInput(): String {
            return edit.text.toString().trim()
        }

        builder.setView(binding.root)

        val dialog = builder.create()
        dialog.window?.run {
            setBackgroundDrawableResource(android.R.color.transparent)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }
        dialog.show()
        dialog.setOnDismissListener {
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        }

        edit.setTextAndSelectEnd(initialAction.content2)

        binding.content.binding.btnDisableVariables.setOnClickListener {
            dialog.dismiss()
            context.safeSharedPreference.useVoiceContent2 = false
            VoiceDialog(context).requestVoiceContent(
                initialAction.copy(content2 = getInput()),
                onGet,
                onGet2
            )
        }

        binding.content.binding.layoutButtons.btnConfirm.setOnClickListener {
            onGet2.invoke(getInput())
            dialog.dismiss()
        }
        binding.content.binding.layoutButtons.btnCancel.setOnClickListener {
            dialog.dismiss()
        }
    }
}
