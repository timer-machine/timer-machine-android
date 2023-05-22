package xyz.aprildown.timer.app.timer.one.layout

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.view.forEachIndexed
import com.github.deweyreed.tools.anko.dp
import xyz.aprildown.chromemenu.AbstractAppMenuPropertiesDelegate
import xyz.aprildown.chromemenu.AppMenuButtonHelper
import xyz.aprildown.chromemenu.AppMenuHandler
import xyz.aprildown.timer.app.base.data.PreferenceData
import xyz.aprildown.timer.app.base.utils.produceTime
import xyz.aprildown.timer.app.timer.one.R

private fun Long.produceActionString(): String {
    return "%s %s".format(if (this >= 0) "+" else "-", this.produceTime())
}

internal class TweakTimeLayout(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    interface Callback {
        fun onTweakTime(amount: Long)
    }

    private val currentSettings = PreferenceData.TweakTimeSettings(context)

    private val mainButton: Button
    private var secondButton: View? = null
    private var tweakButton: View? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER

        mainButton = View.inflate(context, R.layout.layout_tweak_time_main_button, null) as Button
        mainButton.text = currentSettings.mainTime.produceActionString()
        addView(mainButton)

        if (currentSettings.hasSecond) {
            val button =
                View.inflate(context, R.layout.layout_tweak_time_main_button, null) as Button
            button.text = currentSettings.secondTime.produceActionString()
            addView(button)
            secondButton = button
        }

        if (currentSettings.hasSlot) {
            val button = View.inflate(context, R.layout.layout_tweak_time_tweak_button, null)
            val size = context.dp(36).toInt()
            addView(button, LayoutParams(size, size))
            tweakButton = button
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setCallback(activity: Activity, callback: Callback) {
        mainButton.setOnClickListener { callback.onTweakTime(currentSettings.mainTime) }
        secondButton?.setOnClickListener { callback.onTweakTime(currentSettings.secondTime) }

        val slots = currentSettings.slots
        val delegate = object : AbstractAppMenuPropertiesDelegate() {
            override fun prepareMenu(menu: Menu) {
                menu.forEachIndexed { index, item ->
                    val time = slots[index]
                    if (time != 0L) {
                        item.isVisible = true
                        item.title = time.produceActionString()
                    } else {
                        item.isVisible = false
                    }
                }
            }

            override fun onMenuItemClicked(item: MenuItem) {
                val index = item.order
                if (index in slots.indices) {
                    callback.onTweakTime(slots[index])
                }
            }
        }
        tweakButton?.setOnTouchListener(
            AppMenuButtonHelper(
                AppMenuHandler(activity, delegate, R.menu.one_tweak_time)
            )
        )
    }
}
