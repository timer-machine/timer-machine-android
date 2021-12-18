package xyz.aprildown.timer.component.settings

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import xyz.aprildown.timer.app.base.data.PreferenceData
import xyz.aprildown.timer.app.base.utils.produceTime
import xyz.aprildown.timer.component.key.DurationPicker
import xyz.aprildown.timer.component.settings.databinding.DialogTweakTimeBinding
import kotlin.math.abs
import xyz.aprildown.timer.app.base.R as RBase

class TweakTimeDialog {

    private lateinit var items: MutableList<Pair<Boolean, Long>>

    fun show(context: Context, onDone: () -> Unit) {
        val binding = DialogTweakTimeBinding.inflate(LayoutInflater.from(context))
        MaterialAlertDialogBuilder(context)
            .setTitle(RBase.string.tweak_time_title)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val items = itemsToAmounts()
                PreferenceData.TweakTimeSettings.saveNewSettings(
                    context,
                    items[0],
                    items[1],
                    items.subList(2, items.size)
                )
                onDone.invoke()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(RBase.string.disable) { _, _ ->
                PreferenceData.TweakTimeSettings.saveNewSettings(
                    context,
                    60_000L,
                    0L,
                    List(4) { 0L }
                )
                onDone.invoke()
            }
            .show()

        val settings = PreferenceData.TweakTimeSettings(context)
        items = mutableListOf<Pair<Boolean, Long>>().apply {
            val main = settings.mainTime
            add((main >= 0) to abs(main))
            val second = settings.secondTime
            add((second >= 0) to abs(second))
            addAll(settings.slots.map { (it >= 0) to abs(it) })
        }

        binding.layoutTweakTimeMain.root.setUpWithIndex(0)
        binding.layoutTweakTimeSecond.root.setUpWithIndex(1)

        binding.layoutTweakTimeOverflow1.root.setUpWithIndex(2)
        binding.layoutTweakTimeOverflow2.root.setUpWithIndex(3)
        binding.layoutTweakTimeOverflow3.root.setUpWithIndex(4)
        binding.layoutTweakTimeOverflow4.root.setUpWithIndex(5)
    }

    private fun View.setUpWithIndex(index: Int) {
        findViewById<MaterialButton>(R.id.btnAddTimeSettingItem).run {
            fun changeImage(positive: Boolean) {
                setIconResource(if (positive) RBase.drawable.ic_add else RBase.drawable.ic_minus)
            }
            changeImage(items[index].first)
            setOnClickListener {
                val oldItem = items[index]
                val newSign = !oldItem.first
                changeImage(newSign)
                items[index] = oldItem.copy(first = newSign)
            }
        }
        findViewById<TextView>(R.id.textAddTimeSettingItem).run {
            text = items[index].second.produceTime()
            setOnClickListener {
                DurationPicker(context) { hours, minutes, seconds ->
                    val time = (hours * 3600L + minutes * 60L + seconds) * 1000L
                    items[index] = items[index].copy(second = time)
                    this@run.text = time.produceTime()
                }.show()
            }
        }
    }

    private fun itemsToAmounts(): List<Long> {
        return items.map { if (it.first) it.second else -it.second }
    }
}
