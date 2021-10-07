package xyz.aprildown.timer.app.scheduler

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import xyz.aprildown.timer.domain.entities.SchedulerEntity

internal class SchedulerAdapter(
    private val onClickScheduler: (scheduler: SchedulerEntity) -> Unit,
    private val schedulerCallback: VisibleScheduler.Callback
) : RecyclerView.Adapter<VisibleScheduler.ViewHolder>() {

    private val schedulers = mutableListOf<VisibleScheduler>()

    override fun getItemCount(): Int = schedulers.size
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VisibleScheduler.ViewHolder {
        return VisibleScheduler.ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_scheduler, parent, false)
        ).apply {
            itemView.setOnClickListener {
                onClickScheduler.invoke(schedulers[bindingAdapterPosition].scheduler)
            }
        }
    }

    override fun onBindViewHolder(holder: VisibleScheduler.ViewHolder, position: Int) {
        schedulers[position].bind(holder, schedulerCallback)
    }

    fun get(pos: Int): VisibleScheduler = schedulers[pos]
    fun set(new: List<VisibleScheduler>) {
        schedulers.clear()
        schedulers.addAll(new)
        notifyDataSetChanged()
    }

    fun remove(pos: Int) {
        schedulers.removeAt(pos)
        notifyItemRemoved(pos)
    }
}
