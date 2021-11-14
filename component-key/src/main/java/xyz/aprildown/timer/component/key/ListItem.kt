package xyz.aprildown.timer.component.key

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import xyz.aprildown.tools.helper.gone
import xyz.aprildown.tools.helper.setSelectableItemBackground
import xyz.aprildown.tools.helper.show
import xyz.aprildown.tools.R as RTools

class ListItem(
    context: Context,
    attrs: AttributeSet
) : LinearLayout(context, attrs) {

    private val primaryTextView: TextView
    private val secondaryTextView: TextView

    init {
        val res = context.resources
        orientation = VERTICAL
        gravity = Gravity.CENTER_VERTICAL
        setSelectableItemBackground()

        val keylineIconDimen = res.getDimensionPixelSize(RTools.dimen.keyline_icon)
        val verticalPadding = res.getDimensionPixelSize(R.dimen.list_item_text_vertical_padding)
        setPadding(keylineIconDimen, verticalPadding, keylineIconDimen, verticalPadding)

        val sa = context.obtainStyledAttributes(attrs, R.styleable.ListItem)
        val primaryText = sa.getString(R.styleable.ListItem_li_textPrimary)
        val secondaryText = sa.getString(R.styleable.ListItem_li_textSecondary)
        sa.recycle()

        View.inflate(context, R.layout.view_list_item, this)

        primaryTextView = getChildAt(0) as TextView
        setPrimaryText(primaryText)

        secondaryTextView = getChildAt(1) as TextView
        setSecondaryText(secondaryText)

        adjustHeight(secondaryText)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // Found at material-components-android snackbar implementation
        if (secondaryTextView.lineCount > 1) {
            minimumHeight =
                context.resources.getDimensionPixelSize(R.dimen.list_item_three_lines_height)
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    fun setPrimaryText(@StringRes stringRes: Int) {
        setPrimaryText(resources.getString(stringRes))
    }

    fun setPrimaryText(text: String?) {
        primaryTextView.text = text
    }

    fun setSecondaryText(@StringRes stringRes: Int) {
        setSecondaryText(resources.getString(stringRes))
    }

    fun setSecondaryText(text: String?) {
        secondaryTextView.text = text
        adjustHeight(text)
    }

    private fun adjustHeight(secondaryText: String?) {
        val res = context.resources
        @Suppress("LiftReturnOrAssignment")
        if (secondaryText.isNullOrBlank()) {
            secondaryTextView.gone()
            // setMinimumHeight will trigger requestLayout which calls onMeasure
            minimumHeight = res.getDimensionPixelSize(R.dimen.list_item_one_line_height)
        } else {
            secondaryTextView.show()
            minimumHeight = res.getDimensionPixelSize(R.dimen.list_item_two_lines_height)
        }
    }
}
