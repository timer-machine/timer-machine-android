package xyz.aprildown.timer.app.timer.one.step

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.items.AbstractItem
import com.mikepenz.fastadapter.utils.DefaultIdDistributorImpl
import xyz.aprildown.timer.app.timer.one.R
import xyz.aprildown.timer.app.base.R as RBase

internal class VisibleGroupEnd : AbstractItem<VisibleGroupEnd.ViewHolder>() {

    override val layoutRes: Int = R.layout.item_step_group_end
    override val type: Int = RBase.id.type_step_group_end
    override fun getViewHolder(v: View): ViewHolder = ViewHolder(v)

    /**
     * Although we need unique identifiers,
     * FastAdapter will handles all for us since it decrements from -2 [DefaultIdDistributorImpl].
     */

    // override var identifier: Long = -1

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
