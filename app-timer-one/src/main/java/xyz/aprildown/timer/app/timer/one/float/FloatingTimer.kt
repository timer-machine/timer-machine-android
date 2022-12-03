package xyz.aprildown.timer.app.timer.one.float

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.cardview.widget.CardView
import androidx.core.view.updateLayoutParams
import com.github.deweyreed.tools.anko.toast
import xyz.aprildown.timer.app.base.data.FloatingWindowPip
import xyz.aprildown.timer.app.base.ui.newDynamicTheme
import xyz.aprildown.timer.app.base.utils.produceTime
import xyz.aprildown.timer.app.timer.one.R
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.utils.AppTracker
import xyz.aprildown.timer.presentation.StreamMachineIntentProvider
import xyz.aprildown.timer.presentation.stream.MachineContract
import xyz.aprildown.timer.presentation.stream.StreamState
import xyz.aprildown.timer.presentation.stream.TimerIndex
import xyz.aprildown.timer.presentation.stream.TimerMachineListener
import xyz.aprildown.timer.presentation.stream.getFirstIndex
import xyz.aprildown.timer.presentation.stream.getNiceLoopString
import xyz.aprildown.timer.presentation.stream.getStep
import xyz.aprildown.timer.app.base.R as RBase

internal class FloatingTimer(
    c: Context,
    private val timer: TimerEntity,
    private val streamMachineIntentProvider: StreamMachineIntentProvider,
    private val appTracker: AppTracker,
) : TimerMachineListener {

    /**
     * 1. Use application context to avoid memory leak.
     * 2. Bind the service using application context to prevent the service from being stopped after
     *    the user exits the app.
     */
    private val context = c.applicationContext

    private lateinit var textLoop: TextView
    private lateinit var textStepName: TextView
    private lateinit var textTime: TextView
    private lateinit var btnStartPause: ImageButton

    private var state: StreamState = StreamState.RESET

    private lateinit var floater: Floater
    private var presenter: MachineContract.Presenter? = null
    private var bind = false

    fun show() {
        val themedContext = ContextThemeWrapper(context, RBase.style.AppTheme)

        val dynamicTheme = newDynamicTheme
        val colorPrimary = dynamicTheme.colorPrimary
        val colorOnPrimary = dynamicTheme.colorOnPrimary

        val view = View.inflate(
            themedContext,
            R.layout.layout_floating_window,
            null
        )
        (view as CardView).setBackgroundColor(colorPrimary)

        view.findViewById<TextView>(R.id.textFloatingTitle).run {
            setTextColor(colorOnPrimary)
            text = timer.name
        }

        textLoop = view.findViewById(R.id.textFloatingLoop)
        textLoop.setTextColor(colorOnPrimary)
        textLoop.text = "0/%d".format(timer.loop)

        textStepName = view.findViewById(R.id.textFloatingStepName)
        textStepName.setTextColor(colorOnPrimary)
        textStepName.text = timer.getStep(timer.getFirstIndex())?.label

        textTime = view.findViewById(R.id.textFloatingTime)
        textTime.setTextColor(colorOnPrimary)
        textTime.text = 0L.produceTime()

        btnStartPause = view.findViewById(R.id.btnFloatingStartPause)
        btnStartPause.setColorFilter(colorOnPrimary)
        updateButton()

        val btnClose = view.findViewById<ImageButton>(R.id.btnFloatingClose)
        btnClose.setColorFilter(colorOnPrimary)

        val btnPrev = view.findViewById<ImageButton>(R.id.btnFloatingPrev)
        btnPrev.setColorFilter(colorOnPrimary)

        val btnNext = view.findViewById<ImageButton>(R.id.btnFloatingNext)
        btnNext.setColorFilter(colorOnPrimary)

        val timerId = timer.id

        val btnStop = view.findViewById<ImageButton>(R.id.btnFloatingStop)
        btnStop.setColorFilter(colorOnPrimary)
        btnStop.setOnClickListener {
            context.startService(streamMachineIntentProvider.resetIntent(timerId))
        }
        btnPrev.setOnClickListener {
            context.startService(streamMachineIntentProvider.decreIntent(timerId))
        }
        btnNext.setOnClickListener {
            context.startService(streamMachineIntentProvider.increIntent(timerId))
        }
        btnClose.setOnClickListener {
            tearDown()
        }

        btnStartPause.setOnClickListener {
            context.startService(
                if (state.isRunning) streamMachineIntentProvider.pauseIntent(timerId)
                else streamMachineIntentProvider.startIntent(timerId)
            )
        }

        floater = Floater(context, view, appTracker = appTracker)
        try {
            floater.show()
        } catch (_: WindowManager.BadTokenException) {
            context.toast(RBase.string.settings_floating_window_pip_floating_failed)
            floater.dismiss()
            return
        }
        // After show, layout params are added.
        FloatingWindowPip(context).run {
            view.alpha = floatingWindowAlpha
            view.updateLayoutParams<ViewGroup.LayoutParams> {
                val (w, h) = calculateFloatingWindowSize()
                width = w
                height = h
                val displayMetrics = context.resources.displayMetrics
                floater.updatePos(
                    x = (displayMetrics.widthPixels - w) / 2,
                    y = (displayMetrics.heightPixels - h) / 2
                )
            }
        }

        context.bindService(
            streamMachineIntentProvider.bindIntent(),
            mConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    private fun tearDown() {
        dropPresenter()
        if (bind) {
            bind = false
            context.unbindService(mConnection)
        }
        floater.dismiss()
    }

    fun setPresenter(presenter: MachineContract.Presenter) {
        this.presenter = presenter
        this.presenter?.addListener(timer.id, this)

        presenter.getTimerStateInfo(timer.id).let {
            if (it != null) {
                val t = it.timerEntity
                state = it.state
                val index = it.index
                val totalLoop = t.loop
                updateButton()
                textStepName.text = t.getStep(index)?.label
                textLoop.text = index.getNiceLoopString(totalLoop)
                textTime.text = it.time.produceTime()
            }
        }
    }

    private fun dropPresenter() {
        presenter?.removeListener(timer.id, this)
        presenter = null
    }

    override fun begin(timerId: Int) = Unit

    override fun started(timerId: Int, index: TimerIndex) {
        state = StreamState.RUNNING
        updateButton()
        val totalLoop = timer.loop
        textLoop.text = index.getNiceLoopString(totalLoop)
        textStepName.text = timer.getStep(index)?.label
    }

    override fun paused(timerId: Int) {
        state = StreamState.PAUSED
        updateButton()
    }

    override fun updated(timerId: Int, time: Long) {
        textTime.text = time.produceTime()
    }

    override fun finished(timerId: Int) = Unit

    override fun end(timerId: Int, forced: Boolean) {
        state = StreamState.RESET
        updateButton()
        textTime.text = 0L.produceTime()
        textLoop.text = "0/%d".format(timer.loop)
        textStepName.text = timer.getStep(timer.getFirstIndex())?.label

        if (!forced && FloatingWindowPip(context).autoClose) {
            tearDown()
        }
    }

    private fun updateButton() {
        btnStartPause.run {
            if (state.isRunning) {
                contentDescription = context.getString(RBase.string.pause)
                setImageResource(RBase.drawable.ic_pause)
            } else {
                contentDescription = context.getString(RBase.string.start)
                setImageResource(RBase.drawable.ic_start)
            }
        }
    }

    private val mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            setPresenter((service as MachineContract.PresenterProvider).getPresenter())
            bind = true
        }

        override fun onServiceDisconnected(name: ComponentName?) = Unit
    }
}
