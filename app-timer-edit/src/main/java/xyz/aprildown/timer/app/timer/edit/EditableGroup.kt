package xyz.aprildown.timer.app.timer.edit

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ImageButton
import androidx.core.view.isVisible
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import xyz.aprildown.timer.component.key.NameLoopView
import xyz.aprildown.timer.app.base.R as RBase

internal class EditableGroup(
    var name: String,
    var loop: Int,
    var totalTime: Long = 0L,
    private val handler: Handler,
    private val showTotalTime: Boolean
) : AbstractItem<EditableGroup.ViewHolder>() {

    object TotalTimeChanged

    interface Handler {
        fun onGroupDeleteButtonClick(view: View, position: Int)
        fun onGroupLoopChanged()
    }

    override val layoutRes: Int = R.layout.item_edit_group
    override val type: Int = RBase.id.type_step_group
    override fun getViewHolder(v: View): ViewHolder = ViewHolder(v, handler, showTotalTime)

    class ViewHolder(
        view: View,
        private val handler: Handler,
        showTotalTime: Boolean
    ) : FastAdapter.ViewHolder<EditableGroup>(view) {
        private val nameLoopView = view.findViewById<NameLoopView>(R.id.viewStepGroupNameLoop)
        private var nameViewTextChangeListener: TextWatcher? = null
        private var loopViewTextChangeListener: TextWatcher? = null
        private val deleteBtn = view.findViewById<ImageButton>(R.id.btnStepGroupDelete)
        private val infoView = view.findViewById<StepInfoView>(R.id.viewStepGroupInfo)

        init {
            infoView.isVisible = showTotalTime
        }

        override fun bindView(item: EditableGroup, payloads: List<Any>) {
            if (payloads.isNotEmpty()) {
                payloads.forEach { payload ->
                    when (payload) {
                        is TotalTimeChanged -> {
                            infoView.setDuration(item.totalTime)
                        }
                    }
                }
                return
            }

            nameLoopView.setName(item.name)
            nameViewTextChangeListener = object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) = Unit

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) =
                    Unit

                override fun afterTextChanged(s: Editable?) {
                    item.name = nameLoopView.getName()
                }
            }
            nameLoopView.nameView.addTextChangedListener(nameViewTextChangeListener)

            nameLoopView.setLoop(item.loop)
            loopViewTextChangeListener = object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) = Unit

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) =
                    Unit

                override fun afterTextChanged(s: Editable?) {
                    item.loop = nameLoopView.getLoop()
                    handler.onGroupLoopChanged()
                }
            }
            nameLoopView.loopView.addTextChangedListener(loopViewTextChangeListener)

            deleteBtn.setOnClickListener {
                handler.onGroupDeleteButtonClick(it, bindingAdapterPosition)
            }

            infoView.setDuration(item.totalTime)
        }

        override fun unbindView(item: EditableGroup) {
            nameLoopView.nameView.removeTextChangedListener(nameViewTextChangeListener)
            nameViewTextChangeListener = null
            nameLoopView.loopView.removeTextChangedListener(loopViewTextChangeListener)
            loopViewTextChangeListener = null
        }
    }
}
