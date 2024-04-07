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
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.deweyreed.tools.helper.requireCallback
import dagger.hilt.android.AndroidEntryPoint
import xyz.aprildown.timer.app.base.data.PreferenceData.lastBackupUri
import xyz.aprildown.timer.app.base.ui.MainCallback
import xyz.aprildown.timer.domain.utils.AppTracker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import xyz.aprildown.timer.app.base.R as RBase

@AndroidEntryPoint
class Export2Fragment : Fragment() {

    private val viewModel: Export2ViewModel by viewModels()

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
            Export2(
                screen = viewModel.screen.collectAsState().value,
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
        if (resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            if (uri.toString().isBlank() || uri == Uri.EMPTY) return
            val context = requireContext()
            context.lastBackupUri = uri
            viewModel.setSaveLocation(uri.toString())
        }
    }
}

@Composable
private fun Export2(
    screen: Export2ViewModel.Screen,
    onLocationChange: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MaterialTheme {
        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Header(text = stringResource(id = RBase.string.export_path_title))

            SaveLocation(
                location = screen.saveLocation,
                modifier = Modifier.clickable(onClick = onLocationChange),
            )

            Header(text = stringResource(id = RBase.string.backup_app_data))

            DataEntry(
                iconRes = RBase.drawable.ic_timer,
                nameRes = RBase.string.main_action_timers,
                include = screen.includeTimers,
                onIncludedChange = screen.onTimersChange,
            )
            DataEntry(
                iconRes = RBase.drawable.ic_scheduler,
                nameRes = RBase.string.main_action_schedulers,
                include = screen.includeSchedulers,
                onIncludedChange = screen.onSchedulersChange,
            )
            DataEntry(
                iconRes = RBase.drawable.ic_stat,
                nameRes = RBase.string.main_action_record,
                include = screen.includeRecords,
                onIncludedChange = screen.onRecordsChange,
            )
            DataEntry(
                iconRes = RBase.drawable.ic_settings,
                nameRes = RBase.string.main_action_settings,
                include = screen.includeSettings,
                onIncludedChange = screen.onSettingsChange,
            )

            Button(
                onClick = screen.onExport,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                enabled = !screen.saveLocation.isNullOrBlank() &&
                    (screen.includeTimers || screen.includeSettings) &&
                    !screen.exporting,
            ) {
                Text(text = stringResource(id = RBase.string.export_action))
            }
        }

        if (screen.exportErrorMessage != null) {
            ExportError(
                message = screen.exportErrorMessage,
                consume = screen.consumeExportError,
            )
        }

        if (screen.exporting) {
            ExportingDialog()
        }
    }
}

@Composable
private fun Header(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.padding(horizontal = 12.dp, vertical = 12.dp),
        style = MaterialTheme.typography.titleSmall,
    )
}

@Composable
private fun SaveLocation(
    location: String?,
    modifier: Modifier = Modifier,
) {
    if (location.isNullOrBlank()) {
        SaveLocationEmpty(modifier = modifier)
    } else {
        SaveLocationPath(location = location, modifier = modifier)
    }
}

@Composable
private fun SaveLocationPath(location: String, modifier: Modifier) {
    ListItem(
        headlineContent = {
            Text(text = location)
        },
        modifier = modifier,
        overlineContent = {
            Text(text = stringResource(id = RBase.string.export_select_location))
        },
        leadingContent = {
            Icon(imageVector = Icons.Rounded.Description, contentDescription = null)
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
                contentDescription = null,
            )
        },
    )
}

@Composable
private fun SaveLocationEmpty(modifier: Modifier) {
    ListItem(
        headlineContent = {
            Text(text = stringResource(id = RBase.string.export_select_location))
        },
        modifier = modifier,
        leadingContent = {
            Icon(imageVector = Icons.Rounded.Description, contentDescription = null)
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
                contentDescription = null,
            )
        },
    )
}

@Composable
private fun DataEntry(
    @DrawableRes iconRes: Int,
    @StringRes nameRes: Int,
    include: Boolean,
    onIncludedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = {
            Text(text = stringResource(id = nameRes))
        },
        modifier = modifier.clickable { onIncludedChange(!include) },
        leadingContent = {
            Icon(painter = painterResource(id = iconRes), contentDescription = null)
        },
        trailingContent = {
            Switch(checked = include, onCheckedChange = onIncludedChange)
        },
    )
}

@Composable
private fun ExportError(
    message: String,
    consume: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = consume,
        confirmButton = {
            TextButton(onClick = consume) {
                Text(text = stringResource(id = RBase.string.ok))
            }
        },
        text = {
            val context = LocalContext.current
            Text(
                text = buildString {
                    append(context.getString(RBase.string.export_error))

                    if (message.isNotBlank()) {
                        append("\n\n")
                        append(message)
                    }
                },
            )
        }
    )
}

@Composable
private fun ExportingDialog() {
    Dialog(onDismissRequest = {}) {
        Surface(shape = MaterialTheme.shapes.medium) {
            CircularProgressIndicator(modifier = Modifier.padding(24.dp))
        }
    }
}
