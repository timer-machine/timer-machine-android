package io.github.deweyreed.timer.ui

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.materialdrawer.model.AbstractDrawerItem
import com.mikepenz.materialdrawer.model.DividerDrawerItem
import com.github.deweyreed.tools.R as RTools
import com.mikepenz.materialdrawer.R as RMaterialDrawer

/**
 * [DividerDrawerItem]
 */
internal class DrawerDividerItem :
    AbstractDrawerItem<DrawerDividerItem, DrawerDividerItem.ViewHolder>() {

    override val layoutRes: Int = RTools.layout.divider
    override val type: Int = RMaterialDrawer.id.material_drawer_item_divider
    override fun getViewHolder(v: View): ViewHolder = ViewHolder(v)

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.divider.run {
            isClickable = false
            isEnabled = false
            minimumHeight = 1
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val divider: View = view
    }
}
