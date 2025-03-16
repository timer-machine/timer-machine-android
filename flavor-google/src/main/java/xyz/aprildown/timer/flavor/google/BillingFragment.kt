package xyz.aprildown.timer.flavor.google

import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ClickableSpan
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.core.text.method.LinkMovementMethodCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.transition.TransitionManager
import com.github.deweyreed.tools.anko.newTask
import com.github.deweyreed.tools.anko.snackbar
import com.github.deweyreed.tools.arch.observeEvent
import com.github.deweyreed.tools.helper.IntentHelper
import com.github.deweyreed.tools.helper.createChooserIntentIfDead
import com.github.deweyreed.tools.helper.gone
import com.github.deweyreed.tools.helper.show
import com.github.deweyreed.tools.helper.startActivityOrNothing
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import xyz.aprildown.timer.app.base.data.FlavorData
import xyz.aprildown.timer.app.base.utils.openLink
import xyz.aprildown.timer.component.key.PreferenceStyleListFragment
import xyz.aprildown.timer.domain.utils.Constants
import xyz.aprildown.timer.flavor.google.databinding.FragmentBillingBinding
import javax.inject.Inject
import xyz.aprildown.timer.app.base.R as RBase

@AndroidEntryPoint
internal class BillingFragment : Fragment(R.layout.fragment_billing), MenuProvider {

    @Inject
    lateinit var flavorData: FlavorData

    private lateinit var billingSupervisor: BillingSupervisor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().addMenuProvider(this, this, Lifecycle.State.STARTED)

        billingSupervisor = BillingSupervisor(
            requireContext(),
            requireSkuDetails = true,
            requestProState = true,
            requestBackupSubState = true,
            // consumeInAppPurchases = true,
        )
        billingSupervisor.withLifecycleOwner(this)
        billingSupervisor.supervise()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentBillingBinding.bind(view)

        if (savedInstanceState == null) {
            setUpEntries()
        }

