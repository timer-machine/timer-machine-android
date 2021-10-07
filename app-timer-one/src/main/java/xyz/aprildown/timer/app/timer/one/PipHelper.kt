package xyz.aprildown.timer.app.timer.one

import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import xyz.aprildown.timer.app.base.data.FloatingWindowPip
import xyz.aprildown.timer.app.base.utils.produceTime
import xyz.aprildown.timer.presentation.one.OneViewModel
import xyz.aprildown.timer.presentation.stream.StreamState
import xyz.aprildown.timer.presentation.stream.TimerIndex
import xyz.aprildown.timer.presentation.stream.getNiceLoopString
import xyz.aprildown.timer.presentation.stream.getStep
import xyz.aprildown.tools.arch.observeEvent
import xyz.aprildown.tools.helper.pendingServiceIntent

@RequiresApi(Build.VERSION_CODES.O)
internal class PipHelper(
    private val fragment: Fragment,
    private val viewModel: OneViewModel
) {
    private val activity = fragment.requireActivity()
    private lateinit var pipView: View

    private lateinit var timeObserver: Observer<Long>
    private lateinit var indexObserver: Observer<TimerIndex>
    private lateinit var stateObserver: Observer<StreamState>

    private val streamMachineIntentProvider = viewModel.streamMachineIntentProvider

    fun enterPipMode() {
        activity.enterPictureInPictureMode(
            PictureInPictureParams.Builder()
                .setActions(getTimerRemoteActions())
                .setAspectRatio(Rational(3, 2))
                .build()
        )
    }

    fun showPipView() {
        val decorView = activity.window.decorView as ViewGroup
        pipView = LayoutInflater.from(activity)
            .inflate(R.layout.layout_pip, decorView, false)
        decorView.addView(pipView)
        val textView = pipView.findViewById<TextView>(R.id.textPipTime)
        val stepName = pipView.findViewById<TextView>(R.id.textPipStepName)
        val loop = pipView.findViewById<TextView>(R.id.textPipLoop)

        timeObserver = Observer {
            textView.text = (it ?: 0).produceTime()
        }
        viewModel.timerCurrentTime.observe(fragment, timeObserver)

        indexObserver = Observer {
            val timer = viewModel.timer.value ?: return@Observer
            val totalLoop = timer.loop
            loop.text = it.getNiceLoopString(totalLoop)
            stepName.text = timer.getStep(it)?.label
        }
        viewModel.timerCurrentIndex.observe(fragment, indexObserver)

        stateObserver = Observer {
            activity.setPictureInPictureParams(
                PictureInPictureParams.Builder()
                    .setActions(getTimerRemoteActions())
                    .build()
            )
        }
        viewModel.timerCurrentState.observe(fragment, stateObserver)

        if (FloatingWindowPip(activity).autoClose) {
            viewModel.finishEvent.observeEvent(fragment) {
                dismissPipView()
                activity.finish()
            }
        }
    }

    fun dismissPipView() {
        val decorView = activity.window.decorView as ViewGroup
        decorView.removeView(pipView)
        viewModel.timerCurrentTime.removeObserver(timeObserver)
        viewModel.timerCurrentIndex.removeObserver(indexObserver)
        viewModel.timerCurrentState.removeObserver(stateObserver)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getTimerRemoteActions(): List<RemoteAction> {
        val timer = viewModel.timer.value ?: return emptyList()
        return listOf(
            if (viewModel.timerCurrentState.value?.isRunning == true) {
                RemoteAction(
                    Icon.createWithResource(activity, R.drawable.ic_pause),
                    activity.getString(R.string.pause),
                    activity.getString(R.string.pause),
                    activity.pendingServiceIntent(streamMachineIntentProvider.pauseIntent(timer.id))
                )
            } else {
                RemoteAction(
                    Icon.createWithResource(activity, R.drawable.ic_start),
                    activity.getString(R.string.start),
                    activity.getString(R.string.start),
                    activity.pendingServiceIntent(streamMachineIntentProvider.startIntent(timer.id))
                )
            },
            RemoteAction(
                Icon.createWithResource(activity, R.drawable.ic_stop),
                activity.getString(R.string.stop),
                activity.getString(R.string.stop),
                activity.pendingServiceIntent(streamMachineIntentProvider.resetIntent(timer.id))
            )
        )
    }
}
