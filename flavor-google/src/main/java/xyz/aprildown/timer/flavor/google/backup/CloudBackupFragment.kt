package xyz.aprildown.timer.flavor.google.backup

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.text.buildSpannedString
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.firebase.ui.auth.AuthUI
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import xyz.aprildown.timer.app.base.data.FlavorData
import xyz.aprildown.timer.app.base.utils.NavigationUtils.subLevelNavigate
import xyz.aprildown.timer.domain.usecases.Fruit
import xyz.aprildown.timer.domain.utils.AppConfig
import xyz.aprildown.timer.domain.utils.Constants
import xyz.aprildown.timer.flavor.google.BillingActivity
import xyz.aprildown.timer.flavor.google.R
import xyz.aprildown.timer.flavor.google.backup.usecases.AutoCloudBackup
import xyz.aprildown.timer.flavor.google.backup.usecases.CloudBackup
import xyz.aprildown.timer.flavor.google.backup.usecases.CloudBackupState
import xyz.aprildown.timer.flavor.google.backup.usecases.CurrentBackupState
import xyz.aprildown.timer.flavor.google.backup.usecases.CurrentBackupStateError
import xyz.aprildown.timer.flavor.google.showErrorDialog
import xyz.aprildown.timer.flavor.google.utils.IapPromotionDialog
import xyz.aprildown.timer.flavor.google.utils.causeFirstMessage
import xyz.aprildown.tools.anko.longSnackbar
import xyz.aprildown.tools.anko.newTask
import xyz.aprildown.tools.anko.snackbar
import xyz.aprildown.tools.arch.observeEvent
import xyz.aprildown.tools.helper.IntentHelper
import xyz.aprildown.tools.helper.createChooserIntentIfDead
import xyz.aprildown.tools.helper.safeSharedPreference
import xyz.aprildown.tools.helper.startActivityOrNothing
import javax.inject.Inject
import xyz.aprildown.timer.app.base.R as RBase

@AndroidEntryPoint
internal class CloudBackupFragment : PreferenceFragmentCompat() {

    private val viewModel by hiltNavGraphViewModels<CloudBackupViewModel>(RBase.id.dest_cloud_backup)

    @Inject
    lateinit var currentBackupState: CurrentBackupState

    @Inject
    lateinit var currentBackupStateError: CurrentBackupStateError

    @Inject
    lateinit var autoCloudBackup: AutoCloudBackup

    @Inject
    lateinit var flavorData: FlavorData

