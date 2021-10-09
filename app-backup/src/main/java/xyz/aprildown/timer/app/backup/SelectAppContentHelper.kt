package xyz.aprildown.timer.app.backup

import android.view.View
import android.widget.CompoundButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import xyz.aprildown.tools.helper.gone
import xyz.aprildown.tools.view.ListItemWithLayout
import xyz.aprildown.timer.app.base.R as RBase

internal class SelectAppContentSettings {
    var wipeFirst: Boolean = false
    var isTimersChecked: Boolean = true
    var isTimerStampsChecked: Boolean = true
    var isSchedulersChecked: Boolean = true
    var isSettingsChecked: Boolean = true
}

internal class SelectAppContentHelper {

    val settings = SelectAppContentSettings()

    private lateinit var itemWipe: ListItemWithLayout
    private lateinit var checkTimers: CompoundButton
    private lateinit var checkTimerStamps: CompoundButton
    private lateinit var checkSchedulers: CompoundButton

    fun setUpView(view: View) {
        val context = view.context

        // Get vViews

        itemWipe = view.findViewById(R.id.itemSelectAppContentWipe)
        val checkWipe = itemWipe.getLayoutView<CompoundButton>()
        checkTimers = view.findViewById<ListItemWithLayout>(R.id.itemSelectAppContentTimer)
            .getLayoutView()
        checkTimerStamps =
            view.findViewById<ListItemWithLayout>(R.id.itemSelectAppContentTimerStamp)
                .getLayoutView()
        checkSchedulers = view.findViewById<ListItemWithLayout>(R.id.itemSelectAppContentScheduler)
            .getLayoutView()
        val checkSettings = view.findViewById<ListItemWithLayout>(R.id.itemSelectAppContentSettings)
            .getLayoutView<CompoundButton>()

        // Init views
        checkWipe.setOnCheckedChangeListener(null)
        checkWipe.isChecked = settings.wipeFirst
        checkTimers.isChecked = settings.isTimersChecked
        checkTimerStamps.isChecked = settings.isTimerStampsChecked
        checkSchedulers.isChecked = settings.isSchedulersChecked
        checkSettings.isChecked = settings.isSettingsChecked

        // Respond to changes
        checkWipe.setOnCheckedChangeListener { _, isChecked ->
            if (settings.wipeFirst != isChecked) {
                settings.wipeFirst = isChecked
                if (isChecked) {
                    MaterialAlertDialogBuilder(context)
                        .setCancelable(false)
                        .setMessage(RBase.string.import_wipe_warning)
                        .setPositiveButton(RBase.string.understand, null)
                        .show()
                }
            }
        }
        checkTimers.setOnCheckedChangeListener { _, isChecked ->
            settings.isTimersChecked = isChecked
            if (!isChecked) {
                checkTimerStamps.isChecked = false
                checkSchedulers.isChecked = false
            }
        }
        checkTimerStamps.setOnCheckedChangeListener { _, isChecked ->
            settings.isTimerStampsChecked = isChecked
            if (isChecked) {
                checkTimers.isChecked = true
            }
        }
        checkSchedulers.setOnCheckedChangeListener { _, isChecked ->
            settings.isSchedulersChecked = isChecked
            if (isChecked) {
                checkTimers.isChecked = true
            }
        }
        checkSettings.setOnCheckedChangeListener { _, isChecked ->
            settings.isSettingsChecked = isChecked
        }
    }

    fun removeWipeItem() {
        itemWipe.gone()
        itemWipe.getLayoutView<CompoundButton>().setOnCheckedChangeListener(null)
    }
}
