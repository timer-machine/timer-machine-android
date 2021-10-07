package xyz.aprildown.timer.app.base.ui

import android.app.Activity
import xyz.aprildown.theme.Theme

interface DynamicTheme {
    val colorPrimary: Int
    val colorPrimaryVariant: Int
    val colorOnPrimary: Int
    val colorSecondary: Int
    val colorSecondaryVariant: Int
    val colorOnSecondary: Int

    val colorStatusBar: Int
    val colorNavigationBar: Int

    fun tintSystemUi(activity: Activity)

    var enabled: Boolean

    fun withoutDynamicTheme(r: DynamicThemeResume.() -> Unit)

    interface DynamicThemeResume {
        fun resume()
    }
}

val newDynamicTheme: DynamicTheme get() = DynamicThemeImpl()

private class DynamicThemeImpl : DynamicTheme {

    private val theme = Theme.get()

    override val colorPrimary: Int get() = theme.colorPrimary
    override val colorPrimaryVariant: Int get() = theme.colorPrimaryVariant
    override val colorOnPrimary: Int get() = theme.colorOnPrimary
    override val colorSecondary: Int get() = theme.colorSecondary
    override val colorSecondaryVariant: Int get() = theme.colorSecondaryVariant
    override val colorOnSecondary: Int get() = theme.colorOnSecondary
    override val colorStatusBar: Int get() = theme.colorStatusBar
    override val colorNavigationBar: Int get() = theme.colorNavigationBar

    override fun tintSystemUi(activity: Activity) {
        Theme.tintSystemUi(activity)
    }

    override var enabled: Boolean
        get() = Theme.get().enabled
        set(value) {
            Theme.get().enabled = value
        }

    override fun withoutDynamicTheme(r: DynamicTheme.DynamicThemeResume.() -> Unit) {
        enabled = false
        r.invoke(
            object : DynamicTheme.DynamicThemeResume {
                override fun resume() {
                    enabled = true
                }
            }
        )
    }
}
