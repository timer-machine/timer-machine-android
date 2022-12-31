package xyz.aprildown.timer.component.key

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatTextView

/**
 * https://github.com/apg-mobile/android-round-textview
 */
class RoundTextView : AppCompatTextView {

    private var tvBgColor = Color.TRANSPARENT
    private var tvAllCorner: Float = 0.toFloat()
    private var tvTopLeftCorner = DEFAULT_CORNER.toFloat()
    private var tvTopRightCorner = DEFAULT_CORNER.toFloat()
    private var tvBottomRightCorner = DEFAULT_CORNER.toFloat()
    private var tvBottomLeftCorner = DEFAULT_CORNER.toFloat()

    constructor(context: Context) : super(context) {
        setViewBackground()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        extractAttribute(context, attrs)
        setViewBackground()
    }

    constructor(
        context: Context,
        attrs: AttributeSet,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        extractAttribute(context, attrs)
        setViewBackground()
    }

    private fun extractAttribute(context: Context, attrs: AttributeSet) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.RoundTextView, 0, 0)
        try {
            tvBgColor = ta.getColor(R.styleable.RoundTextView_bgColor, Color.TRANSPARENT)
            tvAllCorner =
                ta.getDimension(R.styleable.RoundTextView_allCorner, java.lang.Float.MIN_VALUE)
            tvTopLeftCorner = ta.getDimension(
                R.styleable.RoundTextView_topLeftCorner,
                DEFAULT_CORNER.toFloat()
            )
            tvTopRightCorner = ta.getDimension(
                R.styleable.RoundTextView_topRightCorner,
                DEFAULT_CORNER.toFloat()
            )
            tvBottomRightCorner = ta.getDimension(
                R.styleable.RoundTextView_bottomRightCorner,
                DEFAULT_CORNER.toFloat()
            )
            tvBottomLeftCorner = ta.getDimension(
                R.styleable.RoundTextView_bottomLeftCorner,
                DEFAULT_CORNER.toFloat()
            )
        } finally {
            ta.recycle()
        }
    }

    // fun setCorner(all: Int) {
    //     tvAllCorner = all.toFloat()
    //     setViewBackground()
    // }

    // fun setCorner(topLeft: Int, topRight: Int, bottomRight: Int, bottomLeft: Int) {
    //     tvTopLeftCorner = topLeft.toFloat()
    //     tvTopRightCorner = topRight.toFloat()
    //     tvBottomRightCorner = bottomRight.toFloat()
    //     tvBottomLeftCorner = bottomLeft.toFloat()
    //     setViewBackground()
    // }

    fun setBgColor(color: Int) {
        tvBgColor = color
        setViewBackground()
    }

    private fun setViewBackground() {
        val drawable = if (tvAllCorner != java.lang.Float.MIN_VALUE) {
            getCornerDrawable(tvAllCorner, tvAllCorner, tvAllCorner, tvAllCorner, tvBgColor)
        } else {
            getCornerDrawable(
                tvTopLeftCorner,
                tvTopRightCorner,
                tvBottomLeftCorner,
                tvBottomRightCorner,
                tvBgColor
            )
        }
        this.background = drawable
    }
}

private const val DEFAULT_CORNER = 5
// private val DEFAULT_ALL_CORNER = Integer.MIN_VALUE

private fun getCornerDrawable(
    topLeft: Float,
    topRight: Float,
    bottomLeft: Float,
    bottomRight: Float,
    @ColorInt color: Int
): Drawable {
    val outerR = FloatArray(8)
    outerR[0] = topLeft
    outerR[1] = topLeft
    outerR[2] = topRight
    outerR[3] = topRight
    outerR[4] = bottomRight
    outerR[5] = bottomRight
    outerR[6] = bottomLeft
    outerR[7] = bottomLeft

    val drawable = ShapeDrawable()
    drawable.shape = RoundRectShape(outerR, null, null)
    drawable.paint.color = color

    return drawable
}
