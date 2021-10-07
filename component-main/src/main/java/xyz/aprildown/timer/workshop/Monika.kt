package xyz.aprildown.timer.workshop

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import xyz.aprildown.tools.anko.sp

class Monika : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val monika = requireContext()
        return RecyclerView(monika).apply {
            setHasFixedSize(true)
            overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            setBackgroundColor(Color.parseColor("#fee6f4"))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val monika = requireActivity()
        val monika1 = view as RecyclerView
        val monika2 = LinearLayoutManager(monika)
        monika1.layoutManager = monika2

        val monika4 = Resources.getSystem().displayMetrics.widthPixels

        val monika5 = MonikaAdapter(monika, monika4)
        monika1.adapter = monika5
        monika1.addOnScrollListener(object : EndlessRecyclerViewScrollListener(monika2) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView?) {
                monika5.more()
            }
        })
    }

    private class MonikaAdapter(
        monika: Context,
        monika1: Int
    ) : RecyclerView.Adapter<MonikaViewHolder>() {

        companion object {
            private const val MONIKA = "Just Monika."
            private const val MONIKA1 = MONIKA.length
            private const val MONIKA2 = 17f
            private const val MONIKA3 = 100
        }

        private var monika = MONIKA3
        private val monika1 = monika.sp(MONIKA2)
        private val monika2: String

        init {
            val monika3 = Paint()
            monika3.textSize = this.monika1
            val monika4 = monika3.measureText(MONIKA).toInt()

            monika2 = buildString {
                repeat(monika1 / monika4) {
                    append(MONIKA).append(" ")
                }
                // first to fill the end empty, second to substring
                append(MONIKA).append(MONIKA)
            }
        }

        fun more() {
            val monika3 = monika
            monika += MONIKA3
            notifyItemRangeInserted(monika3, MONIKA3)
        }

        override fun getItemCount(): Int = monika

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonikaViewHolder {
            return MonikaViewHolder(TextView(parent.context).apply {
                maxLines = 1
                setSingleLine()
                setTextSize(TypedValue.COMPLEX_UNIT_SP, MONIKA2)
                setTextColor(ColorStateList.valueOf(Color.BLACK))
                setTypeface(typeface, Typeface.BOLD)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            })
        }

        override fun onBindViewHolder(holder: MonikaViewHolder, position: Int) {
            (holder.itemView as TextView).text =
                (monika2.substring(position % MONIKA1))
        }
    }

    private class MonikaViewHolder(view: View) : RecyclerView.ViewHolder(view)

    /**
     * https://gist.github.com/nesquena/d09dc68ff07e845cc622
     */
    @Suppress("MemberVisibilityCanBePrivate")
    private abstract class EndlessRecyclerViewScrollListener(
        layoutManager: LinearLayoutManager
    ) : RecyclerView.OnScrollListener() {
        // The minimum amount of items to have below your current scroll position
        // before loading more.
        private var visibleThreshold = 30

        // The current offset index of data you have loaded
        private var currentPage = 0

        // The total number of items in the data set after the last load
        private var previousTotalItemCount = 0

        // True if we are still waiting for the last set of data to load.
        private var loading = true

        // Sets the starting page index
        private val startingPageIndex = 0

        val mLayoutManager: RecyclerView.LayoutManager = layoutManager

        fun getLastVisibleItem(lastVisibleItemPositions: IntArray): Int {
            var maxSize = 0
            for (i in lastVisibleItemPositions.indices) {
                if (i == 0) {
                    maxSize = lastVisibleItemPositions[i]
                } else if (lastVisibleItemPositions[i] > maxSize) {
                    maxSize = lastVisibleItemPositions[i]
                }
            }
            return maxSize
        }

        // This happens many times a second during a scroll, so be wary of the code you place here.
        // We are given a few useful parameters to help us work out if we need to load some more data,
        // but first we check if we are waiting for the previous load to finish.
        override fun onScrolled(view: RecyclerView, dx: Int, dy: Int) {
            var lastVisibleItemPosition = 0
            val totalItemCount = mLayoutManager.itemCount

            when (mLayoutManager) {
                is LinearLayoutManager -> {
                    lastVisibleItemPosition = mLayoutManager.findLastVisibleItemPosition()
                }
                is StaggeredGridLayoutManager -> {
                    val lastVisibleItemPositions =
                        mLayoutManager.findLastVisibleItemPositions(null)
                    // get maximum element within the list
                    lastVisibleItemPosition = getLastVisibleItem(lastVisibleItemPositions)
                }
                is GridLayoutManager -> {
                    lastVisibleItemPosition = mLayoutManager.findLastVisibleItemPosition()
                }
            }

            // If the total item count is zero and the previous isn't, assume the
            // list is invalidated and should be reset back to initial state
            // If it’s still loading, we check to see if the data set count has
            // changed, if so we conclude it has finished loading and update the current page
            // number and total item count.

            // If it isn’t currently loading, we check to see if we have breached
            // the visibleThreshold and need to reload more data.
            // If we do need to reload some more data, we execute onLoadMore to fetch the data.
            // threshold should reflect how many total columns there are too

            // If the total item count is zero and the previous isn't, assume the
            // list is invalidated and should be reset back to initial state
            if (totalItemCount < previousTotalItemCount) {
                this.currentPage = this.startingPageIndex
                this.previousTotalItemCount = totalItemCount
                if (totalItemCount == 0) {
                    this.loading = true
                }
            }
            // If it’s still loading, we check to see if the data set count has
            // changed, if so we conclude it has finished loading and update the current page
            // number and total item count.
            if (loading && totalItemCount > previousTotalItemCount) {
                loading = false
                previousTotalItemCount = totalItemCount
            }

            // If it isn’t currently loading, we check to see if we have breached
            // the visibleThreshold and need to reload more data.
            // If we do need to reload some more data, we execute onLoadMore to fetch the data.
            // threshold should reflect how many total columns there are too
            if (!loading && lastVisibleItemPosition + visibleThreshold > totalItemCount) {
                currentPage++
                onLoadMore(currentPage, totalItemCount, view)
                loading = true
            }
        }

        // Call this method whenever performing new searches
        // fun resetState() {
        //     this.currentPage = this.startingPageIndex
        //     this.previousTotalItemCount = 0
        //     this.loading = true
        // }

        // Defines the process for actually loading more data based on page
        abstract fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView?)
    }
}
