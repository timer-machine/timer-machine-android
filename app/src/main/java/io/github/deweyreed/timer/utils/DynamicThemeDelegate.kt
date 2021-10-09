package io.github.deweyreed.timer.utils

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.withStyledAttributes
import com.nex3z.togglebuttongroup.button.CircularToggle
import io.github.deweyreed.scrollhmspicker.ScrollHmsPicker
import io.github.deweyreed.timer.R
import xyz.aprildown.theme.Theme
import xyz.aprildown.theme.ThemeInflationDelegate
import xyz.aprildown.timer.app.timer.list.record.RecordTimersButton
import xyz.aprildown.timer.component.key.NameLoopView
import xyz.aprildown.tools.helper.themeColor
import io.github.deweyreed.scrollhmspicker.R as RScrollHmsPicker
import xyz.aprildown.timer.component.key.R as RComponentKey

class DynamicThemeDelegate : ThemeInflationDelegate() {

    @Suppress("SpellCheckingInspection")
    override fun createView(context: Context, name: String, attrs: AttributeSet?): View? =
        when (name) {

            "View" -> View(context, attrs).apply {
                tintBackground(this, attrs)
            }
            "androidx.constraintlayout.widget.ConstraintLayout" ->
                ConstraintLayout(context, attrs).apply {
                    tintBackground(this, attrs)
                }

            "com.nex3z.togglebuttongroup.button.CircularToggle" ->
                CircularToggle(context, attrs).apply {
                    markerColor = Theme.get().colorSecondary
                    setTextColor(
                        ColorStateList(
                            arrayOf(
                                intArrayOf(android.R.attr.state_checked),
                                intArrayOf()
                            ),
                            intArrayOf(
                                Theme.get().colorOnSecondary,
                                context.themeColor(android.R.attr.textColorPrimary)
                            )
                        )
                    )
                }
            "io.github.deweyreed.scrollhmspicker.ScrollHmsPicker" ->
                ScrollHmsPicker(context, attrs).apply {
                    context.withStyledAttributes(
                        attrs,
                        RScrollHmsPicker.styleable.ScrollHmsPicker
                    ) {
                        matchThemeColor(
                            typedArray = this,
                            index = RScrollHmsPicker.styleable.ScrollHmsPicker_shp_selected_color
                        )?.let {
                            setColorIntSelected(it)
                        }
                    }
                }
            "xyz.aprildown.timer.component.key.NameLoopView" ->
                NameLoopView(context, attrs).apply {
                    context.withStyledAttributes(attrs, RComponentKey.styleable.NameLoopView) {
                        matchThemeColor(
                            typedArray = this,
                            index = RComponentKey.styleable.NameLoopView_nlv_view_color
                        )?.let {
                            withColor(it)
                        }
                    }
                }
            "xyz.aprildown.timer.app.timer.list.record.RecordTimersButton" ->
                RecordTimersButton(context, attrs).apply {
                    applyDefaultTint(attrs)
                }
            else -> null
        }

    private fun tintBackground(view: View, attrs: AttributeSet?) {
        view.context.withStyledAttributes(attrs, R.styleable.Theme_View) {
            matchThemeColor(
                typedArray = this,
                index = R.styleable.Theme_View_android_background
            )?.let {
                view.setBackgroundColor(it)
            }
        }
    }
}
