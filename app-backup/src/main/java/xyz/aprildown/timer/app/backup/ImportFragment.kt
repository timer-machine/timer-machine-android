package xyz.aprildown.timer.app.backup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.core.text.buildSpannedString
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.NavHostFragment
import com.github.deweyreed.tools.anko.longSnackbar
import com.github.deweyreed.tools.arch.observeEvent
import com.github.deweyreed.tools.helper.gone
import com.github.deweyreed.tools.helper.requireCallback
import com.github.deweyreed.tools.helper.restartWithFading
import com.github.deweyreed.tools.helper.show
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import ernestoyaquello.com.verticalstepperform.VerticalStepperFormView
import ernestoyaquello.com.verticalstepperform.listener.StepperFormListener
import okio.buffer
import okio.source
import xyz.aprildown.timer.app.base.data.PreferenceData.lastBackupUri
import xyz.aprildown.timer.app.base.ui.AppNavigator
import xyz.aprildown.timer.app.base.ui.MainCallback
import xyz.aprildown.timer.app.base.ui.newDynamicTheme
import xyz.aprildown.timer.app.base.utils.AppPreferenceProvider
import xyz.aprildown.timer.domain.usecases.data.ImportAppData
import xyz.aprildown.timer.domain.utils.AppTracker
import xyz.aprildown.timer.presentation.backup.ImportViewModel
import javax.inject.Inject
import xyz.aprildown.timer.app.base.R as RBase

@AndroidEntryPoint
class ImportFragment : Fragment(R.layout.layout_vertical_form), StepperFormListener {

    private val viewModel: ImportViewModel by viewModels()

    @Inject
    lateinit var appNavigator: AppNavigator

    @Inject
    lateinit var appTracker: AppTracker

    @Inject
    lateinit var appPreferenceProvider: AppPreferenceProvider

    private lateinit var mainCallback: MainCallback.ActivityCallback

    private lateinit var locationStep: ImportLocationStep
    private lateinit var contentStep: ImportContentStep

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            handleActivityResult(resultCode = it.resultCode, data = it.data)
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainCallback = requireCallback()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = view.context
        val form = view as VerticalStepperFormView
        locationStep = ImportLocationStep(
            resources.getString(RBase.string.import_path_title),
            this
        )
        contentStep =
            ImportContentStep(resources.getString(RBase.string.import_content_title))
        form.setup(this, locationStep, contentStep)
            .displayBottomNavigation(false)
            .apply {
                newDynamicTheme.run {
                    basicColorScheme(colorPrimary, colorPrimaryVariant, colorOnPrimary)
                    nextButtonColors(
                        colorSecondary,
                        colorSecondary,
                        colorOnSecondary,
                        colorOnSecondary
                    )
                }
            }
            .stepNextButtonText(getString(RBase.string.backup_next_step))
            .confirmationStepTitle(resources.getString(RBase.string.import_begin))
            .lastStepNextButtonText(getString(RBase.string.import_action))
            .displayCancelButtonInLastStep(true)
            .lastStepCancelButtonText(getString(android.R.string.cancel))
            .init()
        viewModel.error.observeEvent(viewLifecycleOwner) { exception ->
            MaterialAlertDialogBuilder(context)
                .setMessage(
                    buildSpannedString {
                        append(getText(RBase.string.import_error))

                        (exception.localizedMessage ?: exception.message)
                            ?.takeIf { it.isNotBlank() }
                            ?.let { message ->
                                append("\n\n")
                                append(message)
                            }
                    }
                )
                .setPositiveButton(android.R.string.ok, null)
                .setOnDismissListener {
                    popBackToBackup()
                }
                .show()
        }
    }

    override fun onCompletedForm() {
        val context = requireContext()
        val settings = contentStep.stepData

        val fileUri = locationStep.stepData

        var data = ""
        try {
            context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                data = inputStream.source().buffer().readUtf8()
            }
        } catch (_: Exception) {
        }

        viewModel.importAppData(
            ImportAppData.Params(
                data = data,
                wipeFirst = settings.wipeFirst,
                importTimers = settings.isTimersChecked,
                importTimerStamps = settings.isTimerStampsChecked,
                importSchedulers = settings.isSchedulersChecked
            ),
            handlePrefs = { map ->
                if (settings.isSettingsChecked) {
                    appPreferenceProvider.applyAppPreferences(map)
                }
            },
            onSuccess = {
                MaterialAlertDialogBuilder(context)
                    .setCancelable(false)
                    .setTitle(RBase.string.import_done)
                    .setMessage(RBase.string.import_restart_content)
                    .setPositiveButton(RBase.string.import_restart) { _, _ ->
                        val activity = requireActivity()
                        activity.restartWithFading(appNavigator.getMainIntent())
                    }
                    .show()
            }
        )
    }

    override fun onCancelledForm() {
        popBackToBackup()
    }

    private fun popBackToBackup() {
        NavHostFragment.findNavController(this).popBackStack(RBase.id.dest_backup_restore, false)
    }

    fun importFile() {
        SafIntentSafeBelt(
            context = requireContext(),
            appTracker = appTracker,
            viewForSnackbar = mainCallback.snackbarView
        ).drive(
            launcher = launcher,
            intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("*/*")
        )
    }

    private fun handleActivityResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            val context = requireContext()
            var name: String? = null
            var isFileEmpty = false

            try {
                context.contentResolver.query(
                    uri,
                    arrayOf(
                        OpenableColumns.DISPLAY_NAME,
                        OpenableColumns.SIZE
                    ),
                    null,
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        name =
                            cursor.getStringOrNull(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                        isFileEmpty =
                            cursor.getLongOrNull(cursor.getColumnIndex(OpenableColumns.SIZE)) == 0L
                    }
                }
            } catch (e: Exception) {
                appTracker.trackError(e)
                mainCallback.snackbarView.longSnackbar(e.message.toString())
                return
            }

            context.lastBackupUri = uri
            if (isFileEmpty) {
                locationStep.emptyFilePicked()
            } else {
                locationStep.locationPicked(uri, name)
            }
        }
    }
}

