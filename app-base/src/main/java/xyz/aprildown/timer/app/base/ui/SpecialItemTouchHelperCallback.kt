package xyz.aprildown.timer.app.base.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import xyz.aprildown.timer.app.base.R
import xyz.aprildown.tools.helper.color
import xyz.aprildown.tools.helper.drawable
import xyz.aprildown.tools.utils.ThemeColorUtils
import kotlin.math.abs
import com.mikepenz.materialize.R as RMaterialize
import xyz.aprildown.tools.R as RTools

class SpecialItemTouchHelperCallback(
    private val context: Context,
    private val config: Config,
    private val canBeSwiped: ((viewHolder: RecyclerView.ViewHolder) -> Boolean)? = null,
    private val startSwipeCallback: SwipeCallback? = null,
    private val endSwipeCallback: SwipeCallback? = null,
) : ItemTouchHelper.SimpleCallback(
    0,
    ItemTouchHelper.START or ItemTouchHelper.END
) {

    class Config {

        @ColorInt
        var inactiveBackgroundColor: Int = 0

        @ColorInt
        var inactiveIconColor: Int = 0

        @DrawableRes
        var startIconRes: Int = 0

        @ColorInt
        var startActiveIconColor: Int = 0

        @ColorInt
        var startActiveBackgroundColor: Int = 0

        @DrawableRes
        var endIconRes: Int = 0

        @ColorInt
        var endActiveIconColor: Int = 0

        @ColorInt
        var endActiveBackgroundColor: Int = 0

        var topPadding: Int = 0
        var bottomPadding: Int = 0

        companion object {
            fun getEditDeleteConfig(context: Context): Config = Config().apply {
                inactiveBackgroundColor = context.color(RMaterialize.color.md_grey_300)
                inactiveIconColor = context.color(RMaterialize.color.md_grey_700)

                startIconRes = R.drawable.ic_edit
                startActiveIconColor = context.color(RMaterialize.color.md_green_800)
                startActiveBackgroundColor = context.color(RMaterialize.color.md_green_200)

                endIconRes = R.drawable.ic_delete
                endActiveIconColor = context.color(RMaterialize.color.md_red_500)
                endActiveBackgroundColor = ThemeColorUtils.adjustAlpha(endActiveIconColor, 0.2f)
            }
        }
    }

    interface SwipeCallback {
        fun onSwipe(viewHolder: RecyclerView.ViewHolder)
    }

    private var initialized = false

    private lateinit var startIcon: Drawable
    private lateinit var endIcon: Drawable

    private val iconPadding: Int =
        context.resources.getDimensionPixelSize(RTools.dimen.keyline_icon)

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun isLongPressDragEnabled(): Boolean = false

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float = 0.5f
    override fun getSwipeVelocityThreshold(defaultValue: Float): Float = defaultValue * 0.5f
    override fun getSwipeEscapeVelocity(defaultValue: Float): Float = defaultValue * 4f

    override fun getSwipeDirs(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        return if (canBeSwiped?.invoke(viewHolder) == false) {
            0
        } else {
            super.getSwipeDirs(recyclerView, viewHolder)
        }
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        when (direction) {
            ItemTouchHelper.START -> startSwipeCallback?.onSwipe(viewHolder)
            ItemTouchHelper.END -> endSwipeCallback?.onSwipe(viewHolder)
        }
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        if (dX == 0f) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            return
        }

        val left = viewHolder.itemView.left.toFloat()
        val top = viewHolder.itemView.top.toFloat() + config.topPadding
        val right = viewHolder.itemView.right.toFloat()
        val bottom =
            (viewHolder.itemView.bottom.toFloat() - config.bottomPadding).coerceAtLeast(top)
        val width = right - left
        val height = bottom - top
        val saveCount = c.save()

        initialize(context)

        val progress = abs(dX) / width
        val swipeThreshold = getSwipeThreshold(viewHolder)

        val iconScale: Float = when (progress) {
            in 0f..swipeThreshold -> 1f
            in swipeThreshold..(swipeThreshold + SCALE_DISTANCE) ->
                1f + ((progress - swipeThreshold) / SCALE_DISTANCE * ICON_SCALE_OFFSET)
            else -> 1f + ICON_SCALE_OFFSET
        }

        if (dX > 0) {
            c.clipRect(left, top, left + dX, bottom)

            if (progress > swipeThreshold) {
                c.drawColor(config.startActiveBackgroundColor)
                startIcon.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                    config.startActiveIconColor,
                    BlendModeCompat.SRC_IN
                )
            } else {
                c.drawColor(config.inactiveBackgroundColor)
                startIcon.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                    config.inactiveIconColor,
                    BlendModeCompat.SRC_IN
                )
            }

            val cx = left + iconPadding + startIcon.intrinsicWidth / 2f

            val cy = top + height / 2f
            val halfIconSize = startIcon.intrinsicWidth * iconScale / 2f
            startIcon.setBounds(
                (cx - halfIconSize).toInt(), (cy - halfIconSize).toInt(),
                (cx + halfIconSize).toInt(), (cy + halfIconSize).toInt()
            )
            startIcon.draw(c)
        } else {
            c.clipRect(right + dX, top, right, bottom)

            if (progress > swipeThreshold) {
                c.drawColor(config.endActiveBackgroundColor)
                endIcon.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                    config.endActiveIconColor,
                    BlendModeCompat.SRC_IN
                )
            } else {
                c.drawColor(config.inactiveBackgroundColor)
                endIcon.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                    config.inactiveIconColor,
                    BlendModeCompat.SRC_IN
                )
            }

            val cx = right - iconPadding - endIcon.intrinsicWidth / 2f

            val cy = top + height / 2f
            val halfIconSize = endIcon.intrinsicWidth * iconScale / 2f
            endIcon.setBounds(
                (cx - halfIconSize).toInt(), (cy - halfIconSize).toInt(),
                (cx + halfIconSize).toInt(), (cy + halfIconSize).toInt()
            )
            endIcon.draw(c)
        }

        c.restoreToCount(saveCount)

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    private fun initialize(context: Context) {
        if (!initialized) {
            initialized = true

            startIcon = context.drawable(config.startIconRes).mutate()
            endIcon = context.drawable(config.endIconRes).mutate()
        }
    }
}

private const val SCALE_DISTANCE = 0.025f
private const val ICON_SCALE_OFFSET = 0.2f
