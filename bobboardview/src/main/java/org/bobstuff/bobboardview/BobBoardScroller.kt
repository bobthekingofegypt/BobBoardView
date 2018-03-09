package org.bobstuff.bobboardview

import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.animation.Interpolator

/**
 * A board is made up of columns and rows.
 *
 * <-- Viewport -->
 * <--         content view       -->
 * ----------------------------------
 * | column A | column B | column C |
 * | ------------------------------ |
 * | row 1a    | row 1b    | row 1c |
 * | row 2a    | row 2b    | row 2c |
 * | row 3a    | row 3b    | row 3c |
 * | row 4a    | row 4b    | row 4c |
 * |--------------------------------|
 *
 * If the viewport is smaller than the content view, ie you can only see part of the horizontal
 * representation of your board then when you move an item from column A to column C we need to
 * move the BoardViews horizonal recycler as you drag the item near the edge.  This class is
 * responsible for doing that.  It should accelerate the movement the longer you hover and the closer
 * your drag point is to the edge of the screen and it should stop when you move the view away from
 * the edge.  It's not actually using the drag views position but your fingers position, this is
 * because you can start a drag from the very edge of a view meaning you wouldn't be able to move it
 * outside the bounds properly.
 *
 * Created by bob
 */

class BobBoardScroller(private val recyclerView: RecyclerView,
                       private val percentageScrollZone: Int,
                       private val maxScrollSpeed: Int,
                       private val bobBoardScrollerCallback: BobBoardScrollerCallback) {
    private var dx: Int = 0
    private var dy: Int = 0
    private var dragScrollStartTimeInMs: Long = 0
    private var isTracking: Boolean = false
    private var scrollZone: Int = 0
    private var accumulatedScrollX: Int = 0
    private val scrollRunnable: Runnable = object : Runnable {
        override fun run() {
            //Log.d("TEST", "MONKEY!!!!!")
            if (scrollIfNecessary()) {
                bobBoardScrollerCallback.onScrolled(accumulatedScrollX)
                recyclerView.removeCallbacks(this)
                ViewCompat.postOnAnimation(recyclerView, this)
            }
        }
    }

    interface BobBoardScrollerCallback {
        fun onScrolled(accumulatedScroll: Int)
    }

    fun startTracking() {
        this.scrollZone = (recyclerView.width * (percentageScrollZone / 100.0)).toInt()
        dragScrollStartTimeInMs = java.lang.Long.MIN_VALUE
        isTracking = true
    }

    fun stopTracking() {
        dragScrollStartTimeInMs = java.lang.Long.MIN_VALUE
        isTracking = false
    }

    fun updateDrag(x: Float, y: Float) {
        dx = x.toInt()
        dy = y.toInt()
        scrollRunnable.run()
    }

    private fun interpolateOutOfBoundsScroll(
                                     scrollZone: Int,
                                     scrollOverlap: Int,
                                     msSinceStartScroll: Long): Int {
        val maxScroll = maxScrollSpeed
        val outOfBoundsRatio = Math.min(1f, 1f * scrollOverlap / scrollZone)
        val cappedScroll = (maxScroll *
                sDragViewScrollCapInterpolator.getInterpolation(outOfBoundsRatio)).toInt()
        val timeRatio = if (msSinceStartScroll > DRAG_SCROLL_ACCELERATION_LIMIT_TIME_MS) {
            1f
        } else {
            msSinceStartScroll.toFloat() / DRAG_SCROLL_ACCELERATION_LIMIT_TIME_MS
        }
        val value = (cappedScroll * sDragScrollInterpolator
                .getInterpolation(timeRatio)).toInt()

        return if (value == 0) {
            1
        } else value
    }

    internal fun scrollIfNecessary(): Boolean {
        if (!isTracking) {
            dragScrollStartTimeInMs = java.lang.Long.MIN_VALUE
            return false
        }
        val now = System.currentTimeMillis()
        val scrollDuration = if (dragScrollStartTimeInMs == java.lang.Long.MIN_VALUE)
            0
        else
            now - dragScrollStartTimeInMs
        val width = recyclerView.width
        val scrollX = when {
            dx < scrollZone && recyclerView.canScrollHorizontally(LEFT) ->
                -interpolateOutOfBoundsScroll(scrollZone, dx, scrollDuration)
            dx > width - scrollZone && recyclerView.canScrollHorizontally(RIGHT) ->
                interpolateOutOfBoundsScroll(scrollZone, dx - (width - scrollZone), scrollDuration)
            else -> 0
        }

        if (scrollX != 0) {
            if (dragScrollStartTimeInMs == java.lang.Long.MIN_VALUE) {
                dragScrollStartTimeInMs = now
            }
            accumulatedScrollX += scrollX
            recyclerView.scrollBy(scrollX, 0)
            return true
        }
        dragScrollStartTimeInMs = java.lang.Long.MIN_VALUE
        accumulatedScrollX = 0
        return false
    }

    companion object {
        private const val LEFT = -1
        private const val RIGHT = 1
        const val DRAG_SCROLL_ACCELERATION_LIMIT_TIME_MS: Long = 2500
        val sDragScrollInterpolator = Interpolator { t -> t * t * t * t * t }
        val sDragViewScrollCapInterpolator = Interpolator { t ->
            var t = t
            t -= 1.0f
            t * t * t * t * t + 1.0f
        }
    }
}
