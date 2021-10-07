package xyz.aprildown.timer.app.timer.edit

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.google.android.material.chip.Chip
import xyz.aprildown.timer.app.base.utils.produceTime

internal class StepInfoView(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val durationChip: Chip

    init {

        View.inflate(context, R.layout.view_step_info, this)

        durationChip = findViewById(R.id.chipStepInfoDuration)
    }

    fun setDuration(durationInMilli: Long) {
        durationChip.text = durationInMilli.produceTime()
    }
}
