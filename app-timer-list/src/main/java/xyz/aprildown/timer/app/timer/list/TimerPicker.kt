package xyz.aprildown.timer.app.timer.list

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewStub
import android.widget.CompoundButton
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.ISelectionListener
import com.mikepenz.fastadapter.adapters.GenericItemAdapter
import com.mikepenz.fastadapter.expandable.getExpandableExtension
import com.mikepenz.fastadapter.expandable.items.AbstractExpandableItem
import com.mikepenz.fastadapter.listeners.CustomEventHook
import com.mikepenz.fastadapter.select.getSelectExtension
import dagger.hilt.android.AndroidEntryPoint
import xyz.aprildown.timer.app.base.ui.AppNavigator
import xyz.aprildown.timer.app.base.utils.getDisplayName
import xyz.aprildown.timer.app.timer.list.databinding.FragmentTimerPickerBinding
import xyz.aprildown.timer.app.timer.list.databinding.ListItemTimerPickerFolderBinding
import xyz.aprildown.timer.app.timer.list.databinding.ListItemTimerPickerTimerBinding
import xyz.aprildown.timer.domain.entities.FolderEntity
import xyz.aprildown.timer.domain.entities.TimerInfo
import xyz.aprildown.timer.presentation.timer.TimerPickerViewModel
import xyz.aprildown.timer.app.base.R as RBase
import xyz.aprildown.tools.R as RTools

@AndroidEntryPoint
class TimerPicker : DialogFragment() {

    // We dismiss the dialog in onPause, so we won't lose this callback.
    var callback: ((AppNavigator.PickTimerResult) -> Unit)? = null

    private val viewModel: TimerPickerViewModel by viewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = FragmentTimerPickerBinding.inflate(layoutInflater)

        val arguments = requireArguments()
        val multi = arguments.getBoolean(EXTRA_MULTI, false)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(RBase.string.timer_pick_hint)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .apply {
                if (multi) {
                    setNeutralButton(RBase.string.deselect, null)
                }
            }
            .create()

        val itemAdapter = GenericItemAdapter()
        val fastAdapter = FastAdapter.with(itemAdapter)

