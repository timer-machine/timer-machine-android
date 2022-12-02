package xyz.aprildown.timer.app.timer.one.step

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.core.view.GestureDetectorCompat
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.items.AbstractItem
import xyz.aprildown.timer.app.timer.one.R
import xyz.aprildown.tools.helper.attachToView
import xyz.aprildown.timer.app.base.R as RBase

internal class VisibleGroup(
    private val name: String,
    private val totalLoop: Int,
    private val number: Int,
    id: Long,
    private val stepLongClickListener: OnStepLongClickListener
) : AbstractItem<VisibleGroup.ViewHolder>() {

    override val layoutRes: Int = R.layout.item_step_group
    override val type: Int = RBase.id.type_step_group
    override fun getViewHolder(v: View): ViewHolder = ViewHolder(v)
    override var identifier: Long = id

    var loopIndex: Int = -1

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.run {
            GestureDetectorCompat(
                holder.itemView.context,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onDoubleTap(e: MotionEvent): Boolean {
                        stepLongClickListener.onStepDoubleTap(this@VisibleGroup)
                        return true
                    }
                }
            ).attachToView(indicatorLayout)

            numberView.text = number.toString()
            nameView.text = name
            loopView.text = "%d/%d".format(loopIndex + 1, totalLoop)
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val indicatorLayout: View = view.findViewById(R.id.layoutStepGroupIndicator)
        val numberView: TextView = view.findViewById(R.id.textStepGroupNumber)
        val nameView: TextView = view.findViewById(R.id.textStepGroupName)
        val loopView: TextView = view.findViewById(R.id.textStepGroupLoop)
    }
}
