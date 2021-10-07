package xyz.aprildown.timer.app.base.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import xyz.aprildown.timer.app.base.R

class ListEmptyView(
    context: Context,
    attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

    private val emptyImageView: ImageView

    var mode = MODE_CREATE
        set(value) {
            if (field == value) return

            field = value

            emptyImageView.run {
                when (value) {
                    MODE_CREATE -> {
                        setImageResource(R.drawable.ic_arrow_down)
                    }
                    MODE_DELETE -> {
                        setImageResource(R.drawable.ic_delete)
                    }
                }
            }
        }

    init {
        View.inflate(context, R.layout.view_list_empty_view, this)

        emptyImageView = findViewById(R.id.viewListEmptyView)
    }

    companion object {
        const val MODE_CREATE = 0
        const val MODE_DELETE = 1
    }
}
