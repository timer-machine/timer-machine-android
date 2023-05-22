package xyz.aprildown.timer.app.base.data

import android.content.Context
import android.content.res.Resources
import androidx.annotation.FloatRange
import androidx.core.content.edit
import androidx.core.math.MathUtils
import com.github.deweyreed.tools.anko.dp
import xyz.aprildown.tools.helper.safeSharedPreference
import kotlin.math.min

class FloatingWindowPip(private val context: Context) {
    private val sp = context.safeSharedPreference

    var autoClose: Boolean
        get() = sp.getBoolean(KEY_AUTO_CLOSE, false)
        set(value) = sp.edit { putBoolean(KEY_AUTO_CLOSE, value) }

    /**
     * From 0.01 to 1.0.
     */
    var floatingWindowAlpha: Float
        get() = sp.getFloat(KEY_FLOATING_WINDOW_ALPHA, 1f)
        set(value) = sp.edit {
            putFloat(
                KEY_FLOATING_WINDOW_ALPHA,
                MathUtils.clamp(value, 0.01f, 1f)
            )
        }

    /**
     * From 0.0 to 1.0.
     * Min size: 128dp * 108dp. Max size: screen width * calculated height
     */
    var floatingWindowSize: Float
        get() = sp.getFloat(KEY_FLOATING_WINDOW_SIZE, 0f)
        set(value) = sp.edit {
            putFloat(KEY_FLOATING_WINDOW_SIZE, MathUtils.clamp(value, 0f, 1f))
        }

    fun calculateFloatingWindowSize(
        @FloatRange(from = 0.0, to = 1.0) percent: Float = floatingWindowSize
    ): Pair<Int, Int> {
        val ratio =
            FLOATING_WINDOW_MIN_WIDTH_DP.toFloat() / FLOATING_WINDOW_MIN_HEIGHT_DP.toFloat()
        val screenWidth: Int = Resources.getSystem().displayMetrics.run {
            min(widthPixels, heightPixels)
        }
        val minWidth: Int = context.dp(FLOATING_WINDOW_MIN_WIDTH_DP).toInt()
        val resultWidth: Float = minWidth + (screenWidth - minWidth) * percent
        return resultWidth.toInt() to (resultWidth / ratio).toInt()
    }

    companion object {
        const val KEY_AUTO_CLOSE = "key_auto_close_floating_window"
        const val KEY_FLOATING_WINDOW_ALPHA = "pref_floating_window_alpha"
        const val KEY_FLOATING_WINDOW_SIZE = "pref_floating_window_size"

        private const val FLOATING_WINDOW_MIN_WIDTH_DP = 128
        private const val FLOATING_WINDOW_MIN_HEIGHT_DP = 108
    }
}