private class ImportLocationStep(
    title: String,
    private val parentFragment: ImportFragment
) : AbstractStep<Uri>(title) {

    private var locationUri: Uri = Uri.EMPTY
    private lateinit var locationTextView: TextView

    override fun createStepContentLayout(): View {
        val context = context
        val view = View.inflate(context, R.layout.step_text_button, null)
        locationTextView = view.findViewById<TextView>(R.id.textStepTextButton).apply {
            gone()
        }
        view.findViewById<Button>(R.id.btnStepTextButton).run {
            setText(RBase.string.import_select_location)
            setOnClickListener {
                parentFragment.importFile()
            }
        }
        return view
    }

    fun locationPicked(uri: Uri, content: String? = null) {
        locationUri = uri
        locationTextView.show()
        locationTextView.text = content
        markAsCompletedOrUncompleted(true)
    }

    fun emptyFilePicked() {
        markAsUncompleted(parentFragment.getString(RBase.string.import_empty_file), true)
    }

    override fun isStepDataValid(stepData: Uri): IsDataValid {
        return IsDataValid(stepData != Uri.EMPTY, context.getString(RBase.string.import_no_file))
    }

    override fun getStepDataAsHumanReadableString(): String = locationTextView.text.toString()
    override fun getStepData(): Uri = locationUri
}

private class ImportContentStep(
    title: String
) : AbstractStep<SelectAppContentSettings>(title) {

    private lateinit var helper: SelectAppContentHelper

    override fun createStepContentLayout(): View {
        val context = context
        val view = View.inflate(context, R.layout.step_select_content, null)
        helper = SelectAppContentHelper().apply {
            setUpView(view)
        }
        return view
    }

    override fun isStepDataValid(stepData: SelectAppContentSettings?): IsDataValid =
        IsDataValid(true)

    override fun getStepData(): SelectAppContentSettings {
        return helper.settings
    }
}