        val selectExtension = fastAdapter.getSelectExtension()
        val expandableExtension = fastAdapter.getExpandableExtension()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val allSelected = selectExtension.selectedItems.mapNotNull { it as? TimerItem }
                if (allSelected.isNotEmpty()) {
                    val firstFolder = allSelected.first().parent as? FolderItem
                    callback?.invoke(
                        AppNavigator.PickTimerResult(
                            timerInfo = allSelected.map { it.timerInfo },
                            folder = if (allSelected.all { it.parent === firstFolder }) {
                                firstFolder?.folderEntity
                            } else {
                                null
                            }
                        )
                    )
                }
                dialog.dismiss()
            }
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setOnClickListener {
                selectExtension.deselect()
            }
        }

        if (multi) {
            fastAdapter.addEventHook(
                object : CustomEventHook<GenericItem>() {
                    override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {
                        return (viewHolder as? FolderItem.ViewHolder)?.binding?.viewTimerPickerExpandArea
                    }

                    override fun attachEvent(view: View, viewHolder: RecyclerView.ViewHolder) {
                        view.setOnClickListener {
                            expandableExtension.toggleExpandable(
                                viewHolder.bindingAdapterPosition,
                                true
                            )
                        }
                    }
                }
            )
        } else {
            fastAdapter.onClickListener = { _, _, item, position ->
                if (item is FolderItem) {
                    expandableExtension.toggleExpandable(position, true)
                    true
                } else {
                    false
                }
            }
        }

        selectExtension.run {
            isSelectable = true
            allowDeselection = true
            multiSelect = multi
            selectWithItemUpdate = true
            selectionListener = object : ISelectionListener<GenericItem> {
                override fun onSelectionChanged(item: GenericItem, selected: Boolean) {
                    when (item) {
                        is FolderItem -> {
                            val subItems = item.subItems
                            subItems.forEach {
                                if (it.isSelected != selected) {
                                    it.isSelected = selected
                                }
                            }
                            if (item.isExpanded) {
                                fastAdapter.notifyItemRangeChanged(
                                    fastAdapter.getPosition(item) + 1,
                                    subItems.size
                                )
                            }
                        }
                        is TimerItem -> {
                            val folderItem = item.parent as FolderItem
                            val shouldFolderBeSelected = folderItem.subItems.all { it.isSelected }
                            if (folderItem.isSelected != shouldFolderBeSelected) {
                                folderItem.isSelected = shouldFolderBeSelected
                                fastAdapter.notifyItemChanged(fastAdapter.getPosition(folderItem))
                            }
                        }
                    }
                }
            }
        }

        binding.listTimerPicker.run {
            adapter = fastAdapter
            // CheckBox's animation will be gone when this is enabled.
            (itemAnimator as DefaultItemAnimator).supportsChangeAnimations = false
        }

        val selectLayoutRes = if (multi) RTools.layout.widget_check else RTools.layout.widget_radio
        val select = arguments.getIntArray(EXTRA_SELECT) ?: intArrayOf()
        viewModel.folderTimers.observe(this) { map: Map<FolderEntity, List<TimerInfo>> ->
            itemAdapter.set(mutableListOf<GenericItem>().apply {
                map.filter { it.value.isNotEmpty() }
                    .forEach { (folder, timers) ->
                        val folderItem = FolderItem(folder, selectable = multi)
                        val subItems = folderItem.subItems
                        timers.forEach { timer ->
                            subItems.add(
                                TimerItem(
                                    timerInfo = timer,
                                    selectLayoutRes = selectLayoutRes
                                ).apply {
                                    isSelected = timer.id in select
                                }
                            )
                        }
                        folderItem.isSelected = subItems.all { it.isSelected }
                        add(folderItem)
                    }
            })
            for (index in fastAdapter.itemCount downTo 0) {
                val item = fastAdapter.getItem(index) ?: continue
                if (item is FolderItem && !item.folderEntity.isTrash) {
                    expandableExtension.expand(index, true)
                }
            }
        }

        return dialog
    }

    override fun onPause() {
        super.onPause()
        dismiss()
    }

    companion object {
        private const val EXTRA_MULTI = "multi"
        private const val EXTRA_SELECT = "select"

        fun createTimerPicker(
            multi: Boolean,
            select: List<Int>,
            f: (AppNavigator.PickTimerResult) -> Unit
        ): TimerPicker {
            return TimerPicker().apply {
                arguments = bundleOf(
                    EXTRA_MULTI to multi,
                    EXTRA_SELECT to select.toIntArray()
                )
                callback = f
            }
        }
    }
}

private class FolderItem(
    val folderEntity: FolderEntity,
    private val selectable: Boolean
) : AbstractExpandableItem<FolderItem.ViewHolder>() {
    override val type: Int = R.layout.list_item_timer_picker_folder
    override val layoutRes: Int = R.layout.list_item_timer_picker_folder
    override fun getViewHolder(v: View): ViewHolder = ViewHolder(v).apply {
        binding.viewTimerPickerSelection.isVisible = selectable
    }

    override var isSelectable: Boolean = selectable
    override val isAutoExpanding: Boolean = false

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.run {
            binding.run {
                textTimerPickerName.text = folderEntity.getDisplayName(root.context)
                imageTimerPickerExpand.rotation = if (isExpanded) 180f else 0f
                (viewTimerPickerSelection as CompoundButton).isChecked = isSelected
            }
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ListItemTimerPickerFolderBinding.bind(view)
    }
}

private class TimerItem(
    val timerInfo: TimerInfo,
    @LayoutRes private val selectLayoutRes: Int
) : AbstractExpandableItem<TimerItem.ViewHolder>() {
    override val type: Int = R.layout.list_item_timer_picker_timer
    override val layoutRes: Int = R.layout.list_item_timer_picker_timer
    override fun getViewHolder(v: View): ViewHolder = ViewHolder(v, selectLayoutRes)

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.run {
            binding.textTimerPickerName.text = timerInfo.name
            selection.isChecked = isSelected
        }
    }

    class ViewHolder(view: View, @LayoutRes layoutRes: Int) : RecyclerView.ViewHolder(view) {
        val binding = ListItemTimerPickerTimerBinding.bind(view)
        val selection: CompoundButton =
            view.findViewById<ViewStub>(R.id.stubTimerPickerSelection).run {
                layoutResource = layoutRes
                inflate() as CompoundButton
            }
    }
}
