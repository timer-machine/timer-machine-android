package xyz.aprildown.timer.app.timer.edit

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import xyz.aprildown.timer.domain.entities.StepType

internal class StepTouchHelper(
    private val onItemMoved: (from: Int, to: Int) -> Unit,
    private val onItemDropped: (from: Int, to: Int) -> Unit,
    private val onItemSwiped: (position: Int) -> Unit
) : ItemTouchHelper.SimpleCallback(
    ItemTouchHelper.UP or ItemTouchHelper.DOWN,
    ItemTouchHelper.START or ItemTouchHelper.END
) {

    private var fromPos = RecyclerView.NO_POSITION
    private var toPos = RecyclerView.NO_POSITION

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float = 0.5f
    override fun getSwipeVelocityThreshold(defaultValue: Float): Float = defaultValue * 0.5f
    override fun getSwipeEscapeVelocity(defaultValue: Float): Float = defaultValue * 4f

    override fun getDragDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val item = recyclerView.fastAdapter.getItem(viewHolder.bindingAdapterPosition)
        // Only normal steps can be dragged.
        return if (item != null && canItemBeMoved(item)) {
            super.getDragDirs(recyclerView, viewHolder)
        } else {
            0
        }
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val fastAdapter = recyclerView.fastAdapter
        val fromItem = fastAdapter.getItem(viewHolder.bindingAdapterPosition)
        // We still need to check item type even if we've checked it in getDragDirs.
        // Items that can't be touched.
        if (fromItem is EditableGroup ||
            fromItem is EditableGroupEnd ||
            fromItem is EditableFooter
        ) {
            return false
        }
        val toItem = fastAdapter.getItem(target.bindingAdapterPosition)
        // Items whose position can't be changed.
        if (toItem is EditableFooter ||
            (
                toItem is EditableStep &&
                    (toItem.stepType == StepType.START || toItem.stepType == StepType.END)
                )
        ) {
            return false
        }
        if (fromItem is EditableStep) {
            if (fromPos == RecyclerView.NO_POSITION) {
                fromPos = viewHolder.bindingAdapterPosition
            }
            toPos = target.bindingAdapterPosition
        }
        onItemMoved(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
        return true
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        val view = viewHolder?.itemView ?: return
        // From https://github.com/android/animation-samples/tree/master/Motion Reorder
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            view.findViewById<MaterialCardView>(R.id.cardEditStep)?.isDragged = true
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        recyclerView.post {
            viewHolder.itemView.findViewById<MaterialCardView>(R.id.cardEditStep)
                ?.isDragged = false
            // Post here to avoid changing content during RecyclerView updating.
            if (fromPos != RecyclerView.NO_POSITION && toPos != RecyclerView.NO_POSITION) {
                onItemDropped(fromPos, toPos)
            }
            toPos = RecyclerView.NO_POSITION
            fromPos = toPos
        }
    }

    // We handle it manually.
    override fun isLongPressDragEnabled(): Boolean = false

    override fun getSwipeDirs(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val item = recyclerView.fastAdapter.getItem(viewHolder.bindingAdapterPosition)
        return if (item is EditableStep) {
            super.getSwipeDirs(recyclerView, viewHolder)
        } else {
            0
        }
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        onItemSwiped(viewHolder.bindingAdapterPosition)
    }

    companion object {
        private val RecyclerView.fastAdapter: FastAdapter<*> get() = adapter as FastAdapter<*>

        fun canItemBeMoved(item: GenericItem): Boolean {
            return item is EditableStep &&
                item.stepType != StepType.START &&
                item.stepType != StepType.END
        }
    }
}
