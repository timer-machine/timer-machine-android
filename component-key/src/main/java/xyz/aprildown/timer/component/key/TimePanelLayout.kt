package xyz.aprildown.timer.component.key

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.TooltipCompat
import androidx.collection.arrayMapOf
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.chip.Chip
import xyz.aprildown.timer.app.base.data.PreferenceData.TimePanel
import xyz.aprildown.tools.helper.drawable

class TimePanelLayout(
    context: Context,
    attrs: AttributeSet? = null
) : FlexboxLayout(context, attrs) {

    private val panelMap = arrayMapOf<TimePanel, Chip>()

    init {
        flexWrap = FlexWrap.WRAP
        setShowDivider(SHOW_DIVIDER_MIDDLE)
        setDividerDrawable(context.drawable(R.drawable.divider_normal_flexbox))
    }

    fun setPanels(panels: List<TimePanel>) {
        panelMap.clear()
        removeAllViews()

        val context = context
        panels.forEach {
            val chip = View.inflate(context, R.layout.item_time_panel, null) as Chip
            chip.setChipIconResource(it.iconRes)
            chip.setChipIconTintResource(it.colorRes)
            chip.setChipStrokeColorResource(it.colorRes)
            chip.text = it.formatText(context, .0)
            TooltipCompat.setTooltipText(chip, context.getString(it.despRes))
            addView(chip)
            panelMap[it] = chip
        }
    }

    fun updateText(timePanel: TimePanel, data: Double) {
        panelMap[timePanel]?.let { chip ->
            chip.text = timePanel.formatText(context, data)
        }
    }
}
