package org.bobstuff.bobboardview

import android.graphics.Rect
import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * BobBoardItemTouchHelper is a custom version of the concept that is present inside the open source
 * android recyclerview library
 *
 * Created by bob
 */

class BobBoardItemTouchHelper(private val maxScrollSpeed: Int,
                              private val callback: BobBoardItemTouchHelper.Callback):
        RecyclerView.OnChildAttachStateChangeListener {
    private var recyclerView: RecyclerView? = null
    private var debugEnabled: Boolean = false
    var selected: RecyclerView.ViewHolder? = null
    private var actionState = ACTION_STATE_IDLE
    private val swapTargets: MutableList<RecyclerView.ViewHolder> = mutableListOf()
    //re used for for sorting swap targets
    private var distances: MutableList<Int> = mutableListOf()
    /**
     * The reference coordinates for the action start. For drag & drop, this is the time long
     * press is completed vs for swipe, this is the initial touch point.
     */
    var initialTouchX: Float = 0f
    var initialTouchY: Float = 0f
    /**
     * The diff between the last event and initial touch.
     */
    var dX: Float = 0f
    var dY: Float = 0f

    var dragX = 0f
    var dragY = 0f
    /**
     * The coordinates of the selected view at the time it is selected. We record these values
     * when action starts so that we can consistently position it even if LayoutManager moves the
     * View.
     */
    var selectedStartX: Float = 0f

    var selectedStartY: Float = 0f
    /**
     * When user started to drag scroll. Reset when we don't scroll
     */
    private var dragScrollStartTimeInMs: Long = java.lang.Long.MIN_VALUE
    /**
     * Temporary rect instance that is used when we need to lookup Item decorations.
     */
    private var mTmpRect: Rect = Rect()
    private val scrollRunnable: Runnable = object : Runnable {
        override fun run() {
            if (debugEnabled) {
                Log.d(TAG, "scrollRunnable:run() | selected: $selected")
            }
            if (selected != null && scrollIfNecessary()) {
                if (selected != null) { //it might be lost during scrolling
                    moveIfNecessary(selected!!)
                }
                recyclerView?.removeCallbacks(this)
                ViewCompat.postOnAnimation(recyclerView, this)
            }
        }
    }

    fun attachToRecyclerView(recyclerView: RecyclerView?) {
        if (this.recyclerView === recyclerView) {
            return
        }
        this.recyclerView?.removeOnChildAttachStateChangeListener(this)
        this.recyclerView?.removeCallbacks(scrollRunnable)
        this.recyclerView = recyclerView
        this.recyclerView?.addOnChildAttachStateChangeListener(this)
    }

    fun startDrag(viewHolder: RecyclerView.ViewHolder, x: Float, y: Float) {
        if (viewHolder.itemView.parent !== recyclerView) {
            Log.e(TAG, "Start drag has been called with a view holder which is not a child of " + "the RecyclerView which is controlled by this CustomItemTouchHelper.")
            return
        }
        initialTouchX = x
        initialTouchY = y
        dY = 0f
        dX = dY
        select(viewHolder, ACTION_STATE_DRAG)
    }

    fun startDragManual(viewHolder: RecyclerView.ViewHolder) {
        if (viewHolder.itemView.parent !== recyclerView) {
            Log.e(TAG, "Start drag has been called with a view holder which is not a child of " + "the RecyclerView which is controlled by this CustomItemTouchHelper.")
            return
        }
        dY = 0f
        dX = dY
        select(viewHolder, ACTION_STATE_DRAG)
    }

    fun endDrag() {
        select(null, ACTION_STATE_IDLE)
    }

    fun drag(x: Float, y: Float) {
        recyclerView?: return

        updateDxDy(x, y)
        moveIfNecessary(selected!!)
        recyclerView?.removeCallbacks(scrollRunnable)
        scrollRunnable.run()
        recyclerView?.invalidate()
    }

    private fun updateDxDy(x: Float, y: Float) {
        dragX = when {
            x < 0f -> 0f
            x > recyclerView?.width?.toFloat()!! -> recyclerView?.width?.toFloat()!!
            else -> x
        }
        dragY = when {
            y < 0f -> 0f
            y > recyclerView?.height?.toFloat()!! -> recyclerView?.height?.toFloat()!!
            else -> y
        }
        dX = x - initialTouchX
        dY = y - initialTouchY
    }

    private fun select(selected: RecyclerView.ViewHolder?, actionState: Int) {
        if (selected === this.selected && actionState == this.actionState) {
            return
        }
        this.dragScrollStartTimeInMs = java.lang.Long.MIN_VALUE
        this.actionState = actionState

        if (selected != null) {
            this.selected = null
        }
        if (selected != null) {
            this.selectedStartX = selected.itemView.left.toFloat()
            this.selectedStartY = selected.itemView.top.toFloat()
            this.selected = selected

            if (actionState == ACTION_STATE_DRAG) {
                selected.itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        }
        recyclerView?.parent?.requestDisallowInterceptTouchEvent(selected != null)
        recyclerView?.invalidate()
    }

    /**
     * Checks if we should swap w/ another view holder.
     */
    fun moveIfNecessary(viewHolder: RecyclerView.ViewHolder) {
        val recyclerView = recyclerView?: return
        if (recyclerView.isLayoutRequested || actionState != ACTION_STATE_DRAG) {
            return
        }

        val x = dX.toInt()
        val y = dY.toInt()

        val swapTargets = findSwapTargets(viewHolder)
        if (debugEnabled) {
            Log.d(TAG, "moveIfNecessary() | x: $x; y: $y; swapTargets: $swapTargets")
        }
        if (swapTargets.isEmpty()) {
            return
        }
        // may swap.
        val target = chooseDropTarget(viewHolder, swapTargets, x, y)
        if (debugEnabled) {
            Log.d(TAG, "moveIfNecessary() | x: $x; y: $y; dropTarget: $target")
        }
        if (target == null) {
            swapTargets.clear()
            distances.clear()
            return
        }
        val toPosition = target.adapterPosition
        val fromPosition = viewHolder.adapterPosition
        if (callback.onMove(recyclerView, viewHolder, target)) {
            val layoutManager = recyclerView.layoutManager
            val targetPos = target.adapterPosition
            recyclerView.scrollToPosition(targetPos)
            if (layoutManager is ItemTouchHelper.ViewDropHandler) {
                (layoutManager as ItemTouchHelper.ViewDropHandler).prepareForDrop(viewHolder.itemView,
                        target.itemView, x, y)
                return
            }

            // keep target visible
            //mCallback.onMoved(mRecyclerView, viewHolder, fromPosition,
            //        target, toPosition, x, y)
        }
    }

    private fun canDropOver(): Boolean {
        //TODO implement something like this
        return true
    }

    private fun findSwapTargets(viewHolder: RecyclerView.ViewHolder): MutableList<RecyclerView.ViewHolder> {
        swapTargets.clear()
        distances.clear()

        val recyclerView = recyclerView?: return swapTargets

        val left = Math.round(dX)
        val top = Math.round(dY)
        val right = left + viewHolder.itemView.width
        val bottom = top + viewHolder.itemView.height
        val centerX = (left + right) / 2
        val centerY = (top + bottom) / 2
        val lm = recyclerView.layoutManager
        val childCount = lm.childCount
        for (i in 0 until childCount) {
            val other = lm.getChildAt(i)
            if (other === viewHolder.itemView) {
                continue
            }
            if (other.bottom < top || other.top > bottom
                    || other.right < left || other.left > right) {
                continue
            }
            val otherVh = recyclerView.getChildViewHolder(other)
            val vlp = otherVh.itemView.layoutParams as RecyclerView.LayoutParams
            if (!vlp.isItemRemoved && callback.canDropOver(recyclerView, selected!!, otherVh)) {
                // find the index to add
                val dx = Math.abs(centerX - (other.left + other.right) / 2)
                val dy = Math.abs(centerY - (other.top + other.bottom) / 2)
                val dist = dx * dx + dy * dy

                var pos = 0
                val cnt = swapTargets.size
                for (j in 0 until cnt) {
                    if (dist > distances[j]) {
                        pos++
                    } else {
                        break
                    }
                }
                swapTargets.add(pos, otherVh)
                distances.add(pos, dist)
            }
        }
        return swapTargets
    }

    fun chooseDropTarget(selected: RecyclerView.ViewHolder,
                         dropTargets: List<RecyclerView.ViewHolder>, curX: Int, curY: Int): RecyclerView.ViewHolder? {
        val right = curX + selected.itemView.width
        val bottom = curY + selected.itemView.height
        var winner: RecyclerView.ViewHolder? = null
        var winnerScore = -1
        val dx = curX - selected.itemView.left
        val dy = curY - selected.itemView.top
        val targetsSize = dropTargets.size
        for (i in 0 until targetsSize) {
            val target = dropTargets[i]
            if (dx > 0) {
                val diff = target.itemView.right - right
                if (diff < 0 && target.itemView.right > selected.itemView.right) {
                    val score = Math.abs(diff)
                    if (score > winnerScore) {
                        winnerScore = score
                        winner = target
                    }
                }
            }
            if (dx < 0) {
                val diff = target.itemView.left - curX
                if (diff > 0 && target.itemView.left < selected.itemView.left) {
                    val score = Math.abs(diff)
                    if (score > winnerScore) {
                        winnerScore = score
                        winner = target
                    }
                }
            }
            if (dy < 0) {
                val diff = target.itemView.top - curY
                if (diff > 0 && target.itemView.top < selected.itemView.top) {
                    val score = Math.abs(diff)
                    if (score > winnerScore) {
                        winnerScore = score
                        winner = target
                    }
                }
            }

            if (dy > 0) {
                val diff = target.itemView.bottom - bottom
                if (diff < 0 && target.itemView.bottom > selected.itemView.bottom) {
                    val score = Math.abs(diff)
                    if (score > winnerScore) {
                        winnerScore = score
                        winner = target
                    }
                }
            }
        }
        return winner
    }

    private fun scrollIfNecessary(): Boolean {
        if (debugEnabled) {
            Log.d(TAG, "scrollIfNecessary() | recyclerView ${recyclerView?.scrollX}, ${recyclerView?.scrollY}; dragX: $dragX; dragY: $dragY; dX: $dX; dY: $dY")
        }
        if (selected == null) {
            dragScrollStartTimeInMs = java.lang.Long.MIN_VALUE
            return false
        }
        val now = System.currentTimeMillis()
        val scrollDuration = if (dragScrollStartTimeInMs == java.lang.Long.MIN_VALUE)
            0
        else
            now - dragScrollStartTimeInMs

        val recyclerView = recyclerView?: return false

        val lm = recyclerView.layoutManager
        var scrollX = 0
        var scrollY = 0
        //implement scroll zone
        //check if touch point is inside the scroll zone
        val width = recyclerView.width
        val height = recyclerView.height
        val scrollZone = 200
        if (debugEnabled) {
            Log.d(TAG, "scrollIfNecessary() | lm.canScrollHorizontally: ${lm.canScrollHorizontally()}; lm.canScrollVertically: ${lm.canScrollVertically()}; recyclerView width: $width; recyclerView height: $height")
        }
        if (lm.canScrollHorizontally()) {
            scrollX = when {
                dragX < scrollZone ->
                    -interpolateOutOfBoundsScroll(scrollZone, dragX.toInt(), scrollDuration)
                dragX > width - scrollZone ->
                    interpolateOutOfBoundsScroll(scrollZone, scrollZone - (dragX.toInt() - (width - scrollZone)), scrollDuration)
                else -> 0
            }
        }
        if (lm.canScrollVertically()) {
            scrollY = when {
                dragY < scrollZone ->
                    -interpolateOutOfBoundsScroll(scrollZone, dragY.toInt(), scrollDuration)
                dragY > height - scrollZone ->
                    interpolateOutOfBoundsScroll(scrollZone, scrollZone - (dragY.toInt() - (height - scrollZone)), scrollDuration)
                else -> 0
            }
        }
        if (debugEnabled) {
            Log.d(TAG, "scrollIfNecessary() | calculated scrollX: $scrollX; scrollY: $scrollY; dragScrollStartTimeInMs: $dragScrollStartTimeInMs")
        }
        if (scrollX != 0 || scrollY != 0) {
            if (dragScrollStartTimeInMs == java.lang.Long.MIN_VALUE) {
                dragScrollStartTimeInMs = now
            }
            recyclerView.scrollBy(scrollX, scrollY)
            return true
        }
        dragScrollStartTimeInMs = java.lang.Long.MIN_VALUE
        return false
    }

    private fun interpolateOutOfBoundsScroll(scrollZone: Int,
                                             scrollOverlap: Int,
                                             msSinceStartScroll: Long): Int {
        val maxScroll = maxScrollSpeed
        val outOfBoundsRatio = Math.min(1f, 1f * (scrollZone - scrollOverlap) / scrollZone)
        val cappedScroll = (maxScroll *
                BobBoardScroller.sDragViewScrollCapInterpolator.getInterpolation(outOfBoundsRatio)).toInt()
        val timeRatio = if (msSinceStartScroll > BobBoardScroller.DRAG_SCROLL_ACCELERATION_LIMIT_TIME_MS) {
            1f
        } else {
            msSinceStartScroll.toFloat() / BobBoardScroller.DRAG_SCROLL_ACCELERATION_LIMIT_TIME_MS
        }
        val value = (cappedScroll * BobBoardScroller.sDragScrollInterpolator
                .getInterpolation(timeRatio)).toInt()

        return if (value < 6) {
            6
        } else value
    }

    override fun onChildViewAttachedToWindow(view: View) {
        //no-op
    }

    override fun onChildViewDetachedFromWindow(view: View) {
        Log.d("TEST", "Child detatched from window")
        val holder = recyclerView?.getChildViewHolder(view) ?: return
        if (selected != null && holder === selected) {
            Log.d("TEST", "idling because tracked child was detached")
            select(null, ACTION_STATE_IDLE)
        }
    }

    abstract class Callback {
        abstract fun onMove(recyclerView: RecyclerView,
                            viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean

        abstract fun canDropOver(recyclerView: RecyclerView, current: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder): Boolean
    }

    companion object {
        const val TAG = "BobBoardItemTouchHelper"
        const val ACTION_STATE_IDLE = 0
        const val ACTION_STATE_DRAG = 1
    }
}