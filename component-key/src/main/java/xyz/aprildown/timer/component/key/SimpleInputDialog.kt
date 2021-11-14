package xyz.aprildown.timer.component.key

import android.content.Context
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.updateLayoutParams
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import xyz.aprildown.tools.anko.dip
import xyz.aprildown.tools.helper.gone
import xyz.aprildown.tools.helper.onImeActionClick
import xyz.aprildown.tools.helper.setTextAndSelectEnd
import xyz.aprildown.timer.app.base.R as RBase

class SimpleInputDialog(
    private val context: Context
) {
    fun show(
        title: String? = null,
        @StringRes titleRes: Int = 0,

        preFill: String? = null,
        inputType: Int = InputType.TYPE_CLASS_TEXT,
        hint: String? = null,

        message: String? = null,
        @StringRes messageRes: Int = 0,

        onInput: (String) -> Unit
    ) {
        val builder = MaterialAlertDialogBuilder(context)
            .setPositiveButton(RBase.string.ok, null)
            .setNegativeButton(RBase.string.cancel, null)

        var topSpace = 0
        when {
            title != null -> {
                builder.setTitle(title)
            }
            titleRes != 0 -> {
                builder.setTitle(titleRes)
            }
            else -> {
                topSpace = context.dip(16)
            }
        }

        val view = View.inflate(context, R.layout.dialog_simple_input, null) as ViewGroup
        val edit = view.getChildAt(0) as EditText
        val textView = view.getChildAt(1) as TextView
        edit.requestFocus()

        builder.setView(view)

        val dialog = builder.create()
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        dialog.show()
        dialog.setOnDismissListener {
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        }

        val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        edit.onImeActionClick(EditorInfo.IME_ACTION_DONE) {
            positiveButton.performClick()
        }

        if (preFill != null && preFill.isNotBlank()) {
            edit.setTextAndSelectEnd(preFill)
        }
        if (hint != null) {
            edit.hint = hint
        }
        edit.inputType = edit.inputType or inputType
        if (topSpace > 0) {
            edit.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = topSpace
            }
        }

        if (message == null && messageRes == 0) {
            textView.gone()
        } else {
            textView.text = message ?: context.getString(messageRes)
        }

        positiveButton.setOnClickListener {
            onInput.invoke(edit.text.toString())
            dialog.dismiss()
        }
    }
}
