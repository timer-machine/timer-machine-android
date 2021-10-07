package xyz.aprildown.timer.app.timer.edit

import android.view.View
import androidx.appcompat.widget.AppCompatImageButton
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.items.AbstractItem

internal class EditableGroupEnd(
    private val handler: Handler
) : AbstractItem<EditableGroupEnd.ViewHolder>() {

    interface Handler {
        fun onGroupEndClick(view: View, position: Int)
    }

    override val layoutRes: Int = R.layout.item_edit_group_end
    override val type: Int = R.id.type_step_group_end
    override fun getViewHolder(v: View): ViewHolder = ViewHolder(v, handler)

    class ViewHolder(
        view: View,
        private val handler: Handler
    ) : RecyclerView.ViewHolder(view) {

        private val btnAdd = view.findViewById<AppCompatImageButton>(R.id.btnStepGroupEndAdd)

        init {
            btnAdd.setOnClickListener {
                handler.onGroupEndClick(it, bindingAdapterPosition)
            }
        }
    }
}
