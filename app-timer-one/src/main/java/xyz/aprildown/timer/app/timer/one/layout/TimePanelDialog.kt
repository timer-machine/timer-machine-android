package xyz.aprildown.timer.app.timer.one.layout

import android.content.Context
import android.view.LayoutInflater
import android.widget.CompoundButton
import androidx.core.view.forEachIndexed
import androidx.core.view.get
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import xyz.aprildown.timer.app.base.data.PreferenceData
import xyz.aprildown.timer.app.base.data.PreferenceData.timePanels
import xyz.aprildown.timer.app.timer.one.databinding.DialogTimePanelPickerBinding
import xyz.aprildown.timer.component.key.ListItemWithLayout
import xyz.aprildown.timer.app.base.R as RBase

internal fun Context.showTimePanelPickerDialog(onDone: () -> Unit) {
    val currentTimePanels = timePanels.toMutableList()

    val binding = DialogTimePanelPickerBinding.inflate(LayoutInflater.from(this))
    MaterialAlertDialogBuilder(this)
        .setTitle(RBase.string.time_panel_picker)
        .setView(binding.root)
        .setPositiveButton(android.R.string.ok) { _, _ ->
            timePanels = currentTimePanels
            onDone.invoke()
        }
        .setNegativeButton(android.R.string.cancel, null)
        .show()

    val allTimePanels = PreferenceData.TimePanel.entries

    binding.layoutTimePanel.setPanels(currentTimePanels)
    currentTimePanels.forEach {
        (binding.layoutTimePanelContainer[it.ordinal] as ListItemWithLayout)
            .getLayoutView<CompoundButton>().isChecked = true
    }

    binding.layoutTimePanelContainer.forEachIndexed { index, view ->
        val listItem = view as ListItemWithLayout
        listItem.listItem.setPrimaryText(allTimePanels[index].despRes)
        listItem.getLayoutView<CompoundButton>().setOnCheckedChangeListener { _, isChecked ->
            val selected = allTimePanels[index]
            if (isChecked) {
                if (!currentTimePanels.contains(selected)) {
                    currentTimePanels.add(selected)
                }
            } else {
                currentTimePanels.remove(selected)
            }
            binding.layoutTimePanel.setPanels(currentTimePanels)
        }
    }
}
