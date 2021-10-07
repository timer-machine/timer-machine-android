package xyz.aprildown.timer.app.timer.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import xyz.aprildown.timer.presentation.stream.MachineContract
import xyz.aprildown.timer.presentation.stream.StreamState
import xyz.aprildown.timer.presentation.stream.TimerIndex
import xyz.aprildown.timer.presentation.stream.TimerMachineListener
import java.util.concurrent.Executor

/**
 * We need to use a custom [RecyclerView.Adapter] to get more control.
 */
internal class TimerAdapter(
    private val callback: Callback,
    backgroundExecutor: Executor
) : ListAdapter<MutableTimerItem, RecyclerView.ViewHolder>(
    AsyncDifferConfig.Builder(
        object : DiffUtil.ItemCallback<MutableTimerItem>() {
            override fun areItemsTheSame(
                oldItem: MutableTimerItem,
                newItem: MutableTimerItem
            ): Boolean = oldItem.timerId == newItem.timerId

            override fun areContentsTheSame(
                oldItem: MutableTimerItem,
                newItem: MutableTimerItem
            ): Boolean = oldItem == newItem
        }
    )
        .setBackgroundThreadExecutor(backgroundExecutor)
        .build()
), TimerMachineListener {

    interface Callback {
        fun onTimerAction(viewHolder: RecyclerView.ViewHolder, actionId: Int)
    }

    private var presenter: MachineContract.Presenter? = null

    var showGrid: Boolean = false
        set(value) {
            field = value
            collapsedLayoutRes =
                if (showGrid) R.layout.list_item_timer_collapsed_grid else R.layout.list_item_timer_collapsed
            expandedLayoutRes =
                if (showGrid) R.layout.list_item_timer_expanded_gird else R.layout.list_item_timer_expanded
            notifyDataSetChanged()
        }

    private var collapsedLayoutRes = R.layout.list_item_timer_collapsed

    private var expandedLayoutRes = R.layout.list_item_timer_expanded

    init {
        setHasStableIds(true)
    }

    override fun getItemViewType(position: Int): Int {
        return if (!getItemAt(position).isExpanded) collapsedLayoutRes else expandedLayoutRes
    }

    override fun getItemId(position: Int): Long = getItemAt(position).timerId.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return when (viewType) {
            collapsedLayoutRes -> CollapsedViewHolder(view, callback)
            expandedLayoutRes -> ExpandedViewHolder(view, callback)
            else -> throw IllegalStateException("Wrong view type $viewType")
        }.also { viewHolder ->
            viewHolder.itemView.findViewById<View>(R.id.cardTimer).setOnClickListener {
                callback.onTimerAction(viewHolder, ACTION_ITEM_CLICK)
            }
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: List<Any>
    ) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else if (holder is ExpandedViewHolder) {
            holder.partialBind(getItemAt(position), payloads)
        }
    }

    /**
     * When binding ViewHolders for the first time, all of them are not expanded.
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mutableTimerItem = getItemAt(position)
        if (holder is CollapsedViewHolder) {
            holder.bind(mutableTimerItem)
        } else {
            if (presenter != null && getItemViewType(position) == expandedLayoutRes) {
                val timer = getItemAt(position)
                presenter?.getTimerStateInfo(timer.timerId)?.run {
                    if (holder is ExpandedViewHolder) {
                        holder.presenterBind(timer, timerEntity, state, index, time)
                    }
                }
            }
        }
    }

    fun getItemAt(position: Int): MutableTimerItem = getItem(position)

    /**
     * items => presenter, refresh
     * presenter => items, refresh
     */
    fun setItems(new: List<MutableTimerItem>) {
        submitList(new) {
            refreshPresenter()
        }
    }

    fun expand(pos: Int) {
        getItemAt(pos).isExpanded = true
        notifyItemChanged(pos)
    }

    private fun collapse(pos: Int) {
        getItemAt(pos).run {
            timerItem = null
            isExpanded = false
            state = StreamState.RESET
        }
        notifyItemChanged(pos)
    }

    fun setPresenter(presenter: MachineContract.Presenter) {
        this.presenter?.removeAllListener(this@TimerAdapter)
        this.presenter = presenter
        this.presenter?.addAllListener(this)
        refreshPresenter()
    }

    private fun refreshPresenter() {
        if (presenter != null) {
            currentList.forEachIndexed { index, timerItem ->
                val stateInfo = presenter?.getTimerStateInfo(timerItem.timerId)
                if (stateInfo != null && !stateInfo.state.isReset) {
                    expand(index)
                    timerItem.state = stateInfo.state
                    notifyItemChanged(index, ExpandedViewHolder.MutableTimerEvent.State)
                    notifyItemChanged(
                        index,
                        ExpandedViewHolder.MutableTimerEvent.Timing(stateInfo.time)
                    )
                    notifyItemChanged(
                        index,
                        ExpandedViewHolder.MutableTimerEvent.Index(stateInfo.index)
                    )
                } else {
                    if (timerItem.isExpanded) {
                        collapse(index)
                    }
                }
            }
        }
    }

    fun dropPresenter() {
        presenter?.removeAllListener(this)
        presenter = null
    }

    // region stream callbacks

    override fun begin(timerId: Int) = Unit

    override fun started(timerId: Int, index: TimerIndex) {
        currentList.forEachIndexed { i, timer ->
            if (timer.timerId == timerId) {
                expand(i)
                timer.state = StreamState.RUNNING
                notifyItemChanged(i, ExpandedViewHolder.MutableTimerEvent.State)
                notifyItemChanged(i, ExpandedViewHolder.MutableTimerEvent.Index(index))
            }
        }
    }

    override fun paused(timerId: Int) {
        currentList.forEachIndexed { i, timer ->
            if (timer.timerId == timerId) {
                timer.state = StreamState.PAUSED
                notifyItemChanged(i, ExpandedViewHolder.MutableTimerEvent.State)
            }
        }
    }

    override fun updated(timerId: Int, time: Long) {
        currentList.forEachIndexed { i, timer ->
            if (timer.timerId == timerId) {
                notifyItemChanged(i, ExpandedViewHolder.MutableTimerEvent.Timing(time))
            }
        }
    }

    override fun finished(timerId: Int) = Unit

    override fun end(timerId: Int, forced: Boolean) {
        currentList.forEachIndexed { i, timer ->
            if (timer.timerId == timerId) {
                collapse(i)
            }
        }
    }

    // endregion stream callbacks

    companion object {
        const val ACTION_ITEM_CLICK = 0
        const val ACTION_COLLAPSED_START = 1
        const val ACTION_CONTEXT_MENU = 2
        const val ACTION_EXPANDED_START_PAUSE = 4
        const val ACTION_EXPANDED_STOP = 5
    }
}
