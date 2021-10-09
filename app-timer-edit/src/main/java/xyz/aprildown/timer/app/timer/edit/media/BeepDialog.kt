package xyz.aprildown.timer.app.timer.edit.media

import android.content.Context
import android.text.InputType
import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import xyz.aprildown.timer.app.base.media.Beeper
import xyz.aprildown.tools.view.SimpleInputDialog
import xyz.aprildown.timer.app.base.R as RBase

internal class BeepDialog(private val context: Context) {

    @Suppress("SpellCheckingInspection")
    private val items: ArrayMap<Int, String>
        get() = arrayMapOf<Int, String>().apply {
            put(0, "DTMF 0")
            put(1, "DTMF 1")
            put(2, "DTMF 2")
            put(3, "DTMF 3")
            put(4, "DTMF 4")
            put(5, "DTMF 5")
            put(6, "DTMF 6")
            put(7, "DTMF 7")
            put(8, "DTMF 8")
            put(9, "DTMF 9")
            put(10, "DTMF S")
            put(11, "DTMF P")
            put(12, "DTMF A")
            put(13, "DTMF B")
            put(14, "DTMF C")
            put(15, "DTMF D")
        }

    fun showBeepPicker(
        select: Int,
        audioFocusType: Int,
        streamType: Int,
        onPicked: (Int) -> Unit
    ) {
        Beeper.load(
            Beeper.Settings(
                audioFocusType = audioFocusType,
                streamType = streamType
            ),
            debounce = false
        )
        var selected = 0
        MaterialAlertDialogBuilder(context)
            .setTitle(RBase.string.beep_sound)
            .setSingleChoiceItems(items.values.toTypedArray(), select) { _, which ->
                Beeper.play(context, items.keyAt(which))
                selected = which
            }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                onPicked.invoke(selected)
            }
            .setOnDismissListener {
                Beeper.tearDown()
            }
            .show()
    }

    fun showBeepCountDialog(oldCount: Int, func: (Int) -> Unit) {
        SimpleInputDialog(context).show(
            titleRes = RBase.string.beep_count_title,
            preFill = oldCount.toString(),
            inputType = InputType.TYPE_CLASS_NUMBER,
            messageRes = RBase.string.beep_count_intro
        ) {
            func.invoke(it.toIntOrNull() ?: 0)
        }
    }
}
