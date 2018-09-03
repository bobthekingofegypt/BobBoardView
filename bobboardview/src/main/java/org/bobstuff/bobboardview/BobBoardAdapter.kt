package org.bobstuff.bobboardview

import android.os.Bundle
import android.os.Parcelable
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import android.view.View

/**
 * Abstract superclass to be extended by your custom adapters.  This doesn't provide much functionality
 * other than enforcing a couple of conventions and using generics to try make the use of casting a little
 * easier.  Not fully sure what bounderies should exist between framework and user yet so this is
 * a starting point.
 *
 * Created by bob
 */
abstract class BobBoardAdapter<T : BobBoardAdapter.ListViewHolder<*>>
@JvmOverloads constructor(
        @field:Transient private val debugLoggingEnabled: Boolean = true
) : RecyclerView.Adapter<T>() {
    init {
        setHasStableIds(true)
    }
    /**
     * Access to boardview if this adapter has been attached to a boardview, can be null when not
     * currently attatched to any boardview
     */
    var boardView: BobBoardView? = null

    /**
     * flag to indicate that the next view added to the window was triggered because of a user drag.
     * Currently I'm assuming the serial execution of events means there shouldn't be another view
     * added between this flag being set and the insertion completing.  Hopefully I am right.
     */
    private var mAddedDuringDrag: Boolean = false

    var isListAddedDuringDrag: Boolean
        get() = mAddedDuringDrag
        set(b) {
            if (debugLoggingEnabled) {
                Log.d(BobBoardAdapter.TAG, "setListAddedDuringDrag() | previous: $mAddedDuringDrag; new: $b")
            }
            mAddedDuringDrag = b
        }

    var addedListId: Long = -1

    open fun onSaveInstanceState(): Parcelable? {
        return null
    }

    open fun onRestoreInstanceState(bundle: Bundle) {

    }

    /**
     * Override this method if you want to handle custom changes to the added view, ex. make it
     * invisible.  Just remember to call super
     */
    override fun onViewAttachedToWindow(viewHolder: T) {
        if (debugLoggingEnabled) {
            Log.d(BobBoardAdapter.TAG, "onViewAttachedToWindow() | mAddedDuringDrag: " + mAddedDuringDrag
                    + "; adapter position: " + viewHolder.adapterPosition)
        }
        Log.d("TEST", "addedlistid: $addedListId, viewholder itemid: ${viewHolder.itemId}")
        if (mAddedDuringDrag && addedListId == viewHolder.itemId) {
            mAddedDuringDrag = false
            addedListId = -1
            boardView?.switchListDrag(viewHolder)
        }
    }

    /**
     * Contract for the ListViewHolder to comply with, a "list" in this terminology is a column in the
     * board.
     */
    abstract class ListViewHolder<out T : BobBoardListAdapter<out BobBoardListAdapter.CardViewHolder>>(view: View) : RecyclerView.ViewHolder(view) {
        /**
         * @return the recycler that contains the rows of this board list
         */
        abstract val recyclerView: RecyclerView?
        /**
         * @return the listadapter for the rows of this board list
         */
        abstract val listAdapter: T?
    }

    open fun onAttachedToBoardView(bobBoardView: BobBoardView) {
        this.boardView = bobBoardView
    }

    open fun onDetachedFromBoardView(bobBoardView: BobBoardView) {
        this.boardView = null
    }

    companion object {
        private const val TAG = "BobBoardAdapter"
    }
}
