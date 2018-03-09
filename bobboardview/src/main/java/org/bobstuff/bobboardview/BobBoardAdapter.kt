package org.bobstuff.bobboardview

import android.support.v7.widget.RecyclerView
import android.view.View

/**
 * Abstract superclass to be extended by your custom adapters.  This doesn't provide much functionality
 * other than enforcing a couple of conventions and using generics to try make the use of casting a little
 * easier.  Not fully sure what bounderies should exist between framework and user yet so this is
 * a starting point.
 *
 * Created by bob
 */
abstract class BobBoardAdapter<T : BobBoardAdapter.ListViewHolder<*>> : RecyclerView.Adapter<T>() {
    /**
     * Access to boardview if this adapter has been attached to a boardview, can be null when not
     * currently attatched to any boardview
     */
    var boardView: BobBoardView? = null

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
}
