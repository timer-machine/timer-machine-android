package xyz.aprildown.timer.app.backup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.github.deweyreed.tools.anko.longSnackbar
import com.github.deweyreed.tools.helper.requireCallback
import dagger.hilt.android.AndroidEntryPoint
import okio.sink
import xyz.aprildown.timer.app.base.data.PreferenceData.lastBackupUri
import xyz.aprildown.timer.app.base.ui.AppTheme
import xyz.aprildown.timer.app.base.ui.MainCallback
import xyz.aprildown.timer.domain.usecases.Fruit
import xyz.aprildown.timer.domain.utils.AppTracker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import xyz.aprildown.timer.app.base.R as RBase

@AndroidEntryPoint
class ExportFragment : Fragment() {

    private val viewModel: ExportViewModel by viewModels()

    private lateinit var mainCallback: MainCallback.ActivityCallback

    @Inject
    lateinit var appTracker: AppTracker

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            handleActivityResult(resultCode = it.resultCode, data = it.data)
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainCallback = requireCallback()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val navController = findNavController()
        (view as ComposeView).setContent {
            val screen by viewModel.screen.collectAsState()
            LaunchedEffect(screen.backupResult) {
                val result = screen.backupResult
                if (result?.fruit is Fruit.Ripe) {
                    mainCallback.snackbarView.longSnackbar(RBase.string.export_done)
                    navController.popBackStack(
                        destinationId = RBase.id.dest_backup_restore,
                        inclusive = false,
                    )
                }
            }
            Export(
                screen = screen,
                onLocationChange = ::onLocationChange,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    private fun onLocationChange() {
        val date = Date()
        val timeString = SimpleDateFormat("yyyy-MM-dd-kk-mm", Locale.getDefault()).format(date)
        val initialFilename = "timer-machine-$timeString.json"
        SafIntentSafeBelt(
            context = requireContext(),
            appTracker = appTracker,
            viewForSnackbar = mainCallback.snackbarView
        ).drive(
            launcher = launcher,
            intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("*/*")
                .putExtra(Intent.EXTRA_TITLE, initialFilename)
        )
    }

    private fun handleActivityResult(resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) return
        val uri = data?.data ?: return
        if (uri.toString().isBlank() || uri == Uri.EMPTY) return
        val context = requireContext()
        context.lastBackupUri = uri
        val contentResolver = context.contentResolver
        val documentFile = DocumentFile.fromSingleUri(context, uri)
        viewModel.changeContent(
            content = ExportViewModel.WritableContent(
                getSink = {
                    checkNotNull(contentResolver.openOutputStream(uri)).sink()
                },
                delete = {
                    // ContentResolver.delete doesn't work
                    documentFile?.delete()
                },
            ),
            name = documentFile?.name?.takeIf { it.isNotBlank() } ?: uri.toString(),
        )
    }
}

@Composable
private fun Export(
    screen: BaseBackupViewModel.Screen<*>,
    onLocationChange: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppTheme {
        Backup(
            screen = screen,
            contentLocationTitle = stringResource(id = RBase.string.export_path_title),
            contentLocationButtonText = stringResource(id = RBase.string.export_select_location),
            onChangeContentLocation = onLocationChange,
            backupButtonText = stringResource(id = RBase.string.export_action),
            backupErrorHint = stringResource(id = RBase.string.export_error),
            modifier = modifier,
        )
    }
}
