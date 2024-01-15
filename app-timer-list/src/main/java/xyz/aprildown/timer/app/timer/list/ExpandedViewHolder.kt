package xyz.aprildown.timer.app.timer.list

import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import xyz.aprildown.timer.app.base.utils.produceTime
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.presentation.stream.StreamState
import xyz.aprildown.timer.presentation.stream.TimerIndex
import xyz.aprildown.timer.presentation.stream.getNiceLoopString
import xyz.aprildown.timer.presentation.stream.getStep
import xyz.aprildown.timer.app.base.R as RBase

internal class ExpandedViewHolder(
    view: View,
    callback: TimerAdapter.Callback
) : RecyclerView.ViewHolder(view) {

    sealed class MutableTimerEvent {
        data object State : MutableTimerEvent()
        data class Timing(val time: Long) : MutableTimerEvent()
        data class Index(val index: TimerIndex) : MutableTimerEvent()
    }

    private val context = view.context
    private val name = view.findViewById<TextView>(R.id.textTimerName)
    private val loop = view.findViewById<TextView>(R.id.textTimerLoop)
    private val stepName = view.findViewById<TextView>(R.id.textTimerStepName)
    private val remaining = view.findViewById<TextView>(R.id.textTimerRemainingTime)
    private val stop = view.findViewById<ImageButton>(R.id.imageTimerStop)
    private val start = view.findViewById<ImageButton>(R.id.imageTimerStartPause)

    init {
        start.setOnClickListener {
            callback.onTimerAction(this, TimerAdapter.ACTION_EXPANDED_START_PAUSE)
        }
        stop.setOnClickListener {
            callback.onTimerAction(this, TimerAdapter.ACTION_EXPANDED_STOP)
        }
    }

    fun presenterBind(
        item: MutableTimerItem,
        timerEntity: TimerEntity,
        state: StreamState,
        index: TimerIndex,
        time: Long
    ) {
        if (!state.isReset) {
            item.timerItem = timerEntity
            name.text = timerEntity.name
            if (state.isRunning) {
                start.setImageResource(RBase.drawable.ic_pause)
                start.contentDescription = context.getString(RBase.string.pause)
            } else {
                start.setImageResource(RBase.drawable.ic_start)
                start.contentDescription = context.getString(RBase.string.start)
            }
            val totalLoop = timerEntity.loop
            loop.text = index.getNiceLoopString(totalLoop)
            stepName.text = timerEntity.getStep(index)?.label
            remaining.text = time.produceTime()
        }
    }

    fun partialBind(item: MutableTimerItem, payloads: List<Any>) {
        val context = itemView.context
        payloads.forEach { payload ->
            when (payload) {
                is MutableTimerEvent.Timing -> {
                    remaining.text = payload.time.produceTime()
                }
                is MutableTimerEvent.Index -> {
                    item.timerItem?.let {
                        val index = payload.index
                        loop.text = index.getNiceLoopString(it.loop)
                        stepName.text = it.getStep(index)?.label
                    }
                }
                MutableTimerEvent.State -> {
                    if (item.state.isRunning) {
                        start.setImageResource(RBase.drawable.ic_pause)
                        start.contentDescription = context.getString(RBase.string.pause)
                    } else {
                        start.setImageResource(RBase.drawable.ic_start)
                        start.contentDescription = context.getString(RBase.string.start)
                    }
                }
            }
        }
    }
}
