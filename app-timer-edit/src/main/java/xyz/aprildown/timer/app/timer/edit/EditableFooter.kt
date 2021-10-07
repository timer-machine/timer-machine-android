package xyz.aprildown.timer.app.timer.edit

import android.view.View
import android.widget.Button
import androidx.appcompat.widget.TooltipCompat
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.items.AbstractItem

internal class EditableFooter(
    private val listeners: List<View.OnClickListener>
) : AbstractItem<EditableFooter.ViewHolder>() {
    override val layoutRes: Int = R.layout.layout_edit_add_buttons
    override val type: Int = R.id.type_step_footer
    override fun getViewHolder(v: View): ViewHolder = ViewHolder(v).also {
        v.findViewById<Button>(R.id.btnAddStep).run {
            setOnClickListener(listeners[0])
            TooltipCompat.setTooltipText(
                this,
                context.getString(R.string.edit_add_normal_desp)
            )
        }
        v.findViewById<Button>(R.id.btnAddNotifier).run {
            setOnClickListener(listeners[1])
            TooltipCompat.setTooltipText(
                this,
                context.getString(R.string.edit_add_notifier_desp)
            )
        }
        v.findViewById<Button>(R.id.btnAddStart).run {
            setOnClickListener(listeners[2])
            TooltipCompat.setTooltipText(
                this,
                context.getString(R.string.edit_add_start_desp)
            )
        }
        v.findViewById<Button>(R.id.btnAddEnd).run {
            setOnClickListener(listeners[3])
            TooltipCompat.setTooltipText(
                this,
                context.getString(R.string.edit_add_end_desp)
            )
        }
        v.findViewById<Button>(R.id.btnAddGroup).run {
            setOnClickListener(listeners[4])
            TooltipCompat.setTooltipText(
                this,
                context.getString(R.string.edit_add_group_desp)
            )
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