        setUpBillingStates(binding)
    }

    private fun setUpBillingStates(binding: FragmentBillingBinding) {
        val context = binding.root.context

        billingSupervisor.proState.observe(viewLifecycleOwner) { hasPro ->
            TransitionManager.beginDelayedTransition(binding.cardPro)

            billingSupervisor.proSkuDetails.removeObservers(viewLifecycleOwner)
            if (hasPro) {
                binding.textProPrice.gone()
                binding.btnProPurchase.isEnabled = false
                binding.btnProPurchase.setText(RBase.string.billing_owned)
                binding.btnProPurchase.setOnClickListener(null)
            } else {
                billingSupervisor.proSkuDetails.observe(viewLifecycleOwner) { proSkuDetails ->
                    TransitionManager.beginDelayedTransition(binding.cardPro)

                    binding.textProPrice.show()
                    binding.textProPrice.text =
                        proSkuDetails.oneTimePurchaseOfferDetails?.formattedPrice
                    binding.btnProPurchase.isEnabled = true
                    binding.btnProPurchase.setText(RBase.string.billing_purchase)
                    binding.btnProPurchase.setOnClickListener {
                        billingSupervisor.launchBillingFlow(requireActivity(), proSkuDetails)
                    }
                }
            }
        }
        billingSupervisor.goProEvent.observeEvent(viewLifecycleOwner) {
            binding.root.snackbar(RBase.string.thanks)
        }

        billingSupervisor.backupSubState.observe(viewLifecycleOwner) { hasBackupSub ->
            TransitionManager.beginDelayedTransition(binding.cardBackupSub)

            billingSupervisor.backupSubSkuDetails.removeObservers(viewLifecycleOwner)
            if (hasBackupSub) {
                binding.textBackupSubPrice.gone()
                binding.btnBackupSubSubscribe.isEnabled = false
                binding.btnBackupSubSubscribe.setText(RBase.string.billing_owned)
                binding.btnBackupSubManage.show()
                binding.textBackupSubCancelAnytime.gone()
                binding.textBackupSubPpTos.gone()
            } else {
                billingSupervisor.backupSubSkuDetails.observe(viewLifecycleOwner) { backupSubSkuDetails ->
                    TransitionManager.beginDelayedTransition(binding.cardBackupSub)

                    binding.textBackupSubPrice.show()
                    binding.textBackupSubPrice.text = buildString {
                        val target = backupSubSkuDetails.subscriptionOfferDetails
                            ?.firstOrNull()
                            ?.pricingPhases
                            ?.pricingPhaseList
                            ?.firstOrNull()
                        append(target?.formattedPrice)
                        // https://developer.android.com/reference/com/android/billingclient/api/ProductDetails.PricingPhase#getBillingPeriod()
                        if (target?.billingPeriod == "P1Y") {
                            append(getString(RBase.string.billing_per_year))
                        }
                    }
                    binding.btnBackupSubSubscribe.isEnabled = true
                    binding.btnBackupSubSubscribe.setText(RBase.string.billing_subscribe)
                    binding.btnBackupSubSubscribe.setOnClickListener {
                        billingSupervisor.launchBillingFlow(requireActivity(), backupSubSkuDetails)
                    }
                    binding.btnBackupSubManage.gone()
                    binding.textBackupSubCancelAnytime.show()
                    binding.textBackupSubPpTos.show()
                    updatePpTosText(binding.textBackupSubPpTos)
                }
            }
        }
        binding.btnBackupSubManage.setOnClickListener {
            startActivityOrNothing(
                IntentHelper.webPage(billingSupervisor.getManageSubscriptionLink()).newTask()
            )
        }
        billingSupervisor.backupSubEvent.observeEvent(viewLifecycleOwner) {
            binding.root.snackbar(RBase.string.thanks)
        }

        billingSupervisor.error.observeEvent(viewLifecycleOwner) {
            context.showErrorDialog(it)
        }
    }

    private fun updatePpTosText(textView: TextView) {
        val context = textView.context
        val contentTemplate = getString(RBase.string.billing_explanation)
        val privacyPolicy = getString(RBase.string.privacy_policy)
        val terms = getString(RBase.string.terms_of_service)
        val content = contentTemplate.format(privacyPolicy, terms)
        val spannable = SpannableString(content)

        val privacyPolicyIndex = content.indexOf(privacyPolicy)
        spannable.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    context.openLink(Constants.getPrivacyPolicyLink())
                }
            },
            privacyPolicyIndex,
            privacyPolicyIndex + privacyPolicy.length,
            Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )

        val termsIndex = content.indexOf(terms)
        spannable.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    context.openLink(Constants.getTermsOfServiceLink())
                }
            },
            termsIndex,
            termsIndex + terms.length,
            Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )

        textView.run {
            show()
            setText(spannable, TextView.BufferType.SPANNABLE)
            movementMethod = LinkMovementMethodCompat.getInstance()
        }
    }

    private fun setUpEntries() {
        childFragmentManager.beginTransaction()
            .replace(
                R.id.fragmentContainerOneTime,
                PreferenceStyleListFragment.newInstance(
                    buildList {
                        this += PreferenceStyleListFragment.Entry(
                            RBase.drawable.settings_theme,
                            RBase.string.billing_more_themes_title,
                            RBase.string.billing_more_themes_desp
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            this += PreferenceStyleListFragment.Entry(
                                RBase.drawable.settings_material_you,
                                RBase.string.billing_dynamic_themes_title,
                                RBase.string.billing_dynamic_themes_desp,
                            )
                        }
                        this += PreferenceStyleListFragment.Entry(
                            RBase.drawable.settings_count,
                            RBase.string.billing_baked_count_title,
                            RBase.string.billing_baked_count_desp
                        )
                        this += PreferenceStyleListFragment.Entry(
                            RBase.drawable.ic_ad_off,
                            RBase.string.billing_ad_off_title,
                            RBase.string.billing_ad_off_desp
                        )
                        this += PreferenceStyleListFragment.Entry(
                            RBase.drawable.settings_code,
                            RBase.string.billing_future_title,
                            RBase.string.billing_future_desp
                        )
                    }

                )
            )
            .commit()
        childFragmentManager.beginTransaction()
            .replace(
                R.id.fragmentContainerCloudBackup,
                PreferenceStyleListFragment.newInstance(
                    listOf(
                        PreferenceStyleListFragment.Entry(
                            RBase.drawable.settings_cloud_backup,
                            RBase.string.billing_cloud_backup_title,
                            RBase.string.billing_cloud_backup_desp
                        ),
                    )
                )
            )
            .commit()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.flavor_help, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.action_flavor_help -> {
                val context = requireContext()
                MaterialAlertDialogBuilder(context)
                    .setItems(
                        arrayOf(
                            getString(RBase.string.billing_help_contact),
                        )
                    ) { _, which ->
                        when (which) {
                            0 -> {
                                startActivityOrNothing(
                                    IntentHelper.email(
                                        email = flavorData.email,
                                        subject = getString(RBase.string.billing_help_email_title)
                                    ).createChooserIntentIfDead(context)
                                )
                            }
                        }
                    }
                    .show()
            }
            else -> return false
        }
        return true
    }
}
