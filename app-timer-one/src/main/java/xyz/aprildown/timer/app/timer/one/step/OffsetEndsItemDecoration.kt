package xyz.aprildown.timer.app.timer.one.step

import android.graphics.Rect
import android.view.View
import androidx.annotation.FloatRange
import androidx.recyclerview.widget.RecyclerView

/**
 * https://github.com/dadouf/PagingImageGallery/blob/master/app/src/main/java/com/davidferrand/pagingimagegallery/recyclerview/vfinal/CarouselActivity.kt#L104
 */
internal class OffsetEndsItemDecoration(
    @FloatRange(from = 0.0, to = 1.0) private val percent: Float = 0.33f
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)

        when (parent.getChildAdapterPosition(view)) {
            0 -> {
                outRect.top = getOffset(parent, view)
            }
            state.itemCount - 1 -> {
                outRect.bottom = getOffset(parent, view)
            }
        }
    }

    private fun getOffset(parent: RecyclerView, childView: View): Int {
        // It is crucial to refer to layoutParams.width (view.width is 0 at this time)!
        return (parent.height * percent).toInt() - childView.layoutParams.height / 2
    }
}
