package xyz.aprildown.timer.app.timer.list

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.core.text.buildSpannedString
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.github.deweyreed.tools.anko.dp
import com.github.deweyreed.tools.anko.snackbar
import com.github.deweyreed.tools.arch.observeEvent
import com.github.deweyreed.tools.arch.observeNonNull
import com.github.deweyreed.tools.helper.IntentHelper
import com.github.deweyreed.tools.helper.gone
import com.github.deweyreed.tools.helper.hasPermissions
import com.github.deweyreed.tools.helper.show
import com.github.deweyreed.tools.helper.startActivityOrNothing
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest
import xyz.aprildown.timer.app.base.data.PreferenceData
import xyz.aprildown.timer.app.base.data.PreferenceData.showGridTimerList
import xyz.aprildown.timer.app.base.ui.AppNavigator
import xyz.aprildown.timer.app.base.ui.ListEmptyView
import xyz.aprildown.timer.app.base.ui.MainCallback
import xyz.aprildown.timer.app.base.ui.SpecialItemTouchHelperCallback
import xyz.aprildown.timer.app.base.utils.NavigationUtils.createMainFragmentNavOptions
import xyz.aprildown.timer.app.base.utils.NavigationUtils.subLevelNavigate
import xyz.aprildown.timer.app.base.utils.ScreenWakeLock
import xyz.aprildown.timer.app.base.utils.ShortcutHelper
import xyz.aprildown.timer.app.base.utils.getDisplayName
import xyz.aprildown.timer.app.timer.list.databinding.FragmentTimerBinding
import xyz.aprildown.timer.app.timer.list.databinding.ViewTipAndroid12Binding
import xyz.aprildown.timer.app.timer.list.databinding.ViewTipMissedTimerBinding
import xyz.aprildown.timer.app.timer.list.databinding.ViewTipWhitelistBinding
import xyz.aprildown.timer.domain.entities.FolderEntity
import xyz.aprildown.timer.domain.entities.FolderSortBy
import xyz.aprildown.timer.domain.usecases.Fruit
import xyz.aprildown.timer.domain.usecases.home.TipManager
import xyz.aprildown.timer.presentation.stream.MachineContract
import xyz.aprildown.timer.presentation.stream.StreamState
import xyz.aprildown.timer.presentation.timer.TimerViewModel
import xyz.aprildown.tools.helper.safeSharedPreference
import javax.inject.Inject
import xyz.aprildown.timer.app.base.R as RBase

