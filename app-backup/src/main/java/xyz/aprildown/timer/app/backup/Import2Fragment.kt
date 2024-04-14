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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.deweyreed.tools.helper.requireCallback
import dagger.hilt.android.AndroidEntryPoint
import xyz.aprildown.timer.app.base.data.PreferenceData.lastBackupUri
import xyz.aprildown.timer.app.base.ui.MainCallback
import xyz.aprildown.timer.domain.utils.AppTracker
import javax.inject.Inject
import xyz.aprildown.timer.app.base.R as RBase

@AndroidEntryPoint
class Import2Fragment : Fragment() {

    private val viewModel: Import2ViewModel by viewModels()

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
        (view as ComposeView).setContent {
            Import(
                screen = viewModel.screen.collectAsState().value,
                importScreen = viewModel.importScreen.collectAsState().value,
                onLocationChange = ::onLocationChange,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    private fun onLocationChange() {
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
        if (resultCode != Activity.RESULT_OK) return
        val uri = data?.data ?: return
        if (uri.toString().isBlank() || uri == Uri.EMPTY) return
        val context = requireContext()
        context.lastBackupUri = uri
        viewModel.changeContentLocation(
            location = uri.toString(),
            name = DocumentFile.fromSingleUri(context, uri)?.name,
        )
    }
}

@Composable
private fun Import(
    screen: BaseBackupViewModel.Screen,
    importScreen: Import2ViewModel.ImportScreen,
    onLocationChange: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Backup(
        screen = screen,
        contentLocationTitle = stringResource(id = RBase.string.import_path_title),
        contentLocationButtonText = stringResource(id = RBase.string.import_select_location),
        onChangeContentLocation = onLocationChange,
        backupButtonText = stringResource(id = RBase.string.import_action),
        backupErrorHint = stringResource(id = RBase.string.import_error),
        modifier = modifier,
        extraOptions = {
            WipeContent(
                wipe = importScreen.wipe,
                onWipeChanged = importScreen.onWipeChanged,
                modifier = Modifier.fillMaxSize(),
            )
        },
    )
}

@Composable
private fun WipeContent(
    wipe: Boolean,
    onWipeChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = {
            Text(text = stringResource(id = RBase.string.import_wipe_first))
        },
        modifier = modifier.clickable { onWipeChanged(!wipe) },
        leadingContent = {
            Icon(
                painter = painterResource(id = RBase.drawable.ic_delete),
                contentDescription = null,
            )
        },
        trailingContent = {
            Switch(checked = wipe, onCheckedChange = onWipeChanged)
        },
    )
}
