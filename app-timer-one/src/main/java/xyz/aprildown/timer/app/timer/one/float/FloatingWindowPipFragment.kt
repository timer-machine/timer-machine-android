package xyz.aprildown.timer.app.timer.one.float

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import xyz.aprildown.timer.app.base.data.FloatingWindowPip
import xyz.aprildown.timer.app.base.utils.produceTime
import xyz.aprildown.timer.app.timer.one.R
import xyz.aprildown.timer.component.key.ListItemWithLayout
import xyz.aprildown.timer.app.base.R as RBase

class FloatingWindowPipFragment : Fragment(R.layout.fragment_floating_window_pip) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = view.context

        val floatingWindowPip = FloatingWindowPip(context)

        val itemAutoClose =
            view.findViewById<ListItemWithLayout>(R.id.itemFloatingWindowPipAutoClose)
        val switchAutoClose = itemAutoClose.getLayoutView<CompoundButton>()

        fun updateAutoCloseText(enabled: Boolean) {
            itemAutoClose.listItem.setSecondaryText(
                if (enabled) {
                    RBase.string.settings_floating_window_pip_auto_close_on
                } else {
                    RBase.string.settings_floating_window_pip_auto_close_off
                }
            )
        }

        floatingWindowPip.autoClose.let {
            switchAutoClose.isChecked = it
            updateAutoCloseText(it)
        }
        switchAutoClose.setOnCheckedChangeListener { _, isChecked ->
            floatingWindowPip.autoClose = isChecked
            updateAutoCloseText(isChecked)
        }

        val floatingView = view.findViewById<View>(R.id.viewFloatingWindowPipFloating)
        floatingView.setUpFloatingTimerView()
        floatingView.alpha = floatingWindowPip.floatingWindowAlpha
        val seekAlpha = view.findViewById<SeekBar>(R.id.seekFloatingWindowPipFloatingAlpha)
        seekAlpha.progress = (floatingWindowPip.floatingWindowAlpha * 100).toInt()
        seekAlpha.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val alpha = progress / 100f
                    floatingView.alpha = alpha
                    floatingWindowPip.floatingWindowAlpha = alpha
                }
            }
        })

        val seekSize = view.findViewById<SeekBar>(R.id.seekFloatingWindowPipFloatingSize)
        seekSize.progress = (floatingWindowPip.floatingWindowSize * 100).toInt()

        fun updateFloatingSize(percent: Float) {
            floatingView.updateLayoutParams<ViewGroup.LayoutParams> {
                val (w, h) = floatingWindowPip.calculateFloatingWindowSize(percent)
                width = w
                height = h
            }
        }
        updateFloatingSize(floatingWindowPip.floatingWindowSize)
        seekSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val percent = progress / 100f
                    updateFloatingSize(percent)
                    floatingWindowPip.floatingWindowSize = percent
                }
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun View.setUpFloatingTimerView() {
        (this as CardView).cardElevation = 0f
        findViewById<TextView>(R.id.textFloatingTitle).text = "Timer"
        findViewById<TextView>(R.id.textFloatingLoop).text = "1/3"
        findViewById<TextView>(R.id.textFloatingStepName).text = "Step Name"
        findViewById<TextView>(R.id.textFloatingTime).text = 0L.produceTime()
    }
}
