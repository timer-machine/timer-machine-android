package xyz.aprildown.timer.app.timer.list

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import androidx.annotation.StringRes
import com.github.zawadz88.materialpopupmenu.popupMenu
import com.google.android.material.card.MaterialCardView
import xyz.aprildown.timer.app.base.utils.getDisplayName
import xyz.aprildown.timer.app.timer.list.databinding.ViewFolderToolbarBinding
import xyz.aprildown.timer.domain.entities.FolderEntity
import xyz.aprildown.timer.domain.entities.FolderSortBy
import xyz.aprildown.tools.helper.dimen
import xyz.aprildown.tools.view.SimpleInputDialog

internal class FolderToolbar(
    context: Context,
    attrs: AttributeSet? = null
) : MaterialCardView(context, attrs) {

    interface Callback {
        fun onGetFolders(): List<FolderEntity>
        fun onChangeFolder(folderId: Long)
        fun onCreateNewFolder(name: String)
        fun onChangeSort(sortBy: FolderSortBy)
        fun onChangeCurrentFolderName(newName: String)
        fun onDeleteCurrentFolder()
        fun onToggleGridView(toGrid: Boolean)
    }

    private val binding: ViewFolderToolbarBinding

    var canCurrentFolderBeRenamed = false

    var callback: Callback? = null

    init {
        radius = 0f
        cardElevation = context.dimen(R.dimen.elevation_timer_card).toFloat()
        binding = ViewFolderToolbarBinding.inflate(LayoutInflater.from(context), this)

        fun requestFolderName(f: (String) -> Unit) {
            SimpleInputDialog(context).show(titleRes = R.string.folder_name) {
                if (it.isNotBlank()) {
                    f.invoke(it)
                }
            }
        }

        binding.textFolderToolbarCurrent.setOnClickListener { view ->
            val folders = callback?.onGetFolders() ?: return@setOnClickListener
            if (folders.isEmpty()) return@setOnClickListener

            popupMenu {
                dropdownGravity = Gravity.TOP or Gravity.START
                section {
                    folders.forEach { folder ->
                        if (!folder.isTrash) {
                            item {
                                label = folder.getDisplayName(context)
                                callback = {
                                    this@FolderToolbar.callback?.onChangeFolder(folderId = folder.id)
                                }
                            }
                        }
                    }
                }
                section {
                    item {
                        labelRes = R.string.folder_trash
                        callback = {
                            this@FolderToolbar.callback?.onChangeFolder(folderId = FolderEntity.FOLDER_TRASH)
                        }
                    }
                }
            }.show(context, view)
        }

        binding.btnFolderToolbarGridList.setOnClickListener {
            callback?.onToggleGridView(
                toGrid = binding.btnFolderToolbarGridList.tag as? Boolean ?: true
            )
        }

        binding.btnFolderToolbarSort.setOnClickListener {
            popupMenu {
                dropdownGravity = Gravity.TOP or Gravity.END
                section {

                    fun addItem(@StringRes nameRes: Int, sortBy: FolderSortBy) {
                        item {
                            labelRes = nameRes
                            callback = {
                                this@FolderToolbar.callback?.onChangeSort(sortBy)
                            }
                        }
                    }

                    addItem(R.string.folder_sort_added_newest, FolderSortBy.AddedNewest)
                    addItem(R.string.folder_sort_added_oldest, FolderSortBy.AddedOldest)
                    addItem(R.string.folder_sort_run_newest, FolderSortBy.RunNewest)
                    addItem(R.string.folder_sort_run_oldest, FolderSortBy.RunOldest)
                    addItem(R.string.folder_sort_a_to_z, FolderSortBy.AToZ)
                    addItem(R.string.folder_sort_z_to_a, FolderSortBy.ZToA)
                }
            }.show(context, it)
        }

        binding.btnFolderToolbarMore.setOnClickListener { view ->
            popupMenu {
                dropdownGravity = Gravity.TOP or Gravity.END
                section {
                    item {
                        labelRes = R.string.folder_new_folder
                        callback = {
                            requestFolderName {
                                this@FolderToolbar.callback?.onCreateNewFolder(it)
                            }
                        }
                    }
                    if (canCurrentFolderBeRenamed) {
                        item {
                            labelRes = R.string.folder_rename
                            callback = {
                                requestFolderName {
                                    this@FolderToolbar.callback?.onChangeCurrentFolderName(it)
                                }
                            }
                        }
                    }
                    item {
                        labelRes = R.string.folder_delete
                        callback = {
                            this@FolderToolbar.callback?.onDeleteCurrentFolder()
                        }
                    }
                }
            }.show(context, view)
        }
    }

    fun setCurrentFolderName(name: String) {
        binding.textFolderToolbarCurrent.text = name
    }

    fun setGridView(showGrid: Boolean) {
        binding.btnFolderToolbarGridList.run {
            tag = showGrid
            if (showGrid) {
                contentDescription = context.getString(R.string.folder_to_grid_view)
                setImageResource(R.drawable.ic_grid_view)
            } else {
                contentDescription = context.getString(R.string.folder_to_list_view)
                setImageResource(R.drawable.ic_list_view)
            }
        }
    }
}
