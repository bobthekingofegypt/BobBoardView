package org.bobstuff.bobboardview

import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import android.view.View

/**
 * Abstract class for a ListAdapter in a board.  This is more than just a contract it contains some
 * default behaviour that is required for the BobBoardView instance to work.  If you chose to override
 * any of the default function implementation make sure to call the super implementations aswell.
 *
 * ----------------------------------
 * | column A | column B | column C |
 * | ------------------------------ |
 * | row 1a    | row 1b    | row 1c |
 * | row 2a    | row 2b    | row 2c |
 * | row 3a    | row 3b    | row 3c |
 * | row 4a    | row 4b    | row 4c |
 * |--------------------------------|
 *
 * isCardAddedDuringDrag logic:
 * ----------------------------
 * In a BoardView a list is the recyclerview that is part of a column and the ListAdapter is the
 * RecyclerView adapter that manages the rows.  Because a board view is in essence a RecyclerView of
 * recycler views and you can drag entries from one recyclerview to another we need some custom event
 * handling to understand when that happens.  Here we are using a flag isCardAddedDuringDrag and the
 * recyclerViews own callback to tell us when a new ViewHandler has been added to get access to the
 * view handler reference once it is onscreen.  So the steps will be:
 *  - subclass adds model to adapters data representation
 *  - subclass call setIsCardAddedDuringDrag
 *  - subclass notifies the adapter that the data has changed, notifyItemInserted if you want animation
 *  - recyclerview will work out how that needs to be drawn triggering the create and bind as necessary
 *    inserting the a new viewholder and adding it to the window before calling onViewAttachedToWindow
 *    to let us know that the view will be show
 *  - we use this call to check if it was a drag added call and if it was we tell the class registered
 *    for cardEventCallbacks (usually the DefaultCardEventCallbacks that has a reference to the BoardView
 *    through the board adapter, its hard to safely maintain a reference to the boardview here in the
 *    list adapters so I don't try).
 *
 * Created by bob
 */
abstract class BobBoardListAdapter<T : BobBoardListAdapter.CardViewHolder>
@JvmOverloads constructor(
        protected var cardEventCallbacks: CardEventCallbacks,
        @field:Transient private val debugLoggingEnabled: Boolean = true
) : RecyclerView.Adapter<T>() {
    init {
        setHasStableIds(true)
    }
    /**
     * flag to indicate that the next view added to the window was triggered because of a user drag.
     * Currently I'm assuming the serial execution of events means there shouldn't be another view
     * added between this flag being set and the insertion completing.  Hopefully I am right.
     */
    private var mAddedDuringDrag: Boolean = false

    var isCardAddedDuringDrag: Boolean
        get() = mAddedDuringDrag
        set(b) {
            if (debugLoggingEnabled) {
                Log.d(TAG, "setCardAddedDuringDrag() | previous: $mAddedDuringDrag; new: $b")
            }
            mAddedDuringDrag = b
        }

    var addedCardId: Long = -1

    /**
     * Override this method if you want to handle custom changes to the added view, ex. make it
     * invisible.  Just remember to call super
     */
    override fun onViewAttachedToWindow(viewHolder: T) {
        if (debugLoggingEnabled) {
            Log.d(TAG, "onViewAttachedToWindow() | mAddedDuringDrag: " + mAddedDuringDrag
                    + "; adapter position: " + viewHolder.adapterPosition)
        }
        if (mAddedDuringDrag && addedCardId == viewHolder.itemId) {
            mAddedDuringDrag = false
            cardEventCallbacks.cardMovedDuringDrag(viewHolder)
        }
    }

    /**
     * Interface for the card drag events, this is used to pass the events back down to the interested
     * listener.  If using DefaultCardEventCallbacks this is a way to inform the boardview of the events.
     * A custom implementation can be created if you want to trigger some non standard behaviour.
     */
    interface CardEventCallbacks {
        fun cardSelectedForDrag(viewHolder: BobBoardListAdapter.CardViewHolder, x: Float, y: Float)
        fun cardMovedDuringDrag(viewHolder: BobBoardListAdapter.CardViewHolder, callCardExitedListCallback: Boolean)
        fun cardMovedDuringDrag(viewHolder: BobBoardListAdapter.CardViewHolder)
    }

    class DefaultCardEventCallbacks(private val bobBoardAdapter: BobBoardAdapter<*>, private val listViewHolder: BobBoardAdapter.ListViewHolder<*>) : CardEventCallbacks {

        override fun cardSelectedForDrag(viewHolder: BobBoardListAdapter.CardViewHolder, x: Float, y: Float) {
            bobBoardAdapter.boardView!!.startCardDrag(listViewHolder, viewHolder, x, y)
        }

        override fun cardMovedDuringDrag(viewHolder: BobBoardListAdapter.CardViewHolder, callCardExitedListCallback: Boolean) {
            bobBoardAdapter.boardView!!.switchCardDrag(listViewHolder, viewHolder, callCardExitedListCallback)
        }

        override fun cardMovedDuringDrag(viewHolder: BobBoardListAdapter.CardViewHolder) {
            bobBoardAdapter.boardView!!.switchCardDrag(listViewHolder, viewHolder, true)
        }

    }

    abstract class CardViewHolder(view: View) : RecyclerView.ViewHolder(view)

    companion object {
        private const val TAG = "BobBoardListAdapter"
    }

}
