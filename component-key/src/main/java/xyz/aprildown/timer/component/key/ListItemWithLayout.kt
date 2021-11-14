package xyz.aprildown.timer.component.key

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewStub
import android.widget.Checkable
import android.widget.RelativeLayout
import androidx.annotation.LayoutRes
import androidx.core.view.updateLayoutParams
import xyz.aprildown.tools.helper.setSelectableItemBackground
import xyz.aprildown.tools.R as RTools

class ListItemWithLayout(
    context: Context,
    attrs: AttributeSet? = null
) : RelativeLayout(context, attrs) {

    val listItem: ListItem
    private var inflatedView: View? = null

    init {
        gravity = Gravity.CENTER_VERTICAL
        setSelectableItemBackground()

        val primaryText: String?
        val secondaryText: String?
        val layoutRes: Int
        val divider: Int
        if (attrs != null) {
            val sa = context.obtainStyledAttributes(attrs, R.styleable.ListItemWithLayout)
            primaryText = sa.getString(R.styleable.ListItemWithLayout_li_textPrimary)
            secondaryText = sa.getString(R.styleable.ListItemWithLayout_li_textSecondary)
            layoutRes = sa.getResourceId(R.styleable.ListItemWithLayout_li_layout, 0)
            divider = sa.getInt(R.styleable.ListItemWithLayout_li_divider, 0)
            sa.recycle()
        } else {
            primaryText = null
            secondaryText = null
            layoutRes = 0
            divider = 0
        }

        View.inflate(context, R.layout.view_list_item_with_layout, this)

        listItem = getChildAt(0) as ListItem
        listItem.run {
            setBackgroundResource(0)
            setPrimaryText(primaryText)
            setSecondaryText(secondaryText)
        }

        setLayoutRes(layoutRes)

        if (divider != 0) {
            val dividerView = (getChildAt(2) as ViewStub).inflate()
            if (divider == DIVIDER_MARGIN) {
                dividerView.updateLayoutParams<LayoutParams> {
                    val margin = context.resources.getDimensionPixelSize(RTools.dimen.keyline_icon)
                    marginStart = margin
                    marginEnd = margin
                }
            }
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        inflatedView?.isEnabled = enabled
    }

    /**
     * This method CANNOT be used with [R.styleable.ListItemWithLayout_li_layout].
     */
    fun setLayoutRes(@LayoutRes layoutRes: Int) {
        if (layoutRes != 0) {
            val viewStub = getChildAt(1) as ViewStub
            viewStub.layoutResource = layoutRes
            inflatedView = viewStub.inflate()
            // listItem.updateLayoutParams<LayoutParams> {
            //     addRule(START_OF, R.id.viewListItemInflated)
            // }
            delegateClickToCheckableLayout()
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getLayoutView(): T = inflatedView as T

    fun delegateClickToCheckableLayout() {
        val view = inflatedView
        if (view is Checkable) {
            setOnClickListener {
                view.toggle()
            }
        }
    }
}

// private const val DIVIDER_FULL = 1
private const val DIVIDER_MARGIN = 2
