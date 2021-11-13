package xyz.aprildown.timer.app.settings.theme

import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.color.ColorChooserDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import xyz.aprildown.timer.app.settings.R
import xyz.aprildown.timer.component.key.ListItemWithLayout
import xyz.aprildown.tools.helper.requireCallback
import xyz.aprildown.timer.app.base.R as RBase

internal class CustomThemeDialog : DialogFragment(), ColorChooserDialog.ColorCallback {

    interface Callback {
        fun onCustomThemePick(@ColorInt primary: Int, @ColorInt secondary: Int)
    }

    private lateinit var callback: Callback

    @ColorInt
    private var colorPrimary: Int = 0

    @ColorInt
    private var colorSecondary: Int = 0

    private lateinit var imagePrimary: ImageView
    private lateinit var imageSecondary: ImageView

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = requireCallback()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()

        requireArguments().run {
            colorPrimary = getInt(ARG_PRIMARY)
            colorSecondary = getInt(ARG_SECONDARY)
        }

        val view = View.inflate(context, R.layout.dialog_custom_theme, null)

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(RBase.string.theme_custom_title)
            .setView(view)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                callback.onCustomThemePick(colorPrimary, colorSecondary)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        fun showChooser(@StringRes title: Int, tag: String, tintButtons: Boolean) {
            ColorChooserDialog.Builder(context, title)
                .tag(tag)
                .doneButton(android.R.string.ok)
                .cancelButton(android.R.string.cancel)
                .customButton(RBase.string.theme_pick_colors)
                .presetsButton(RBase.string.theme_pick_presets)
                .backButton(RBase.string.theme_pick_back)
                .allowUserColorInputAlpha(false)
                .dynamicButtonColor(tintButtons)
                .show(childFragmentManager)
        }

        val itemPrimary = view.findViewById<ListItemWithLayout>(R.id.itemCustomThemePrimary)
        imagePrimary = itemPrimary.getLayoutView()
        imagePrimary.setBackgroundColor(colorPrimary)
        itemPrimary.setOnClickListener {
            showChooser(RBase.string.theme_custom_primary, "p", tintButtons = false)
        }

        val itemSecondary = view.findViewById<ListItemWithLayout>(R.id.itemCustomThemeSecondary)
        imageSecondary = itemSecondary.getLayoutView()
        imageSecondary.setBackgroundColor(colorSecondary)
        itemSecondary.setOnClickListener {
            showChooser(RBase.string.theme_custom_secondary, "a", tintButtons = true)
        }

        return dialog
    }

    override fun onColorChooserDismissed(dialog: ColorChooserDialog) = Unit
    override fun onColorSelection(dialog: ColorChooserDialog, selectedColor: Int) {
        val colorFrom: Int
        val targetImageView: ImageView
        if (dialog.tag() == "p") {
            colorFrom = colorPrimary
            colorPrimary = selectedColor
            targetImageView = imagePrimary
        } else {
            colorFrom = colorSecondary
            colorSecondary = selectedColor
            targetImageView = imageSecondary
        }
        targetImageView.post {
            ValueAnimator.ofArgb(colorFrom, selectedColor)
                .apply {
                    addUpdateListener {
                        targetImageView.setBackgroundColor(it.animatedValue as Int)
                    }
                    startDelay = 100
                }
                .setDuration(300)
                .start()
        }
    }

    companion object {
        private const val ARG_PRIMARY = "primary"
        private const val ARG_SECONDARY = "secondary"

        fun newInstance(@ColorInt primary: Int, @ColorInt secondary: Int): CustomThemeDialog =
            CustomThemeDialog().apply {
                arguments = bundleOf(
                    ARG_PRIMARY to primary,
                    ARG_SECONDARY to secondary
                )
            }
    }
}
