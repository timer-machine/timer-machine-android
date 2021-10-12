package xyz.aprildown.timer.app.backup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.database.getStringOrNull
import androidx.core.text.buildSpannedString
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import ernestoyaquello.com.verticalstepperform.VerticalStepperFormView
import ernestoyaquello.com.verticalstepperform.listener.StepperFormListener
import okio.buffer
import okio.sink
import xyz.aprildown.timer.app.base.data.PreferenceData.lastBackupUri
import xyz.aprildown.timer.app.base.ui.MainCallback
import xyz.aprildown.timer.app.base.ui.newDynamicTheme
import xyz.aprildown.timer.app.base.utils.AppPreferenceProvider
import xyz.aprildown.timer.domain.usecases.data.ExportAppData
import xyz.aprildown.timer.domain.utils.AppTracker
import xyz.aprildown.timer.presentation.backup.ExportViewModel
import xyz.aprildown.tools.anko.longSnackbar
import xyz.aprildown.tools.arch.observeEvent
import xyz.aprildown.tools.helper.gone
import xyz.aprildown.tools.helper.requireCallback
import xyz.aprildown.tools.helper.show
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import xyz.aprildown.timer.app.base.R as RBase

@AndroidEntryPoint
class ExportFragment : Fragment(R.layout.layout_vertical_form), StepperFormListener {

    private val viewModel: ExportViewModel by viewModels()

    @Inject
    lateinit var appTracker: AppTracker

    @Inject
    lateinit var appPreferenceProvider: AppPreferenceProvider

    private lateinit var mainCallback: MainCallback.ActivityCallback

    private lateinit var locationStep: ExportLocationStep
    private lateinit var contentStep: ExportContentStep

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainCallback = requireCallback()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = view.context
        val form = view as VerticalStepperFormView
        locationStep = ExportLocationStep(resources.getString(RBase.string.export_path_title), this)
        contentStep = ExportContentStep(resources.getString(RBase.string.export_content_title))
        form.setup(this, locationStep, contentStep)
            .displayBottomNavigation(false)
            .apply {
                newDynamicTheme.run {
                    basicColorScheme(colorPrimary, colorPrimaryVariant, colorOnPrimary)
                    nextButtonColors(colorSecondary, colorSecondary, Color.WHITE, Color.WHITE)
                }
            }
            .stepNextButtonText(getString(RBase.string.backup_next_step))
            .confirmationStepTitle(resources.getString(RBase.string.export_begin))
            .lastStepNextButtonText(getString(RBase.string.export_action))
            .displayCancelButtonInLastStep(true)
            .lastStepCancelButtonText(getString(android.R.string.cancel))
            .init()
        viewModel.error.observeEvent(viewLifecycleOwner) { exception ->
            MaterialAlertDialogBuilder(context)
                .setMessage(
                    buildSpannedString {
                        append(getText(RBase.string.export_error))

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
        viewModel.exportAppData(
            ExportAppData.Params(
                exportTimers = settings.isTimersChecked,
                exportTimerStamps = settings.isTimerStampsChecked,
                exportSchedulers = settings.isSchedulersChecked,
                prefs = if (settings.isSettingsChecked) {
                    appPreferenceProvider.getAppPreferences()
                } else {
                    emptyMap()
                }
            ),
            onExport = { exportString ->
                val fileUri = locationStep.stepData
                context.contentResolver.openOutputStream(fileUri, "rwt")?.use { os ->
                    os.sink().buffer().writeUtf8(exportString).flush()
                }
            },
            onSuccess = {
                mainCallback.snackbarView.longSnackbar(RBase.string.export_done)
                popBackToBackup()
            }
        )
    }

    override fun onCancelledForm() {
        popBackToBackup()
    }

    private fun popBackToBackup() {
        NavHostFragment.findNavController(this).popBackStack(RBase.id.dest_backup_restore, false)
    }

    fun pickExportPath() {
        val date = Date()
        val timeString = SimpleDateFormat("yyyy-MM-dd-kk-mm", Locale.getDefault()).format(date)
        val initialFilename = "timer-machine-$timeString.json"
        SafIntentSafeBelt(
            fragment = this,
            appTracker = appTracker,
            viewForSnackbar = mainCallback.snackbarView
        ).drive(
            Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("*/*")
                .putExtra(Intent.EXTRA_TITLE, initialFilename)
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            val context = requireContext()
            var name: String? = null

            try {
                context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        name =
                            cursor.getStringOrNull(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                    }
                }
            } catch (e: Exception) {
                appTracker.trackError(e)
                mainCallback.snackbarView.longSnackbar(e.message.toString())
                return
            }

            context.lastBackupUri = uri
            locationStep.locationPicked(uri, name)
        }
    }
}

private class ExportLocationStep(
    title: String,
    private val parentFragment: ExportFragment
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
            setText(RBase.string.export_select_location)
            setOnClickListener {
                parentFragment.pickExportPath()
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

    override fun isStepDataValid(stepData: Uri?): IsDataValid {
        return IsDataValid(
            stepData != Uri.EMPTY,
            context.getString(RBase.string.export_select_location)
        )
    }

    override fun getStepDataAsHumanReadableString(): String = locationTextView.text.toString()
    override fun getStepData(): Uri = locationUri
}

private class ExportContentStep(
    title: String
) : AbstractStep<SelectAppContentSettings>(title) {

    private lateinit var helper: SelectAppContentHelper

    override fun createStepContentLayout(): View {
        val context = context
        val view = View.inflate(context, R.layout.step_select_content, null)
        helper = SelectAppContentHelper().apply {
            setUpView(view)
            removeWipeItem()
        }
        return view
    }

    override fun isStepDataValid(stepData: SelectAppContentSettings): IsDataValid {
        return IsDataValid(true)
    }

    override fun getStepDataAsHumanReadableString(): String = ""

    override fun getStepData(): SelectAppContentSettings {
        return helper.settings
    }
}
