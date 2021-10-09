package xyz.aprildown.timer.app.timer.edit.voice

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.appcompat.widget.TooltipCompatFix
import androidx.core.view.doOnPreDraw
import androidx.core.view.updateLayoutParams
import xyz.aprildown.timer.app.timer.edit.R
import xyz.aprildown.timer.app.timer.edit.databinding.LayoutVoiceVariableTableBinding
import xyz.aprildown.tools.helper.drawable
import xyz.aprildown.timer.app.base.R as RBase

internal class VoiceVariableTableView(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    val binding: LayoutVoiceVariableTableBinding

    val allVariables: List<String>

    var onVariableClicked: ((variable: String) -> Unit)? = null

    init {
        orientation = HORIZONTAL
        dividerDrawable = context.drawable(R.drawable.voice_variable_table_divider)
        showDividers = SHOW_DIVIDER_BEGINNING or SHOW_DIVIDER_MIDDLE or SHOW_DIVIDER_END

        binding = LayoutVoiceVariableTableBinding.inflate(LayoutInflater.from(context), this)

        binding.root.doOnPreDraw {
            binding.variableStepName.updateLayoutParams<MarginLayoutParams> {
                topMargin = binding.variableTimerName.top
            }
            binding.variableStepDuration.updateLayoutParams<MarginLayoutParams> {
                topMargin = binding.variableTimerDuration.top
            }
            binding.variableStepEndTime.updateLayoutParams<MarginLayoutParams> {
                topMargin = binding.variableTimerEndTime.top
            }
        }

        val allVariableViews = listOf(
            binding.variableTimerName,
            binding.variableTimerLoop,
            binding.variableTimerTotalLoop,
            binding.variableTimerDuration,
            binding.variableTimerElapsed,
            binding.variableTimerElapsedPercent,
            binding.variableTimerRemaining,
            binding.variableTimerRemainingPercent,
            binding.variableTimerEndTime,

            binding.variableGroupName,
            binding.variableGroupLoop,
            binding.variableGroupTotalLoop,
            binding.variableGroupDuration,
            binding.variableGroupElapsed,
            binding.variableGroupElapsedPercent,
            binding.variableGroupRemaining,
            binding.variableGroupRemainingPercent,
            binding.variableGroupEndTime,

            binding.variableStepName,
            binding.variableStepDuration,
            binding.variableStepEndTime,

            binding.variableOtherClockTime,
        )

        allVariables = allVariableViews.map { it.text.toString() }

        allVariableViews.forEach { chip ->
            chip.setOnClickListener {
                onVariableClicked?.invoke(chip.text.toString())
            }
        }

        setUpTooltips()
    }

    private fun setUpTooltips() {
        binding.variableTimerName withTooltip RBase.string.voice_variable_timer_name_desp
        binding.variableTimerLoop withTooltip RBase.string.voice_variable_timer_loop_desp
        binding.variableTimerTotalLoop withTooltip RBase.string.voice_variable_timer_total_loop_desp
        binding.variableTimerDuration withTooltip RBase.string.voice_variable_timer_duration_desp
        binding.variableTimerElapsed withTooltip RBase.string.voice_variable_timer_elapsed_desp
        binding.variableTimerElapsedPercent withTooltip RBase.string.voice_variable_timer_elapsed_percent_desp
        binding.variableTimerRemaining withTooltip RBase.string.voice_variable_timer_remaining_desp
        binding.variableTimerRemainingPercent withTooltip RBase.string.voice_variable_timer_remaining_percent_desp
        binding.variableTimerEndTime withTooltip RBase.string.voice_variable_timer_end_time_desp

        binding.variableGroupName withTooltip RBase.string.voice_variable_group_name_desp
        binding.variableGroupLoop withTooltip RBase.string.voice_variable_group_loop_desp
        binding.variableGroupTotalLoop withTooltip RBase.string.voice_variable_group_total_loop_desp
        binding.variableGroupDuration withTooltip RBase.string.voice_variable_group_duration_desp
        binding.variableGroupElapsed withTooltip RBase.string.voice_variable_group_elapsed_desp
        binding.variableGroupElapsedPercent withTooltip RBase.string.voice_variable_group_elapsed_percent_desp
        binding.variableGroupRemaining withTooltip RBase.string.voice_variable_group_remaining_desp
        binding.variableGroupRemainingPercent withTooltip RBase.string.voice_variable_group_remaining_percent_desp
        binding.variableGroupEndTime withTooltip RBase.string.voice_variable_group_end_time_desp

        binding.variableStepName withTooltip RBase.string.voice_variable_step_name_desp
        binding.variableStepDuration withTooltip RBase.string.voice_variable_step_duration_desp
        binding.variableStepEndTime withTooltip RBase.string.voice_variable_step_end_time_desp

        binding.variableOtherClockTime withTooltip RBase.string.voice_variable_other_clock_time_desp
    }

    private infix fun View.withTooltip(@StringRes stringRes: Int) {
        TooltipCompatFix.setTooltipText(this, context.getString(stringRes))
    }
}
