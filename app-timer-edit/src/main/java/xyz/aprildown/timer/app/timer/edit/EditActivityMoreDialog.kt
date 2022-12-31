package xyz.aprildown.timer.app.timer.edit

import android.view.LayoutInflater
import android.widget.CompoundButton
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.github.deweyreed.tools.arch.observeEvent
import com.github.deweyreed.tools.helper.gone
import com.github.deweyreed.tools.helper.show
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import xyz.aprildown.timer.app.base.ui.AppNavigator
import xyz.aprildown.timer.app.timer.edit.databinding.DialogEditMoreBinding
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.entities.TimerMoreEntity
import xyz.aprildown.timer.presentation.edit.EditViewModel
import xyz.aprildown.timer.app.base.R as RBase

internal fun EditActivity.showBottomMoreDialog(
    viewModel: EditViewModel,
    appNavigator: AppNavigator
) {
    val binding = DialogEditMoreBinding.inflate(LayoutInflater.from(this))
    // Inflate the view on our own to find ?dividerHorizontal
    val dialog = MaterialAlertDialogBuilder(this)
        .setView(binding.root)
        .setPositiveButton(android.R.string.ok, null)
        .setNegativeButton(android.R.string.cancel, null)
        .show()

    val more = viewModel.more.value ?: TimerMoreEntity()

    // Get views

    val showNotifSwitch = binding.itemEditShowNotif.getLayoutView<CompoundButton>()
    val notifCountItem = binding.itemEditNotifCount
    val notifCountSwitch = notifCountItem.getLayoutView<CompoundButton>()
    val triggerSwitch = binding.itemEditTrigger.getLayoutView<CompoundButton>()
    val triggerText = binding.btnEditTrigger

    // Init views
    showNotifSwitch.isChecked = more.showNotif
    notifCountItem.isEnabled = more.showNotif
    notifCountSwitch.isChecked = more.notifCount

    var selectedTimerId = more.triggerTimerId
    if (selectedTimerId == TimerEntity.NULL_ID) {
        triggerText.gone()
        triggerSwitch.isChecked = false
    } else {
        triggerText.show()
        triggerSwitch.isChecked = true
    }

    // Respond to changes
    showNotifSwitch.setOnCheckedChangeListener { _, isChecked ->
        notifCountItem.isEnabled = isChecked
        if (!isChecked) {
            notifCountItem.getLayoutView<CompoundButton>().isChecked = false
        }
    }
    triggerSwitch.setOnCheckedChangeListener { _, isChecked ->
        triggerText.isVisible = isChecked
    }

    viewModel.requestTimerInfoByTimerId(timerId = selectedTimerId)

    viewModel.timerInfoEvent.observeEvent(this) { timerInfo ->
        triggerText.setOnClickListener { _ ->
            appNavigator.pickTimer(
                fm = supportFragmentManager,
                select = listOf(selectedTimerId)
            ) {
                val newTimerInfo = it.timerInfo.first()
                selectedTimerId = newTimerInfo.id
                triggerText.text = newTimerInfo.name
            }
        }
        if (timerInfo.id == TimerEntity.NULL_ID) {
            triggerText.setText(RBase.string.timer_pick_required)
        } else {
            triggerText.text = timerInfo.name
        }
    }

    dialog.setOnDismissListener {
        viewModel.timerInfoEvent.removeObservers(this)
    }

    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
        val showNotif = showNotifSwitch.isChecked
        val notifCount = notifCountSwitch.isChecked
        val triggerTimerId = if (triggerSwitch.isChecked) selectedTimerId else TimerEntity.NULL_ID
        viewModel.more.value = TimerMoreEntity(
            showNotif = showNotif,
            notifCount = notifCount,
            triggerTimerId = triggerTimerId
        )
        dialog.dismiss()
    }
}
