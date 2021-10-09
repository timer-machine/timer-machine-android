package xyz.aprildown.timer.flavor.google.count

import android.graphics.Typeface
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import androidx.core.text.buildSpannedString
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import xyz.aprildown.timer.flavor.google.BillingActivity
import xyz.aprildown.timer.flavor.google.BillingSupervisor
import xyz.aprildown.timer.flavor.google.databinding.DialogBakedCountBinding
import xyz.aprildown.timer.flavor.google.showErrorDialog
import xyz.aprildown.timer.flavor.google.utils.IapPromotionDialog
import xyz.aprildown.timer.flavor.google.utils.causeFirstMessage
import xyz.aprildown.tools.anko.snackbar
import xyz.aprildown.tools.arch.observeEvent
import xyz.aprildown.tools.helper.gone
import xyz.aprildown.tools.helper.show
import xyz.aprildown.timer.app.base.R as RBase

internal class BakedCountDialog {

    fun show(fragment: Fragment) {
        val context = fragment.requireContext()
        val viewModel: BakedCountViewModel by fragment.viewModels()

        viewModel.init()

        val billingSupervisor = BillingSupervisor(context, requestProState = true)
        billingSupervisor.withLifecycleOwner(fragment)
        billingSupervisor.supervise()

        val binding = DialogBakedCountBinding.inflate(LayoutInflater.from(context))

        val dialog = MaterialAlertDialogBuilder(context)
            .setView(binding.root)
            .show()

        binding.btnDisable.setOnClickListener { viewModel.disable() }
        binding.btnCancel.setOnClickListener { dialog.dismiss() }

        viewModel.state.observe(fragment) { state ->
            TransitionManager.beginDelayedTransition(
                binding.root,
                AutoTransition().apply {
                    ordering = TransitionSet.ORDERING_TOGETHER
                }
            )
            billingSupervisor.proState.removeObservers(fragment)
            when (state) {
                BakedCountViewModel.STATE_ENABLED -> {
                    dialog.setCancelable(true)
                    binding.progress.gone()
                    binding.textDownloading.gone()
                    binding.btnEnable.gone()
                    binding.btnDisable.show()
                }
                BakedCountViewModel.STATE_LOADING_PRO_STATE -> {
                    dialog.setCancelable(true)
                    binding.progress.show()
                    binding.textDownloading.gone()
                    binding.btnEnable.gone()
                    binding.btnDisable.gone()

                    billingSupervisor.proState.observe(fragment) {
                        viewModel.proStateUpdated(it)
                    }
                }
                BakedCountViewModel.STATE_DISABLED -> {
                    dialog.setCancelable(true)
                    binding.progress.gone()
                    binding.textDownloading.gone()
                    binding.btnEnable.show()
                    binding.btnEnable.setText(RBase.string.enable)
                    binding.btnEnable.setOnClickListener {
                        viewModel.download()
                    }
                    binding.btnDisable.gone()
                }
                BakedCountViewModel.STATE_TO_BE_PURCHASED -> {
                    dialog.setCancelable(true)
                    binding.progress.gone()
                    binding.textDownloading.gone()
                    binding.btnEnable.show()
                    binding.btnEnable.setText(RBase.string.billing_purchase)
                    binding.btnEnable.setOnClickListener {
                        fragment.startActivity(BillingActivity.getIntent(context))
                    }
                    binding.btnDisable.gone()

                    billingSupervisor.proState.observe(fragment) {
                        viewModel.proStateUpdated(it)
                    }

                    if (viewModel.shouldShowPromotionDialog) {
                        viewModel.shouldShowPromotionDialog = false
                        IapPromotionDialog(context).show(
                            title = context.getString(RBase.string.billing_baked_count_title),
                            message = buildSpannedString {
                                append(context.getString(RBase.string.billing_baked_count_desp))
                                append("\n\n")
                                append(
                                    context.getString(RBase.string.billing_a_part_of_iap),
                                    StyleSpan(Typeface.BOLD),
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                )
                            },
                            positiveButtonTextRes = RBase.string.billing_purchase
                        ) {
                            fragment.startActivity(BillingActivity.getIntent(context))
                        }
                    }
                }
                BakedCountViewModel.STATE_DOWNLOADING -> {
                    dialog.setCancelable(false)
                    binding.progress.show()
                    binding.textDownloading.show()
                    binding.btnEnable.gone()
                    binding.btnDisable.gone()
                }
            }
        }

        billingSupervisor.error.observeEvent(fragment) {
            context.showErrorDialog(it)
        }
        viewModel.error.observeEvent(fragment) {
            binding.root.snackbar(it.causeFirstMessage())
        }

        dialog.setOnDismissListener {
            viewModel.state.removeObservers(fragment)
            viewModel.error.removeObservers(fragment)
            billingSupervisor.proState.removeObservers(fragment)
            billingSupervisor.error.removeObservers(fragment)
        }
    }
}
