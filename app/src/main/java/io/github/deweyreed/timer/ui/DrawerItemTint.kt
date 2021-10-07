package io.github.deweyreed.timer.ui

import android.content.Context
import android.content.res.ColorStateList
import io.github.deweyreed.timer.R
import xyz.aprildown.timer.app.base.ui.newDynamicTheme
import xyz.aprildown.tools.helper.color
import xyz.aprildown.tools.helper.themeColor

internal class DrawerItemTint(private val context: Context) {

    private val dynamicTheme = newDynamicTheme

    fun createTextColorTint(): ColorStateList = ColorStateList(
        arrayOf(
            intArrayOf(android.R.attr.state_selected),
            intArrayOf()
        ),
        intArrayOf(
            dynamicTheme.colorPrimary,
            context.themeColor(android.R.attr.textColorPrimary)
        )
    )

    fun createIconTint(): ColorStateList = ColorStateList(
        arrayOf(
            intArrayOf(android.R.attr.state_selected),
            intArrayOf()
        ),
        intArrayOf(
            dynamicTheme.colorPrimary,
            context.color(R.color.drawer_item_icon_unselected)
        )
    )
}
