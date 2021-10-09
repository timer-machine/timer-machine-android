package xyz.aprildown.timer.flavor.google

import android.graphics.Typeface
import android.text.Spanned
import android.text.style.StyleSpan
import androidx.core.text.buildSpannedString
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.findNavController
import dagger.Reusable
import xyz.aprildown.timer.app.base.ui.FlavorUiInjector
import xyz.aprildown.timer.app.base.utils.NavigationUtils.subLevelNavigate
import xyz.aprildown.timer.flavor.google.count.BakedCountDialog
import xyz.aprildown.timer.flavor.google.utils.IapPromotionDialog
import xyz.aprildown.tools.anko.toast
import xyz.aprildown.tools.arch.observeEvent
import javax.inject.Inject
import xyz.aprildown.timer.app.base.R as RBase

@Reusable
class FlavorUiInjectorImpl @Inject constructor() : FlavorUiInjector {

    override fun showInAppPurchases(activity: FragmentActivity) {
        activity.startActivity(BillingActivity.getIntent(activity))
    }

    override val cloudBackupNavGraphId: Int = R.navigation.backup_graph

    override fun toCloudBackupFragment(currentFragment: Fragment) {
        currentFragment.findNavController().subLevelNavigate(RBase.id.dest_cloud_backup)
    }

    override fun toBakedCountDialog(fragment: Fragment) {
        BakedCountDialog().show(fragment)
    }

    private var billingSupervisor: BillingSupervisor? = null

    private fun ensureBillingSupervisor(fragment: Fragment) {
        if (billingSupervisor == null) {
            billingSupervisor = BillingSupervisor(
                fragment.requireContext(),
                requestProState = true,
            ).apply {
                withLifecycleOwner(fragment)
                supervise()
            }
            fragment.lifecycle.addObserver(
                object : DefaultLifecycleObserver {
                    override fun onDestroy(owner: LifecycleOwner) {
                        billingSupervisor = null
                    }
                }
            )
        }
    }

    override fun useMoreTheme(fragment: Fragment, onApply: () -> Unit) {
        ensureBillingSupervisor(fragment)

        fun clearObservers() {
            billingSupervisor?.proState?.removeObservers(fragment)
            billingSupervisor?.error?.removeObservers(fragment)
        }

        clearObservers()
        billingSupervisor?.proState?.observe(fragment) {
            clearObservers()
            if (it) {
                onApply.invoke()
            } else {
                val context = fragment.requireContext()
                IapPromotionDialog(context).show(
                    title = context.getString(RBase.string.billing_more_themes_title),
                    message = buildSpannedString {
                        append(context.getString(RBase.string.billing_more_themes_desp))
                        append("\n\n")
                        append(
                            context.getString(RBase.string.billing_a_part_of_iap),
                            StyleSpan(Typeface.BOLD),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    },
                    positiveButtonTextRes = RBase.string.billing_purchase
                ) {
                    context.startActivity(BillingActivity.getIntent(context))
                }
            }
        }
        billingSupervisor?.error?.observeEvent(fragment) {
            clearObservers()
            fragment.toast(
                when (it) {
                    is BillingSupervisor.Error.Message -> it.content
                    else -> it.toString()
                }
            )
        }
    }
}