@AndroidEntryPoint
class TimerFragment :
    Fragment(R.layout.fragment_timer),
    MenuProvider,
    MainCallback.FragmentCallback,
    TimerAdapter.Callback {

    private lateinit var mainCallback: MainCallback.ActivityCallback

    private val viewModel: TimerViewModel by viewModels()

    @Inject
    lateinit var appNavigator: AppNavigator

    private var isBind = false

    private var contextMenuItemPosition = RecyclerView.NO_POSITION

    private val listAdapter: TimerAdapter?
        get() = view?.findViewById<RecyclerView>(R.id.listTimers)?.adapter as? TimerAdapter

    private var postNotificationsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainCallback = context as MainCallback.ActivityCallback
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().addMenuProvider(this, this, Lifecycle.State.STARTED)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.timer, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_record -> {
                NavHostFragment.findNavController(this)
                    .subLevelNavigate(RBase.id.dest_record)
                true
            }
            else -> false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = view.context
        val binding = FragmentTimerBinding.bind(view)
        setUpMainActions()
        setUpRecyclerView(binding)
        setUpFolderToolbar(binding)
        setUpGridOrList(binding, context.safeSharedPreference.showGridTimerList)
        setUpObservers()
        setUpTips(binding)
    }

    override fun onStart() {
        super.onStart()
        val activity = requireActivity()
        if (!isBind) {
            isBind = activity.bindService(
                viewModel.getBindIntent(),
                mConnection,
                Context.BIND_AUTO_CREATE
            )
        }
    }

    override fun onResume() {
        super.onResume()
        ScreenWakeLock.acquireScreenWakeLock(
            context = requireActivity(),
            screenTiming = getString(RBase.string.pref_screen_timing_value_timer)
        )
    }

    override fun onPause() {
        super.onPause()
        ScreenWakeLock.releaseScreenLock(
            context = requireActivity(),
            screenTiming = getString(RBase.string.pref_screen_timing_value_timer)
        )
    }

    override fun onStop() {
        super.onStop()
        if (isBind) {
            isBind = false
            requireActivity().unbindService(mConnection)
            listAdapter?.dropPresenter()
        }
    }

    override fun onFabClick(view: View) {
        viewModel.addNewTimer()
    }

    private fun setUpMainActions() {
        mainCallback.actionFab.contentDescription = getString(RBase.string.edit_create_timer)
    }

    private fun setUpRecyclerView(binding: FragmentTimerBinding) {
        val context = binding.root.context
        val timerAdapter = TimerAdapter(this@TimerFragment, Dispatchers.Default.asExecutor())
        binding.listTimers.let { list ->
            list.adapter = timerAdapter
            registerForContextMenu(list)
            list.itemAnimator?.run {
                val duration = 180L
                addDuration = duration
                changeDuration = duration
                moveDuration = duration
                removeDuration = duration
            }
        }
        ItemTouchHelper(
            SpecialItemTouchHelperCallback(
                context,
                SpecialItemTouchHelperCallback.Config.getEditDeleteConfig(context).apply {
                    val padding = context.dp(8).toInt()
                    topPadding = padding
                    bottomPadding = padding
                },
                canBeSwiped = {
                    when (timerAdapter.getItemViewType(it.absoluteAdapterPosition)) {
                        R.layout.list_item_timer_collapsed,
                        R.layout.list_item_timer_expanded -> true
                        else -> false
                    }
                },
                startSwipeCallback = object : SpecialItemTouchHelperCallback.SwipeCallback {
                    override fun onSwipe(viewHolder: RecyclerView.ViewHolder) {
                        val pos = viewHolder.bindingAdapterPosition
                        if (pos in 0 until timerAdapter.itemCount) {
                            val mutableTimerItem = timerAdapter.getItemAt(pos)
                            viewModel.stopAction(
                                mutableTimerItem.timerId,
                                mutableTimerItem.state
                            )
                            deleteTimerAt(pos)
                        }
                    }
                },
                endSwipeCallback = object : SpecialItemTouchHelperCallback.SwipeCallback {
                    override fun onSwipe(viewHolder: RecyclerView.ViewHolder) {
                        val pos = viewHolder.bindingAdapterPosition
                        if (pos in 0 until timerAdapter.itemCount) {
                            val mutableTimerItem = timerAdapter.getItemAt(pos)
                            val id = mutableTimerItem.timerId
                            viewModel.stopAction(id, mutableTimerItem.state)
                            viewModel.openTimerEditScreen(id)
                            timerAdapter.notifyItemChanged(pos)
                        }
                    }
                },
            )
        ).attachToRecyclerView(binding.listTimers)

        viewModel.timerInfo.observe(viewLifecycleOwner) { timerInfo ->
            val newList = (timerInfo ?: emptyList()).map {
                MutableTimerItem(
                    timerInfo = it,
                    timerItem = null,
                    state = StreamState.RESET,
                    isExpanded = false
                )
            }
            TransitionManager.beginDelayedTransition(
                binding.root,
                AutoTransition().apply {
                    excludeChildren(binding.listTimers, true)
                }
            )
            timerAdapter.setItems(newList)

            val isEmpty = newList.isEmpty()
            binding.viewEmpty.isVisible = isEmpty &&
                viewModel.tips.value.let { it == null || it == TipManager.TIP_NO_MORE }
            if (isEmpty) {
                binding.viewEmpty.mode =
                    if (viewModel.currentFolderId.value == FolderEntity.FOLDER_TRASH) {
                        ListEmptyView.MODE_DELETE
                    } else {
                        ListEmptyView.MODE_CREATE
                    }
            }
        }
    }

    override fun onTimerAction(viewHolder: RecyclerView.ViewHolder, actionId: Int) {
        val listAdapter = listAdapter ?: return
        val view = viewHolder.itemView
        val position = viewHolder.bindingAdapterPosition
        if (position !in 0 until listAdapter.itemCount) return
        when (actionId) {
            TimerAdapter.ACTION_ITEM_CLICK -> {
                listAdapter.getItemAt(position).let {
                    mainCallback.enterTimerScreen(view, it.timerId)
                }
            }
            TimerAdapter.ACTION_COLLAPSED_START -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !view.context.hasPermissions(Manifest.permission.POST_NOTIFICATIONS) &&
                    !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
                ) {
                    postNotificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    val mutableTimerItem = listAdapter.getItemAt(position)
                    if (mutableTimerItem.state.isReset) {
                        listAdapter.expand(position)
                        viewModel.startPauseAction(
                            mutableTimerItem.timerId,
                            StreamState.RESET
                        )
                    }
                }
            }
            TimerAdapter.ACTION_CONTEXT_MENU -> {
                contextMenuItemPosition = position
            }
            TimerAdapter.ACTION_EXPANDED_START_PAUSE -> {
                val timer = listAdapter.getItemAt(position)
                viewModel.startPauseAction(timer.timerId, timer.state)
            }
            TimerAdapter.ACTION_EXPANDED_STOP -> {
                val timer = listAdapter.getItemAt(position)
                viewModel.stopAction(timer.timerId, timer.state)
            }
        }
    }

    private fun setUpFolderToolbar(binding: FragmentTimerBinding) {
        val folderToolbar = binding.toolbarTimerFolder
        val context = folderToolbar.context

        folderToolbar.callback = object : FolderToolbar.Callback {
            override fun onGetFolders(): List<FolderEntity> {
                return viewModel.allFolders.value ?: emptyList()
            }

            override fun onChangeFolder(folderId: Long) {
                viewModel.changeFolder(folderId)
            }

            override fun onChangeCurrentFolderName(newName: String) {
                viewModel.changeCurrentFolderName(newName)
            }

            override fun onCreateNewFolder(name: String) {
                viewModel.createNewFolder(name)
            }

            override fun onDeleteCurrentFolder() {
                val currentFolderId = viewModel.currentFolderId.value ?: return
                val isInTheTrash = currentFolderId == FolderEntity.FOLDER_TRASH

                val builder = MaterialAlertDialogBuilder(context)
                    .setTitle(
                        if (isInTheTrash) {
                            RBase.string.folder_empty_trash_confirmation
                        } else {
                            RBase.string.folder_trash_folder_confirmation
                        }
                    )

                if (isInTheTrash) {
                    builder.setMessage(RBase.string.folder_delete_timer_explanation)
                }

                builder
                    .setPositiveButton(RBase.string.ok) { _, _ ->
                        if (isInTheTrash) {
                            viewModel.timerInfo.value?.forEach { timerInfo ->
                                ShortcutHelper.disableTimerShortcut(context, timerInfo.id)
                            }
                        }
                        viewModel.deleteCurrentFolder()
                    }
                    .setNegativeButton(RBase.string.cancel, null)

                builder.show()
            }

            override fun onChangeSort(sortBy: FolderSortBy) {
                viewModel.changeSortBy(sortBy)
            }

            override fun onToggleGridView(toGrid: Boolean) {
                setUpGridOrList(binding, toGrid)
                context.safeSharedPreference.edit {
                    putBoolean(PreferenceData.PREF_GRID_TIMER_LIST, toGrid)
                }
            }
        }

        viewModel.currentFolder.observeNonNull(viewLifecycleOwner) { folder ->
            folderToolbar.run {
                setCurrentFolderName(folder.getDisplayName(context))
                canCurrentFolderBeRenamed = !folder.isDefault && !folder.isTrash
            }

            if (folder.isTrash) {
                mainCallback.actionFab.hide()
            } else {
                mainCallback.actionFab.show()
            }
        }
    }

    private fun setUpGridOrList(binding: FragmentTimerBinding, grid: Boolean) {
        val context = binding.root.context
        binding.listTimers.run {
            if (grid) {
                (layoutManager as GridLayoutManager).spanCount =
                    resources.getInteger(R.integer.timer_list_grid_count)
                val padding = context.dp(8).toInt()
                updatePadding(left = padding, right = padding)
                listAdapter?.showGrid = true
                binding.toolbarTimerFolder.setGridView(showGrid = false)
            } else {
                (layoutManager as GridLayoutManager).spanCount = 1
                updatePadding(left = 0, right = 0)
                listAdapter?.showGrid = false
                binding.toolbarTimerFolder.setGridView(showGrid = true)
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val listAdapter = listAdapter
        var timer: MutableTimerItem? = null
        val position = contextMenuItemPosition
        contextMenuItemPosition = RecyclerView.NO_POSITION
        if (listAdapter != null && position in 0 until listAdapter.itemCount) {
            timer = listAdapter.getItemAt(position)
        }

        if (timer == null) return false

        when (item.itemId) {
            CollapsedViewHolder.MENU_ID_EDIT -> {
                viewModel.openTimerEditScreen(timer.timerId)
            }
            CollapsedViewHolder.MENU_ID_DUPLICATE -> {
                viewModel.duplicate(timer.timerId)
            }
            CollapsedViewHolder.MENU_ID_MOVE -> {
                val currentFolderId = viewModel.currentFolderId.value ?: return false
                val allFolders = viewModel.allFolders.value ?: return false
                MaterialAlertDialogBuilder(requireContext())
                    .setItems(
                        allFolders
                            .map { it.getDisplayName(requireContext()) }
                            .toTypedArray()
                    ) { _, which ->
                        val targetFolderId = allFolders[which].id
                        if (targetFolderId != currentFolderId) {
                            viewModel.moveTimerToFolder(
                                timerId = timer.timerId,
                                folderId = allFolders[which].id
                            )
                        }
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
            CollapsedViewHolder.MENU_ID_DELETE -> {
                deleteTimerAt(position)
            }
            CollapsedViewHolder.MENU_ID_SHARE -> {
                viewModel.generateTimerString(timer.timerId)
            }
            else -> return false
        }
        return true
    }

    private fun deleteTimerAt(position: Int) {
        val listAdapter = listAdapter ?: return
        if (position !in 0 until listAdapter.itemCount) return
        val timerItem = listAdapter.getItemAt(position)

        val currentFolderId = viewModel.currentFolderId.value ?: return

        val timerId = timerItem.timerId
        if (currentFolderId != FolderEntity.FOLDER_TRASH) {
            viewModel.moveTimerToFolder(
                timerId = timerId,
                folderId = FolderEntity.FOLDER_TRASH
            )
            mainCallback.snackbarView.snackbar(
                message = getString(RBase.string.trash_done_template, timerItem.timerName),
                actionText = getString(RBase.string.undo),
                action = {
                    viewModel.moveTimerToFolder(
                        timerId = timerId,
                        folderId = currentFolderId
                    )
                }
            )
        } else {
            val context = requireContext()
            MaterialAlertDialogBuilder(context)
                .setCancelable(false)
                .setMessage(
                    buildSpannedString {
                        append(getString(RBase.string.delete_confirmation_template).format(timerItem.timerName))
                        append("\n\n")
                        append(
                            getString(RBase.string.folder_delete_timer_explanation),
                            StyleSpan(Typeface.BOLD),
                            Spanned.SPAN_INCLUSIVE_INCLUSIVE
                        )
                    }
                )
                .setPositiveButton(RBase.string.ok) { _, _ ->
                    ShortcutHelper.disableTimerShortcut(context, timerId)
                    viewModel.deleteTimer(timerId)
                }
                .setNegativeButton(RBase.string.cancel) { _, _ ->
                    listAdapter.notifyItemChanged(position)
                }
                .show()
        }
    }

    private fun setUpObservers() {
        viewModel.editEvent.observeEvent(viewLifecycleOwner) {
            mainCallback.enterEditScreen(
                timerId = it,
                folderId = viewModel.currentFolderId.value ?: FolderEntity.FOLDER_DEFAULT
            )
        }
        viewModel.intentEvent.observeEvent(viewLifecycleOwner) {
            requireContext().startService(it)
        }
        viewModel.shareStringEvent.observeEvent(viewLifecycleOwner) { fruit ->
            when (fruit) {
                is Fruit.Ripe -> {
                    startActivityOrNothing(IntentHelper.share(requireActivity(), fruit.data))
                }
                is Fruit.Rotten -> {
                    mainCallback.snackbarView.snackbar(fruit.exception.message.toString())
                }
            }
        }
    }

    private fun setUpTips(binding: FragmentTimerBinding) {
        val context = binding.root.context
        viewModel.tips.observe(viewLifecycleOwner) { tip ->
            binding.layoutTip.show()
            binding.layoutTip.removeAllViews()
            when (tip) {
                TipManager.TIP_TUTORIAL -> {
                    startActivity(appNavigator.getIntroIntent(isOnBoarding = true))
                    viewModel.consumeTip(TipManager.TIP_TUTORIAL)
                }
                TipManager.TIP_WHITELIST -> {
                    binding.viewEmpty.gone()
                    ViewTipWhitelistBinding.inflate(
                        layoutInflater,
                        binding.layoutTip,
                        true
                    ).also {
                        it.btnCheck.setOnClickListener {
                            findNavController().navigate(
                                RBase.id.dest_whitelist,
                                null,
                                createMainFragmentNavOptions(RBase.id.dest_whitelist)
                            )
                            viewModel.consumeTip(tip)
                        }
                        it.btnSkip.setOnClickListener {
                            MaterialAlertDialogBuilder(context)
                                .setTitle(RBase.string.whitelist_disclaimer_no_confirmation)
                                .setMessage(RBase.string.whitelist_location)
                                .setPositiveButton(RBase.string.ok) { _, _ ->
                                    viewModel.consumeTip(tip)
                                }
                                .setNegativeButton(RBase.string.cancel, null)
                                .show()
                        }
                    }
                }
                TipManager.TIP_MISSED_TIMER -> {
                    binding.viewEmpty.gone()
                    ViewTipMissedTimerBinding.inflate(
                        layoutInflater,
                        binding.layoutTip,
                        true
                    ).also {
                        it.btnCheck.setOnClickListener {
                            findNavController().navigate(
                                RBase.id.dest_whitelist,
                                null,
                                createMainFragmentNavOptions(RBase.id.dest_whitelist)
                            )
                            viewModel.consumeTip(tip)
                        }
                        it.btnDismiss.setOnClickListener {
                            viewModel.consumeTip(tip)
                        }
                    }
                }
                TipManager.TIP_ANDROID_12 -> {
                    ViewTipAndroid12Binding.inflate(
                        layoutInflater,
                        binding.layoutTip,
                        true
                    ).also {
                        it.groupPhoneCalls.isVisible =
                            !context.hasPermissions(Manifest.permission.READ_PHONE_STATE)
                        it.btnPhoneCalls.setOnClickListener {
                            EasyPermissions.requestPermissions(
                                PermissionRequest.Builder(
                                    this,
                                    0,
                                    Manifest.permission.READ_PHONE_STATE
                                ).build()
                            )
                        }
                        it.groupBattery.isVisible = context.getSystemService<PowerManager>()
                            ?.isIgnoringBatteryOptimizations(context.packageName) != true
                        it.btnBattery.setOnClickListener {
                            context.startActivityOrNothing(
                                Intent.createChooser(
                                    @Suppress("BatteryLife")
                                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                                        .setData("package:${context.packageName}".toUri()),
                                    null
                                ),
                                wrongMessageRes = RBase.string.no_action_found
                            )
                        }
                        it.btnDismiss.setOnClickListener {
                            viewModel.consumeTip(tip)
                        }
                        if (!it.groupPhoneCalls.isVisible && !it.groupBattery.isVisible) {
                            viewModel.consumeTip(tip)
                        }
                    }
                }
                TipManager.TIP_NO_MORE -> {
                    binding.layoutTip.gone()
                    binding.viewEmpty.isVisible = viewModel.timerInfo.value?.isEmpty() == true
                }
            }
        }
    }

    private val mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            listAdapter?.setPresenter((service as MachineContract.PresenterProvider).getPresenter())
        }

        override fun onServiceDisconnected(name: ComponentName?) = Unit
    }
}
