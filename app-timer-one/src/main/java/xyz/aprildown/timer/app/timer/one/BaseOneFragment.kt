package xyz.aprildown.timer.app.timer.one

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.text.InputType
import android.text.format.DateUtils
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.EditText
import androidx.annotation.LayoutRes
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.viewbinding.ViewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import xyz.aprildown.timer.app.base.ui.AppNavigator
import xyz.aprildown.timer.app.base.ui.StepUpdater
import xyz.aprildown.timer.app.base.utils.ShortcutHelper
import xyz.aprildown.timer.app.timer.one.float.FloatingTimer
import xyz.aprildown.timer.component.key.DurationPicker
import xyz.aprildown.timer.component.key.ListItemWithLayout
import xyz.aprildown.timer.component.key.SimpleInputDialog
import xyz.aprildown.timer.domain.utils.AppTracker
import xyz.aprildown.timer.presentation.one.OneViewModel
import xyz.aprildown.timer.presentation.stream.MachineContract
import xyz.aprildown.timer.presentation.stream.StreamState
import xyz.aprildown.timer.presentation.stream.TimerIndex
import xyz.aprildown.timer.presentation.stream.getStep
import xyz.aprildown.tools.anko.longToast
import xyz.aprildown.tools.arch.Event
import xyz.aprildown.tools.helper.createChooserIntentIfDead
import xyz.aprildown.tools.helper.startActivitySafely
import javax.inject.Inject
import javax.inject.Provider
import xyz.aprildown.timer.app.base.R as RBase

abstract class BaseOneFragment<T : ViewBinding>(
    @LayoutRes contentLayoutId: Int
) : Fragment(contentLayoutId) {

    protected val viewModel: OneViewModel by activityViewModels()

    @Inject
    lateinit var appNavigator: AppNavigator

    @Inject
    lateinit var appTracker: AppTracker

    @Inject
    lateinit var stepUpdater: Provider<StepUpdater>

    private var isBind = false

    private var pipHelper: PipHelper? = null

    override fun onStart() {
        super.onStart()
        val activity = requireActivity()
        if (!isBind) {
            isBind = true
            activity.bindService(
                viewModel.getBindIntent(),
                mConnection,
                Context.BIND_AUTO_CREATE
            )
        }
    }

    override fun onStop() {
        super.onStop()
        if (isBind) {
            isBind = false
            requireActivity().unbindService(mConnection)
            viewModel.dropPresenter()
        }
    }

    protected fun setToolbarTitle(title: String) {
        (activity as? OneActivityInterface)?.setToolbarTitle(title)
    }

    protected fun actionStartPause() {
        viewModel.onStartPause()
    }

    protected fun actionStop() {
        viewModel.onReset()
    }

    protected fun actionPrevStep() {
        viewModel.onMove(-1)
    }

    protected fun actionNextStep() {
        viewModel.onMove(1)
    }

    protected fun actionJump(newIndex: TimerIndex) {
        viewModel.onJump(newIndex)
    }

    protected fun actionUpdateStep(index: TimerIndex) {
        val step = viewModel.timer.value?.getStep(index) ?: return

        if (viewModel.uiLocked.value == true) {
            viewModel.messageEvent.value = Event(RBase.string.one_ui_locked)
            return
        }

        if (viewModel.timerCurrentState.value == StreamState.RUNNING) {
            viewModel.onStartPause()
        }

        stepUpdater.get().updateStep(step) {
            viewModel.updateStep(index, it)
        }.show(childFragmentManager, null)
    }

    protected fun actionUpdateStepTime(index: TimerIndex) {
        val step = viewModel.timer.value?.getStep(index) ?: return

        if (viewModel.uiLocked.value == true) {
            viewModel.messageEvent.value = Event(RBase.string.one_ui_locked)
            return
        }

        if (viewModel.timerCurrentState.value == StreamState.RUNNING) {
            viewModel.onStartPause()
        }

        val context = context ?: return
        DurationPicker(context) { hours, minutes, seconds ->
            viewModel.updateStep(
                index,
                step.copy(
                    length = hours * DateUtils.HOUR_IN_MILLIS +
                        minutes * DateUtils.MINUTE_IN_MILLIS +
                        seconds * DateUtils.SECOND_IN_MILLIS
                )
            )
        }.show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    protected fun actionEnterPipMode() {
        val ph = PipHelper(this, viewModel)
        pipHelper = ph
        ph.enterPipMode()
    }

    protected fun actionFloating() {
        val context = requireContext()
        if (Settings.canDrawOverlays(context)) {
            val timer = viewModel.timer.value ?: return
            FloatingTimer(context, timer, viewModel.streamMachineIntentProvider, appTracker).show()
        } else {
            context.startActivitySafely(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                    .setData(Uri.parse("package:${context.packageName}"))
                    .createChooserIntentIfDead(context)
            )
            context.longToast(RBase.string.perm_rational_floating)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        if (isInPictureInPictureMode) {
            pipHelper?.showPipView()
        } else {
            pipHelper?.dismissPipView()
            pipHelper = null
        }
    }

    protected fun actionToOneLayoutSetting() {
        NavHostFragment.findNavController(this).navigate(
            RBase.id.dest_one_layout,
            null,
            NavOptions.Builder()
                .setEnterAnim(RBase.anim.slide_in_bottom)
                .setExitAnim(RBase.anim.slide_out_bottom)
                .setPopEnterAnim(RBase.anim.slide_in_bottom)
                .setPopExitAnim(RBase.anim.slide_out_bottom)
                .build()
        )
    }

    protected fun actionLockUi(lock: Boolean) {
        viewModel.uiLocked.value = lock
    }

    protected fun actionCreateShortcut() {
        val timer = viewModel.timer.value ?: return
        val context = requireContext()

        val builder = MaterialAlertDialogBuilder(context)
            .setTitle(RBase.string.one_action_add_shortcut)
            .setPositiveButton(RBase.string.add, null)
            .setNegativeButton(android.R.string.cancel, null)

        val view = View.inflate(context, R.layout.dialog_one_create_shortcut, null) as ViewGroup

        val editShortcutName = view.getChildAt(0) as EditText
        val switchShortcutOpen =
            (view.getChildAt(1) as ListItemWithLayout).getLayoutView<CompoundButton>()

        builder.setView(view)

        val dialog = builder.create()
        dialog.show()

        editShortcutName.setText(timer.name)

        val timerId = timer.id
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            ShortcutHelper.addTimerShortcut(
                timerId = timerId,
                context = context,
                shortcutName = editShortcutName.text.toString(),
                intent = appNavigator.getStartTimerShortcutIntent(
                    timerId = timerId,
                    openOnClick = switchShortcutOpen.isChecked
                ),
                shortcutCreatedIntent = appNavigator.getShortcutCreatedIntent()
            )
            dialog.dismiss()
        }
    }

    protected fun actionEditTimer() {
        viewModel.onEdit()
    }

    protected fun showPickLoopDialog(maxLoop: Int, onPick: (Int) -> Unit) {
        SimpleInputDialog(requireContext()).show(
            inputType = InputType.TYPE_CLASS_NUMBER,
            hint = getString(RBase.string.name_loop_loop_hint)
        ) { input ->
            input.toIntOrNull()
                ?.takeIf { it in 1..maxLoop }
                ?.dec()
                ?.let(onPick)
        }
    }

    protected fun actionTweakTime(amount: Long = 60_000L) {
        viewModel.tweakTime(amount)
    }

    private val mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            viewModel.setPresenter((service as MachineContract.PresenterProvider).getPresenter())
        }

        override fun onServiceDisconnected(name: ComponentName?) = Unit
    }
}
