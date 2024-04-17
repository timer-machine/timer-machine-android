package xyz.aprildown.timer.app.backup

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import xyz.aprildown.timer.domain.usecases.Fruit
import xyz.aprildown.timer.app.base.R as RBase

@Composable
internal fun Backup(
    screen: BaseBackupViewModel.Screen<*>,
    contentLocationTitle: String,
    contentLocationButtonText: String,
    onChangeContentLocation: () -> Unit,
    backupButtonText: String,
    backupErrorHint: String,
    modifier: Modifier = Modifier,
    extraOptions: @Composable () -> Unit = {},
) {
    MaterialTheme {
        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Header(text = contentLocationTitle)

            ContentLocation(
                title = contentLocationButtonText,
                location = screen.contentName,
                modifier = Modifier.clickable(onClick = onChangeContentLocation),
            )

            Header(text = stringResource(id = RBase.string.backup_app_data))

            extraOptions()

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
                onClick = screen.onBackup,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                enabled = screen.content != null &&
                    (screen.includeTimers || screen.includeSettings) &&
                    !screen.backupOngoing,
            ) {
                Text(text = backupButtonText)
            }
        }

        if (screen.backupResult?.fruit is Fruit.Rotten) {
            BackupError(
                hint = backupErrorHint,
                message = (screen.backupResult.fruit as Fruit.Rotten).exception.message.toString(),
                consume = screen.consumeBackupError,
            )
        }

        if (screen.backupOngoing) {
            BackupOngoingDialog()
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
private fun ContentLocation(
    title: String,
    location: String?,
    modifier: Modifier = Modifier,
) {
    if (location.isNullOrBlank()) {
        ContentLocationEmpty(title = title, modifier = modifier)
    } else {
        ContentLocationPath(title = title, location = location, modifier = modifier)
    }
}

@Composable
private fun ContentLocationPath(
    title: String,
    location: String,
    modifier: Modifier
) {
    ListItem(
        headlineContent = {
            Text(text = location)
        },
        modifier = modifier,
        overlineContent = {
            Text(text = title)
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
private fun ContentLocationEmpty(
    title: String,
    modifier: Modifier
) {
    ListItem(
        headlineContent = {
            Text(text = title)
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
        modifier = modifier
            .toggleable(
                value = include,
                role = Role.Switch,
                onValueChange = onIncludedChange
            ),
        leadingContent = {
            Icon(painter = painterResource(id = iconRes), contentDescription = null)
        },
        trailingContent = {
            Switch(checked = include, onCheckedChange = null)
        },
    )
}

@Composable
private fun BackupError(
    hint: String,
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
            Text(
                text = buildString {
                    append(hint)

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
private fun BackupOngoingDialog() {
    Dialog(onDismissRequest = {}) {
        Surface(shape = MaterialTheme.shapes.medium) {
            CircularProgressIndicator(modifier = Modifier.padding(24.dp))
        }
    }
}
