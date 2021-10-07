package xyz.aprildown.timer.flavor.google.backup

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.text.format.DateUtils
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import dagger.hilt.android.AndroidEntryPoint
import xyz.aprildown.timer.app.base.ui.AppNavigator
import xyz.aprildown.timer.domain.usecases.Fruit
import xyz.aprildown.timer.flavor.google.R
import xyz.aprildown.timer.flavor.google.databinding.FragmentRestoreBinding
import xyz.aprildown.timer.flavor.google.databinding.ListItemRestoreBinding
import xyz.aprildown.timer.flavor.google.utils.causeFirstMessage
import xyz.aprildown.tools.helper.gone
import xyz.aprildown.tools.helper.restartWithFading
import xyz.aprildown.tools.helper.show
import javax.inject.Inject

@AndroidEntryPoint
internal class RestoreFragment : Fragment(R.layout.fragment_restore) {

    private val viewModel: CloudBackupViewModel by navGraphViewModels(R.id.dest_cloud_backup)

    @Inject
    lateinit var appNavigator: AppNavigator

    private var restoreDialog: AlertDialog? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = view.context
        val binding = FragmentRestoreBinding.bind(view)

        val itemAdapter = ItemAdapter<RestoreItem>()
        val fastAdapter = FastAdapter.with(itemAdapter)

        fastAdapter.onClickListener = { _, _, item, _ ->
            confirmToRestore(context = context, reference = item.reference)
            true
        }

        binding.listRestore.run {
            adapter = fastAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        binding.run {
            listRestore.gone()
            progressRestoreLoading.show()
            textRestoreMessage.gone()
        }

        viewModel.userFiles.observe(viewLifecycleOwner) { fruit ->
            when (fruit) {
                is Fruit.Ripe -> {
                    binding.progressRestoreLoading.gone()
                    val files = fruit.data
                    if (files.isEmpty()) {
                        binding.run {
                            listRestore.gone()
                            textRestoreMessage.show()
                            textRestoreMessage.setText(R.string.empty)
                        }
                    } else {
                        binding.run {
                            listRestore.show()
                            textRestoreMessage.gone()
                        }
                        itemAdapter.set(
                            files.map { RestoreItem(it.first, it.second) }
                        )
                    }
                }
                is Fruit.Rotten -> {
                    binding.run {
                        listRestore.gone()
                        progressRestoreLoading.gone()
                        textRestoreMessage.show()
                        textRestoreMessage.text = fruit.exception.causeFirstMessage()
                    }
                }
                else -> Unit
            }
        }

        viewModel.restoreResult.observe(viewLifecycleOwner) { fruit ->
            when (fruit) {
                is Fruit.Ripe -> {
                    restoreDialog?.dismiss()
                    restoreDialog = null

                    MaterialAlertDialogBuilder(context)
                        .setCancelable(false)
                        .setTitle(R.string.import_done)
                        .setMessage(R.string.import_restart_content)
                        .setPositiveButton(R.string.import_restart) { _, _ ->
                            val activity = requireActivity()
                            activity.restartWithFading(appNavigator.getMainIntent())
                        }
                        .show()
                }
                is Fruit.Rotten -> {
                    restoreDialog?.dismiss()
                    restoreDialog = null

                    MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.cloud_backup_restore_failed)
                        .setMessage(fruit.exception.causeFirstMessage())
                        .setPositiveButton(R.string.ok, null)
                        .show()
                }
            }
        }
    }

    private fun confirmToRestore(context: Context, reference: StorageReference) {
        val dialog = MaterialAlertDialogBuilder(context)
            .setMessage(R.string.cloud_backup_restore_alert)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .show()
        val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        positiveButton.isEnabled = false

        val timer = object : CountDownTimer(3000, 1000) {
            private var remainingSeconds = 3
            override fun onTick(millisUntilFinished: Long) {
                positiveButton.text = getString(R.string.count_down_template).format(
                    getString(R.string.ok),
                    remainingSeconds
                )
                remainingSeconds -= 1
            }

            override fun onFinish() {
                positiveButton.setText(R.string.ok)
                positiveButton.isEnabled = true
            }
        }
        timer.start()

        positiveButton.setOnClickListener {
            restoreDialog = MaterialAlertDialogBuilder(context)
                .setCancelable(false)
                .setTitle(R.string.cloud_backup_restoring)
                .setView(R.layout.dialog_loading)
                .setNegativeButton(R.string.cancel) { _, _ ->
                    viewModel.cancelRestoring()
                }
                .show()

            viewModel.startRestoring(reference)
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            timer.cancel()
        }
    }

    private class RestoreItem(
        val reference: StorageReference,
        val metaData: StorageMetadata
    ) : AbstractBindingItem<ListItemRestoreBinding>() {
        override val type: Int = R.layout.list_item_restore
        override fun createBinding(
            inflater: LayoutInflater,
            parent: ViewGroup?
        ): ListItemRestoreBinding {
            return ListItemRestoreBinding.inflate(inflater, parent, false)
        }

        override fun bindView(binding: ListItemRestoreBinding, payloads: List<Any>) {
            super.bindView(binding, payloads)
            binding.itemRestoreItem.run {
                setPrimaryText(
                    DateUtils.formatDateTime(
                        context,
                        metaData.updatedTimeMillis,
                        DateUtils.FORMAT_SHOW_YEAR or
                            DateUtils.FORMAT_SHOW_DATE or
                            DateUtils.FORMAT_SHOW_TIME
                    )
                )
                setSecondaryText(
                    Formatter.formatFileSize(context, metaData.sizeBytes)
                )
            }
        }
    }
}