    private var manualBackupDialog: AlertDialog? = null

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        getSignInResultCallback()
    )

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_cloud_back, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // This super.onViewCreated can't be removed.
        super.onViewCreated(view, savedInstanceState)

        val context = view.context

        val backupState =
            findPreference<Preference>(getString(R.string.backup_key_backup_state))
        requireNotNull(backupState)

        val subscribeNow =
            findPreference<Preference>(getString(R.string.backup_key_subscribe))
        requireNotNull(subscribeNow)

        subscribeNow.setOnPreferenceClickListener {
            startActivity(BillingActivity.getIntent(context))
            true
        }

        val signIn = findPreference<Preference>(getString(R.string.backup_key_sign_in))
        requireNotNull(signIn)
        signIn.setOnPreferenceClickListener {
            launchSignInFlow()
            true
        }

        val backupNow =
            findPreference<Preference>(getString(R.string.backup_key_backup_now))
        requireNotNull(backupNow)

        findPreference<Preference>(getString(R.string.backup_key_restore))
            ?.setOnPreferenceClickListener {
                val currentUser = Firebase.auth.currentUser
                requireNotNull(currentUser)
                viewModel.requestUserFiles(
                    Firebase.storage.reference
                        .child(CloudBackup.BACKUP_FOLDER_NAME)
                        .child(currentUser.uid)
                )
                findNavController().subLevelNavigate(R.id.restoreFragment)
                true
            }

        val autoBackupKey = getString(R.string.backup_key_auto_cloud_backup)
        val autoBackup =
            findPreference<SwitchPreferenceCompat>(autoBackupKey)
        requireNotNull(autoBackup)

        findPreference<Preference>(getString(R.string.backup_key_sign_out))
            ?.setOnPreferenceClickListener {
                confirmToSignOut(context)
                true
            }

        findPreference<Preference>(getString(R.string.backup_key_delete))
            ?.setOnPreferenceClickListener {
                confirmToDeleteAccount(context)
                true
            }

        findPreference<Preference>(getString(R.string.backup_key_contact))
            ?.setOnPreferenceClickListener {
                startActivityOrNothing(
                    IntentHelper.email(
                        email = flavorData.email,
                        subject = getString(RBase.string.billing_help_email_title)
                    ).createChooserIntentIfDead(context)
                )
                true
            }

        findPreference<Preference>(getString(R.string.backup_key_manage_subscription))
            ?.setOnPreferenceClickListener {
                startActivityOrNothing(
                    IntentHelper.webPage(
                        viewModel.billingSupervisor.getManageSubscriptionLink()
                    ).newTask()
                )
                true
            }

        // Only used when subscribed.
        fun syncBackupState() {
            val state = currentBackupState.get()
            backupNow.isEnabled = state.canBackupNow
            if (state != CloudBackupState.Error) {
                backupState.setSummary(state.despId)
            } else {
                backupState.summary = currentBackupStateError.get()
            }
        }

        // Only used when subscribed.
        val backupStateObserver = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                CloudBackupState.PREF_CLOUD_BACKUP_STATE -> {
                    syncBackupState()
                }
                CloudBackupState.PREF_CLOUD_BACKUP_ERROR -> {
                    if (currentBackupState.get() === CloudBackupState.Error) {
                        backupState.summary = currentBackupStateError.get()
                    }
                }
                autoBackupKey -> {
                    autoBackup.isChecked = autoCloudBackup.get()
                }
            }
        }

        viewModel.billingSupervisor.backupSubState.observe(viewLifecycleOwner) { hasBackupSub ->
            if (hasBackupSub) {
                syncBackupState()
                val sharedPreferences = context.safeSharedPreference
                viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
                    override fun onCreate(owner: LifecycleOwner) {
                        sharedPreferences
                            .registerOnSharedPreferenceChangeListener(backupStateObserver)
                    }

                    override fun onDestroy(owner: LifecycleOwner) {
                        sharedPreferences
                            .unregisterOnSharedPreferenceChangeListener(backupStateObserver)
                    }
                })

                backupNow.setOnPreferenceClickListener {
                    manualBackupDialog = MaterialAlertDialogBuilder(context)
                        .setCancelable(false)
                        .setTitle(RBase.string.cloud_backup_uploading)
                        .setView(R.layout.dialog_loading)
                        .setNegativeButton(RBase.string.cancel) { _, _ ->
                            manualBackupDialog?.dismiss()

                            if (autoCloudBackup.get()) {
                                CloudBackup.schedule(context, currentBackupState)
                            } else {
                                currentBackupState.set(CloudBackupState.Required)
                            }

                            viewModel.cancelCloudBackup()
                        }
                        .setOnDismissListener {
                            manualBackupDialog = null
                        }
                        .show()

                    viewModel.manualCloudBackup()
                    true
                }
            } else {
                backupState.setSummary(RBase.string.cloud_backup_state_required)
            }

            onAccountPaymentChanged()

            if (!hasBackupSub && !viewModel.isPromotionShown) {
                viewModel.isPromotionShown = true
                showPromotion()
            }
        }

        onAccountPaymentChanged()

        viewModel.billingSupervisor.error.observeEvent(viewLifecycleOwner) {
            context.showErrorDialog(it)
        }

        viewModel.manualCloudBackupResult.observe(viewLifecycleOwner) { fruit ->
            when (fruit) {
                is Fruit.Ripe -> {
                    manualBackupDialog?.dismiss()

                    requireView().longSnackbar(RBase.string.cloud_backup_finished)

                    viewModel.consumeManualCloudBackupResult()
                }
                is Fruit.Rotten -> {
                    manualBackupDialog?.dismiss()

                    MaterialAlertDialogBuilder(context)
                        .setTitle(RBase.string.cloud_backup_backup_now_failed)
                        .setMessage(fruit.exception.causeFirstMessage())
                        .setPositiveButton(RBase.string.ok, null)
                        .show()
                    if (autoCloudBackup.get()) {
                        CloudBackup.schedule(context, currentBackupState)
                    }

                    viewModel.consumeManualCloudBackupResult()
                }
                else -> Unit
            }
        }

        viewModel.autoBackupEnabledEvent.observeEvent(viewLifecycleOwner) {
            autoBackup.isChecked = true
            requireView().snackbar(RBase.string.cloud_backup_auto_backup_enabled)
        }
    }

    private fun launchSignInFlow() {
        signInLauncher.launch(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setLogo(RBase.mipmap.app_icon_square)
                .setTosAndPrivacyPolicyUrls(
                    Constants.getTermsOfServiceLink(),
                    Constants.getPrivacyPolicyLink()
                )
                .setAvailableProviders(
                    mutableListOf<AuthUI.IdpConfig>().apply {
                        add(AuthUI.IdpConfig.GoogleBuilder().build())
                        if (AppConfig.openDebug) {
                            add(AuthUI.IdpConfig.EmailBuilder().build())
                        }
                    }
                )
                .build()
        )
    }

    private fun getSignInResultCallback(): ActivityResultCallback<ActivityResult> {
        return ActivityResultCallback { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                onAccountPaymentChanged()
                if (Firebase.auth.currentUser != null) {
                    viewModel.tryToSetUpForNewSubscriber()
                }
            }
        }
    }

    private fun onAccountPaymentChanged() {
        val isSignedIn = Firebase.auth.currentUser != null
        val hasSubscription = viewModel.billingSupervisor.backupSubState.value == true

        findPreference<Preference>(getString(R.string.backup_key_subscribe))?.isVisible =
            !hasSubscription
        findPreference<Preference>(getString(R.string.backup_key_sign_in))?.isVisible = !isSignedIn

        findPreference<Preference>(getString(R.string.backup_key_backup_now))?.isEnabled =
            isSignedIn && hasSubscription && currentBackupState.get().canBackupNow
        findPreference<Preference>(getString(R.string.backup_key_restore))?.isEnabled = isSignedIn
        findPreference<Preference>(getString(R.string.backup_key_auto_cloud_backup))
            ?.isEnabled = isSignedIn && hasSubscription
        findPreference<Preference>(getString(R.string.backup_key_sign_out))?.isEnabled = isSignedIn
        findPreference<Preference>(getString(R.string.backup_key_delete))?.isEnabled = isSignedIn
    }

    private fun confirmToSignOut(context: Context) {
        MaterialAlertDialogBuilder(context)
            .setMessage(RBase.string.account_sign_out_confirmation)
            .setNegativeButton(RBase.string.cancel, null)
            .setPositiveButton(RBase.string.ok) { _, _ ->
                val loadingDialog = MaterialAlertDialogBuilder(context)
                    .setCancelable(false)
                    .setTitle(RBase.string.account_signing_out)
                    .setView(R.layout.dialog_loading)
                    .show()
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        AuthUI.getInstance().signOut(context).await()
                        CloudBackup.cancel(context, currentBackupState)
                        onAccountPaymentChanged()
                    } catch (e: Exception) {
                        requireView().longSnackbar(
                            e.localizedMessage?.toString().toString()
                        )
                    } finally {
                        loadingDialog.dismiss()
                    }
                }
            }
            .show()
    }

    private fun confirmToDeleteAccount(context: Context) {
        val dialog = MaterialAlertDialogBuilder(context)
            .setMessage(RBase.string.account_delete_confirmation)
            .setPositiveButton(RBase.string.ok, null)
            .setNegativeButton(RBase.string.cancel, null)
            .show()
        val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        positiveButton.isEnabled = false

        val timer = object : CountDownTimer(3000, 1000) {
            private var remainingSeconds = 3
            override fun onTick(millisUntilFinished: Long) {
                positiveButton.text = getString(RBase.string.count_down_template).format(
                    getString(RBase.string.ok),
                    remainingSeconds
                )
                remainingSeconds -= 1
            }

            override fun onFinish() {
                positiveButton.setText(RBase.string.ok)
                positiveButton.isEnabled = true
            }
        }
        timer.start()

        positiveButton.setOnClickListener {
            dialog.dismiss()

            val loadingDialog = MaterialAlertDialogBuilder(context)
                .setCancelable(false)
                .setTitle(RBase.string.account_deleting)
                .setView(R.layout.dialog_loading)
                .show()

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    AuthUI.getInstance().delete(context).await()
                    CloudBackup.cancel(context, currentBackupState)
                    onAccountPaymentChanged()
                } catch (e: Exception) {
                    requireView().longSnackbar(e.localizedMessage?.toString().toString())
                } finally {
                    loadingDialog.dismiss()
                }
            }
        }

        dialog.setOnDismissListener {
            timer.cancel()
        }
    }

    private fun showPromotion() {
        if (viewModel.billingSupervisor.error.value != null) return
        val context = requireContext()
        IapPromotionDialog(context).show(
            title = context.getString(RBase.string.billing_cloud_backup_title),
            message = buildSpannedString {
                append(context.getString(RBase.string.billing_cloud_backup_desp))
                append("\n\n")
                append(
                    context.getString(RBase.string.billing_subscription_service),
                    StyleSpan(Typeface.BOLD),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            },
            positiveButtonTextRes = RBase.string.billing_subscribe
        ) {
            startActivity(BillingActivity.getIntent(context))
        }
    }
}
