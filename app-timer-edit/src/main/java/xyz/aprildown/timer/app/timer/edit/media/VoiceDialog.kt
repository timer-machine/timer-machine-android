package xyz.aprildown.timer.app.timer.edit.media

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.getSystemService
import com.github.deweyreed.tools.helper.gone
import com.github.deweyreed.tools.helper.onImeActionClick
import com.github.deweyreed.tools.helper.setTextAndSelectEnd
import com.github.deweyreed.tools.helper.show
import com.github.deweyreed.tools.helper.showActionAndMultiLine
import com.github.zawadz88.materialpopupmenu.MaterialPopupMenuBuilder
import com.github.zawadz88.materialpopupmenu.popupMenu
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import xyz.aprildown.timer.app.base.data.PreferenceData.useVoiceContent2
import xyz.aprildown.timer.app.timer.edit.R
import xyz.aprildown.timer.app.timer.edit.databinding.DialogVoiceContentBinding
import xyz.aprildown.timer.app.timer.edit.voice.VoiceVariableDialog
import xyz.aprildown.timer.domain.TimeUtils
import xyz.aprildown.timer.domain.entities.VoiceAction
import xyz.aprildown.tools.helper.safeSharedPreference
import xyz.aprildown.timer.app.base.R as RBase

internal class VoiceDialog(private val context: Context) {

    fun requestVoiceContent(
        initialAction: VoiceAction,
        onGet: (String) -> Unit,
        onGet2: (String) -> Unit,
    ) {
        val builder = MaterialAlertDialogBuilder(context)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)

        val binding = DialogVoiceContentBinding.inflate(LayoutInflater.from(context))
        binding.edit.requestFocus()

        builder.setView(binding.root)

        val dialog = builder.create()
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        dialog.show()
        dialog.setOnDismissListener {
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        }

        val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        binding.edit.setTextAndSelectEnd(initialAction.content)
        binding.edit.onImeActionClick(EditorInfo.IME_ACTION_DONE) {
            positiveButton.performClick()
        }
        binding.edit.showActionAndMultiLine(EditorInfo.IME_ACTION_DONE)

        binding.text.text = buildString {
            append(context.getString(RBase.string.voice_content_info_empty))
            append("\n\n")
            append(context.getString(RBase.string.voice_content_info_variables_intro))
        }

        binding.btnNewVariables.setOnClickListener {
            dialog.dismiss()
            context.safeSharedPreference.useVoiceContent2 = true
            VoiceVariableDialog(context).show(
                initialAction.copy(content = binding.edit.text.toString()),
                onGet,
                onGet2
            )
        }

        binding.btnVariables.setOnClickListener {
            showVoiceVariable(binding, it)
        }
        positiveButton.setOnClickListener {
            onGet.invoke(binding.edit.text.toString())

            dialog.dismiss()
        }
    }

    private fun showVoiceVariable(binding: DialogVoiceContentBinding, anchor: View) {
        context.getSystemService<InputMethodManager>()
            ?.hideSoftInputFromWindow(binding.edit.windowToken, 0)
        popupMenu {
            dropdownGravity = Gravity.START or Gravity.TOP

            section {
                customItem {
                    layoutResId = R.layout.layout_voice_variable
                    viewBoundCallback = { view ->
                        view.findViewById<TextView>(android.R.id.title).gone()
                        view.findViewById<TextView>(android.R.id.content)
                            .setText(RBase.string.voice_content_group_variables)
                    }
                }
            }

            val groupVariable = context.getString(RBase.string.voice_group_variable)
            fun MaterialPopupMenuBuilder.SectionHolder.addItem(
                replacer: String,
                desp: String,
                isGroupVariable: Boolean = false
            ) {
                customItem {
                    layoutResId = R.layout.layout_voice_variable
                    viewBoundCallback = { view ->
                        view.findViewById<TextView>(android.R.id.title).run {
                            show()
                            text = replacer
                        }
                        view.findViewById<TextView>(android.R.id.content).text =
                            if (isGroupVariable) {
                                "($groupVariable)$desp"
                            } else {
                                desp
                            }
                    }
                    callback = {
                        binding.edit.text?.insert(binding.edit.selectionStart, replacer)
                    }
                }
            }

            section {
                addItem(
                    replacer = VoiceAction.REPLACER_LOOP,
                    desp = context.getString(RBase.string.voice_loop),
                    isGroupVariable = true
                )
                addItem(
                    replacer = VoiceAction.REPLACER_TOTAL_LOOP,
                    desp = context.getString(RBase.string.voice_total_loop),
                    isGroupVariable = true
                )
                addItem(
                    replacer = VoiceAction.REPLACER_STEP_NAME,
                    desp = context.getString(RBase.string.voice_step_name)
                )
                addItem(
                    replacer = VoiceAction.REPLACER_STEP_DURATION,
                    desp = context.getString(RBase.string.voice_step_duration)
                )
            }
            section {
                title = context.getString(RBase.string.time_panel_elapsed_time)
                addItem(
                    replacer = VoiceAction.REPLACER_ELAPSED_TIME,
                    desp = context.getString(RBase.string.time_panel_elapsed_time)
                )
                addItem(
                    replacer = VoiceAction.REPLACER_ELAPSED_TIME_PERCENT,
                    desp = context.getString(RBase.string.time_panel_elapsed_percent)
                )
                addItem(
                    replacer = VoiceAction.REPLACER_ELAPSED_TIME_GROUP,
                    desp = context.getString(RBase.string.time_panel_elapsed_time),
                    isGroupVariable = true
                )
                addItem(
                    replacer = VoiceAction.REPLACER_ELAPSED_TIME_PERCENT_GROUP,
                    desp = context.getString(RBase.string.time_panel_elapsed_percent),
                    isGroupVariable = true
                )
            }
            section {
                title = context.getString(RBase.string.time_panel_remaining_time)

                addItem(
                    replacer = VoiceAction.REPLACER_REMAINING_TIME,
                    desp = context.getString(RBase.string.time_panel_remaining_time)
                )
                addItem(
                    replacer = VoiceAction.REPLACER_REMAINING_TIME_PERCENT,
                    desp = context.getString(RBase.string.time_panel_remaining_percent)
                )
                addItem(
                    replacer = VoiceAction.REPLACER_REMAINING_TIME_GROUP,
                    desp = context.getString(RBase.string.time_panel_remaining_time),
                    isGroupVariable = true
                )
                addItem(
                    replacer = VoiceAction.REPLACER_REMAINING_TIME_PERCENT_GROUP,
                    desp = context.getString(RBase.string.time_panel_remaining_percent),
                    isGroupVariable = true
                )
            }
            section {
                title = context.getString(RBase.string.voice_time_variable)
                addItem(
                    replacer = VoiceAction.REPLACER_CURRENT_TIME,
                    desp = context.getString(
                        RBase.string.voice_current_time,
                        TimeUtils.formattedTodayTime(context, 12, 34)
                    )
                )
                addItem(
                    replacer = VoiceAction.REPLACER_STEP_END_TIME,
                    desp = context.getString(RBase.string.time_panel_step_end_time)
                )
                addItem(
                    replacer = VoiceAction.REPLACER_TIMER_END_TIME,
                    desp = context.getString(RBase.string.time_panel_timer_end_time)
                )
                addItem(
                    replacer = VoiceAction.REPLACER_GROUP_END_TIME,
                    desp = context.getString(RBase.string.voice_group_end_time),
                    isGroupVariable = true
                )
            }
        }
            .apply {
                setOnDismissListener {
                    binding.edit.requestFocus()
                }
            }
            .show(context, anchor)
    }
}
