package org.bobstuff.bobboardview

import android.graphics.Rect
import androidx.recyclerview.widget.RecyclerView
import android.view.View

/**
 * Simple item divider that attempts to add equal spacing to all items, it does this by adding the
 * full padding to starting edges of first item and ending edges of last items but half padding to each
 * side for all middle items
 *
 * @TODO should this be part of the library of just a utility in the sample project?
 *
 * Created by bob
 */
enum class BobBoardSimpleDividersOrientation { VERTICAL, HORIZONTAL }

class BobBoardSimpleDividers(private val dividerSizeInPixels: Int,
                             private val orientation: BobBoardSimpleDividersOrientation):
        RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        val itemCount = parent.adapter!!.itemCount

        when (orientation) {
            BobBoardSimpleDividersOrientation.HORIZONTAL -> getHorizontalItemOffsets(outRect, position, itemCount)
            BobBoardSimpleDividersOrientation.VERTICAL -> getVerticalItemOffsets(outRect, position, itemCount)
        }
    }

    private fun getHorizontalItemOffsets(outRect: Rect, position: Int, itemCount: Int) {
        when (position) {
            0 -> outRect.set(dividerSizeInPixels, 0, dividerSizeInPixels/2, 0)
            itemCount - 1 -> outRect.set(dividerSizeInPixels/2, 0, dividerSizeInPixels, 0)
            else -> outRect.set(dividerSizeInPixels/2, 0, dividerSizeInPixels/2, 0)
        }
    }

    private fun getVerticalItemOffsets(outRect: Rect, position: Int, itemCount: Int) {
        when (position) {
            0 -> outRect.set(0, dividerSizeInPixels, 0, dividerSizeInPixels/2)
            itemCount - 1 -> outRect.set(0, dividerSizeInPixels/2, 0, dividerSizeInPixels)
            else -> outRect.set(0, dividerSizeInPixels/2, 0, dividerSizeInPixels/2)
        }
    }
}