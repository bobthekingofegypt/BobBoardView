package org.bobstuff.bobboardview

import android.content.Context
import android.graphics.PointF
import android.graphics.Rect
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.OrientationHelper
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.util.Log
import android.view.DragEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import org.bobstuff.bobboardview.BobBoardAdapter.ListViewHolder

/**
 * BobBoardView is a custom view that attempts to make it easier to make "Board" style applications
 * that can drag their elements around.
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
 * Terminology is a bit confused, but
 *
 * List -  column and list are currently used alternatively with each other
 * to refer to the same thing. They refer to the columns in the above ascii diagram.
 *
 * Card - term used to represent a row, sadly card/story/row are used to refer to the same thing
 *
 * Currently the functionality here is deliberately limited, its only real job is to attempt to
 * orchestrate the touch helper onto the active lists and tell the listener about what is happening
 * with the drag point.  Tracking current drag operations and their meaning on the data is the
 * responsibility of the listener and or adapters.  This may change in the future but for now I have
 * gone for the most flexible solution I could and this design allows the listener and adapters to
 * build complex logic that wouldn't be possible if the functionality was limited to what the
 * boardview could do itself.
 *
 * Created by bob
 */

class BobBoardView : FrameLayout {
    /**
     * Horizontal Recycler view that is responsible for the lists on the board.  It is created and
     * managed by the boardview.  Public access is given for now to allow you to create item
     * decorations or interogate the recyclerview where you only have access to the boardview
     */
    lateinit var listRecyclerView: RecyclerView
        private set
    /**
     * Orientation helper to handle some size and position boilerplate answers
     */
    lateinit var listOrientationHelper: OrientationHelper
    /**
     * Custom scroller used to scroll the main list recyclerview when dragging cards around near the
     * edges
     */
    private lateinit var mBobBoardScroller: BobBoardScroller
    /**
     * Touch helper is a heavily modified version of the recyclerview ItemTouchHelper, boardview is
     * responsible for making sure it is attached to the correct recyclerview at all times, or no
     * recycler when it makes sense
     */
    private lateinit var mItemTouchHelper: BobBoardItemTouchHelper
    /**
     * BobBoardViewListener is the way to register for events around the boardview, it is how you
     * build any of the drag/drop style features in your app
     */
    private lateinit var boardViewListener: BobBoardViewListener
    /**
     * BobBoardAdapter is an abstract class that you need to extend to create your own adapter.  It
     * extends from RecyclerView.Adapter so you can use all normal adapter style code in it but you
     * have to add a couple of extra parts
     */
    var boardAdapter: BobBoardAdapter<out ListViewHolder<*>>? = null
        set(value) {
            if (field !== value) {
                field?.onDetachedFromBoardView(this)
            }
            field = value
            listRecyclerView.adapter = field
            field?.onAttachedToBoardView(this)
        }

    private val tempRect = Rect()
    private var debugEnabled: Boolean = true
    private var cardExitedListCallbackCalled: Boolean = false
    private var isDragging = false
    var currentDragType = DragType.NONE
        private set
    /**
     * Store the last updated drag point, we use this so when we autoscroll the main list view we
     * can retrigger the drag logic forcing a recheck for view overlaps that need swapped, this
     * happens when you drag a card near the edge of the screen.
     * Drag point is relative to the viewport not the actual backing scroll view, so it can just be
     * re-used in future calculations regardless of any autoscrolling.
     */
    private val mLastDragPoint = PointF()

    /**
     * Reference the list view holder that was under the drag event on the last run through the drag
     * listener.  This custom logic is required because drag listeners pre android7 don't like it when
     * you add views to the scene after a drag has begun so we need to do a lot of manual logic based
     * on the drag events generated from the main list itself not the entered and exited events
     */
    private var currentListViewHolder: ListViewHolder<BobBoardListAdapter<*>>? = null
    private var currentListIndex = NO_LIST_ACTIVE
    private var activeListViewHolder: ListViewHolder<BobBoardListAdapter<*>>? = null
    private var activeListIndex = NO_LIST_ACTIVE
    private var listViewReenteringBoardView: Boolean = false
    private var initialEntryEventProcessed: Boolean = false
    var enableDragEvents = true

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    /**
     * @param boardViewListener listener for boardview events
     */
    fun setBoardViewListener(boardViewListener: BobBoardViewListener) {
        this.boardViewListener = boardViewListener
    }

    private fun init() {
        listRecyclerView = RecyclerView(context)
        //temp fix for https://issuetracker.google.com/issues/74602131
        listRecyclerView.preserveFocusAfterLayout = false
        listRecyclerView.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)
        val llm = LinearLayoutManager(context,
                LinearLayoutManager.HORIZONTAL, false)
        listRecyclerView.layoutManager = llm
        listOrientationHelper = OrientationHelper.createHorizontalHelper(llm)

        listRecyclerView.setOnDragListener(OnDragListener { _, event ->
            val action = event.action
            val x = event.x
            val y = event.y

            if (!enableDragEvents && !(action == DragEvent.ACTION_DRAG_ENDED)) {
                return@OnDragListener true
            }

            when (action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    isDragging = true
                    return@OnDragListener true
                }
                DragEvent.ACTION_DRAG_ENTERED -> {
                    if (!initialEntryEventProcessed) {
                        initialEntryEventProcessed = true
                        return@OnDragListener true
                    }
                    if (currentDragType == DragType.LIST) {
                        listViewReenteringBoardView = true
                    }
                    return@OnDragListener true
                }
                DragEvent.ACTION_DRAG_LOCATION -> {
                    mLastDragPoint.set(x, y)

                    if (currentDragType == DragType.LIST && listViewReenteringBoardView) {
                        val reattach = notifyListenerListEnteredBoardView(x, y)
                        if (!reattach) {
                            return@OnDragListener true
                        }
                    }
                    onUpdateDrag(x, y)
                    if (currentDragType == DragType.CARD) {
                        //if moving a card the item touch helper only scrolls vertically inside itself
                        //so we use the autoscroller to move the main list recycler if needed based on
                        //the drag location
                        mBobBoardScroller.updateDrag(x, y)
                    }
                    return@OnDragListener false
                }
                DragEvent.ACTION_DRAG_EXITED -> {
                    if (listViewReenteringBoardView) {
                        //dont process another exit as we haven't processed the last enter yet
                        listViewReenteringBoardView = false
                        return@OnDragListener true
                    }
                    if (currentDragType == DragType.LIST) {
                        notifyListenerListExitedBoardView()
                    }
                    return@OnDragListener true
                }
                DragEvent.ACTION_DROP -> return@OnDragListener true
                DragEvent.ACTION_DRAG_ENDED -> {
                    Log.d("TEST", "!!!!!!!!!!!! called drag ended")
                    initialEntryEventProcessed = false
                    stopDrag()
                    isDragging = false
                    return@OnDragListener true
                }
                else -> Log.e("DragDrop Example", "Unknown action type received by OnDragListener.")
            }

            false
        })

        val maxScrollSpeed = context.resources.getDimensionPixelSize(R.dimen.bobboardview_max_scroll_distance_per_frame)

        mItemTouchHelper = BobBoardItemTouchHelper(maxScrollSpeed,
                SimpleItemTouchHelperCallback())

        mBobBoardScroller = BobBoardScroller(listRecyclerView, 10, maxScrollSpeed,
                object : BobBoardScroller.BobBoardScrollerCallback {
                    override fun onScrolled(accumulatedScroll: Int) {
                        onUpdateDrag(mLastDragPoint.x, mLastDragPoint.y)
                    }
                })

        this.addView(listRecyclerView)
    }

    private fun notifyListenerListEnteredBoardView(x: Float, y: Float): Boolean {
        listViewReenteringBoardView = false

        // reattached here means that the adapter inserted the drag view back into the list when it
        // endtered the board view again.  This is required to handle boards like trello clone
        // that remove and add list as they enter and exit the board view
        var destinationIndex = listAdapterPositionAtPoint(x, y)
        val reattached = boardViewListener.onListDragEnteredBoardView(this, currentListViewHolder, destinationIndex)
        if (reattached) {
            mItemTouchHelper.attachToRecyclerView(listRecyclerView)
        }
        return reattached
    }

    private fun notifyListenerListExitedBoardView() {
        // removed here means that the adapter deleted the original list when it left the confines
        // of the board view, if the users adapter did that we need to stop listening and let it
        // recycle
        val removed = boardViewListener.onListDragExitedBoardView(this, currentListViewHolder!!)
        if (removed) {
            currentListViewHolder?.setIsRecyclable(true)
            currentListViewHolder = null
            mItemTouchHelper.attachToRecyclerView(null)
            currentListIndex = NO_LIST_ACTIVE
        }
    }

    fun listAdapterPositionAtPoint(x: Float, y: Float): Int {
        var destinationIndex = -1
        val listItemViewUnder = listRecyclerView.findChildViewUnder(x, y)
        if (listItemViewUnder == null) {
            val itemCount = listRecyclerView.childCount
            val lm = listRecyclerView.layoutManager

            for (i in 0 until itemCount) {
                val child = listRecyclerView.getChildAt(i)

                lm.calculateItemDecorationsForChild(child, tempRect)
                val vlp = child.layoutParams as MarginLayoutParams
                val top = child.top - tempRect.top - vlp.topMargin
                val bottom = child.top + child.height + tempRect.bottom + vlp.bottomMargin
                val left = child.left - tempRect.left - vlp.leftMargin
                val right = child.left + tempRect.right + child.width + vlp.rightMargin

                tempRect.set(left, top, right, bottom)
                val contains = tempRect.contains(x.toInt(), y.toInt())

                if (debugEnabled) Log.d(TAG, "notifyListenerListEnteredBoardView()| scanned " +
                        "adapter position ${listRecyclerView.getChildAdapterPosition(listRecyclerView.getChildAt(i))}" +
                        "child rect: $tempRect; touch point: (($x, $y); contains: $contains")

                if (contains) {
                    // The view can actually be deleted but is still present and its adapter position
                    // is no position this can happen when the item we are
                    // on top of is actually the currently exiting animation view.
                    // we can check if there is a visible view before or after to get an adapter
                    // position and hope for the best.
                    destinationIndex = listRecyclerView.getChildAdapterPosition(child)

                    if (destinationIndex == -1 && i<childCount) {
                        val backupChild = listRecyclerView.getChildAt(i+1)
                        destinationIndex = listRecyclerView.getChildAdapterPosition(backupChild) - 1
                    }
                    if (destinationIndex == -1 && i != 0) {
                        val backupChild = listRecyclerView.getChildAt(i-1)
                        destinationIndex = listRecyclerView.getChildAdapterPosition(backupChild) + 1
                    }

                    break
                }
            }
        } else {
            destinationIndex = listRecyclerView.getChildAdapterPosition(listItemViewUnder)
        }

        return destinationIndex
    }

    fun listRemovedDuringDrag() {
        currentListViewHolder?.setIsRecyclable(true)
        currentListViewHolder = null
        mItemTouchHelper.attachToRecyclerView(null)
        currentListIndex = NO_LIST_ACTIVE
    }

    fun cardRemovedDuringDrag() {
        currentListViewHolder?.setIsRecyclable(true)
        currentListViewHolder = null
        mItemTouchHelper.attachToRecyclerView(null)
        currentListIndex = NO_LIST_ACTIVE
    }

    private fun notifyListenerCardExitedList() {
        if (debugEnabled) {
            Log.d(TAG, "notifyListenerCardExitedList()| drag child left active list adapter " +
                    "position ${activeListViewHolder?.adapterPosition}; stored index ${activeListIndex}; " +
                    "current adapter position ${currentListViewHolder?.adapterPosition}; stored index: " +
                    "$currentListIndex")
        }
        boardViewListener.onCardDragExitedList(this, activeListViewHolder!!,
                mItemTouchHelper.selected as BobBoardListAdapter.CardViewHolder)
        cardExitedListCallbackCalled = true
        mItemTouchHelper.attachToRecyclerView(null)
        activeListIndex = NO_LIST_ACTIVE
    }

    private fun recyclerChildAdjustBounds(child: View, lm: RecyclerView.LayoutManager, rect: Rect) {
        lm.calculateItemDecorationsForChild(child, tempRect)
        val vlp = child.layoutParams as MarginLayoutParams
        val top = child.top - tempRect.top - vlp.topMargin
        val bottom = child.top + child.height + tempRect.bottom + vlp.bottomMargin + 1
        val left = child.left - tempRect.left - vlp.leftMargin
        val right = child.left + tempRect.right + child.width + vlp.rightMargin + 1

        rect.set(left, top, right, bottom)
    }

    private fun onUpdateDrag(x: Float, y: Float) {
        //if we are dragging a list we don't need to worry about anything crazy, just tell the touch
        // helper the coordinates and let it work it out
        if (currentDragType == DragType.LIST) {
            mItemTouchHelper.drag(x, y)
            return
        }

        /*
        Now for the fun part.  Dragging.  Cards.  So this is where things get a little more convoluted
        as we need to coordinate the switching from tracking one recyclerview to tracking a different
        one based on what is under the touch point.  Since enter/exit events do not trigger properly
        on early android versions when views are added after the drag has started we need to work
        this out manually.

        assumptions: currentListIndex is always populated at the start, it will be set at the
                     time a card is selected and will be updated as we move.  It will be set to NO_LIST_ACTIVE
                     when not on top of any list view (ie in the deadzone/item decoration)
         */
        assert(currentListIndex != NO_LIST_ACTIVE) { "current list index must exist during drag" }

        val listItemViewUnder = listRecyclerView.findChildViewUnder(x, y)
        if (listItemViewUnder == null && !cardExitedListCallbackCalled) {
            notifyListenerCardExitedList()
            return
        } else if (listItemViewUnder == null) {
            return
        }

        val listIndex = listRecyclerView.getChildAdapterPosition(listItemViewUnder)
        val holder = listRecyclerView.getChildViewHolder(listItemViewUnder) as ListViewHolder<*>
        //if we have left our old list view and reached here without touching any deadzone therefor
        //not informing our listeners that the card has left its old list we need to trigger the call now
        if (listIndex != activeListIndex && !cardExitedListCallbackCalled) {
            notifyListenerCardExitedList()
        }

        if (holder.recyclerView == null) {
            //TODO this is a bit stupid, you can't drop in a column without a recyclerview so
            // shouldn't really call this
            boardViewListener.canCardDropInList(this, holder)
            boardViewListener.onCardDragEnteredList(this, currentListViewHolder, holder, null, -1)
            //currentListViewHolder?.setIsRecyclable(true)
            //currentListViewHolder = holder
            //currentListViewHolder?.setIsRecyclable(false)
            //currentListIndex = listIndex
            activeListIndex = listIndex
            activeListViewHolder?.setIsRecyclable(true)
            activeListViewHolder = holder
            activeListViewHolder?.setIsRecyclable(false)
            cardExitedListCallbackCalled = false
            return
        }

        val cardRecyclerView = holder.recyclerView!!
        val cardRecyclerViewDrawingRect = Rect()
        cardRecyclerView.getDrawingRect(cardRecyclerViewDrawingRect)
        val cardRecyclerViewDrawingRectParent = Rect(cardRecyclerViewDrawingRect)
        (holder.itemView as ViewGroup).offsetDescendantRectToMyCoords(cardRecyclerView, cardRecyclerViewDrawingRect)
        listRecyclerView.offsetDescendantRectToMyCoords(cardRecyclerView, cardRecyclerViewDrawingRectParent)

        //Because the user can pad or display their list recycler however they want inside the list view layout we
        //need to ignore a touch if it outside the card recyclers bounds but not if it is inside but on item decoration
        if (x < cardRecyclerViewDrawingRectParent.left || y < cardRecyclerViewDrawingRectParent.top ||
                x > cardRecyclerViewDrawingRectParent.right || y > cardRecyclerViewDrawingRectParent.bottom) {
            //here we are outside the card recycler but we are inside a list view, incase we jumped
            //here in one frame we need to call the callback to tell it we have moved
            if (!cardExitedListCallbackCalled) {
                notifyListenerCardExitedList()
            }
            return
        }

        if (activeListIndex == NO_LIST_ACTIVE) {
            if (debugEnabled) {
                Log.d(TAG, "onUpdateDrag()| currentListIndex was NO_LIST_ACTIVE but we are on " +
                        "top of a list, scanning for destination index")
            }
            var cardViewUnder = cardRecyclerView.findChildViewUnder(x - cardRecyclerViewDrawingRectParent.left,
                    y - cardRecyclerViewDrawingRectParent.top)

            var destinationIndex = cardRecyclerView.getChildAdapterPosition(cardViewUnder)
            val itemCount = cardRecyclerView.adapter.itemCount
            val lm = cardRecyclerView.layoutManager

            if (destinationIndex == -1) {
                if (debugEnabled) {
                    Log.d(TAG, "onUpdateDrag()| destinationIndex was -1 on view check, scanning all children for bounds collision")
                }
                //if cardViewUnderIndex is -1 we may just be sitting on item decoration but are in
                // card recyclerview
                val childCount = cardRecyclerView.childCount
                //shortcut if the list is empty
                if (childCount == 0) {
                    if (debugEnabled) {
                        Log.d(TAG, "onUpdateDrag()| destination index set to 0 as no children found")
                    }
                    destinationIndex = 0
                } else {
                    //lets loop over all the visible children and see which items bounds contains the touch
                    // point
                    for (i in 0 until childCount) {
                        val child = cardRecyclerView.getChildAt(i)
                        recyclerChildAdjustBounds(child, lm, tempRect)

                        val contains = tempRect.contains((x - cardRecyclerViewDrawingRectParent.left).toInt(),
                                (y - cardRecyclerViewDrawingRectParent.top).toInt())
                        if (debugEnabled) {
                            Log.d(TAG, "onUpdateDrag()| scanned child rect: $tempRect; touch point: " +
                                    "(${(x - cardRecyclerViewDrawingRectParent.left).toInt()}, " +
                                    "${(y - cardRecyclerViewDrawingRectParent.top).toInt()}); " +
                                    "contains: $contains; " +
                                    "adapter position: ${cardRecyclerView.getChildAdapterPosition(child)}")
                        }
                        if (contains) {
                            destinationIndex = cardRecyclerView.getChildAdapterPosition(child)
                            if (destinationIndex < 0 && i<childCount) {
                                val backupChild = cardRecyclerView.getChildAt(i+1)
                                destinationIndex = cardRecyclerView.getChildAdapterPosition(backupChild) - 1
                                Log.d(TAG, "onUpdateDrag()|destination index adjusted after forward one item - $destinationIndex")
                            }
                            if (destinationIndex < 0 && i != 0) {
                                val backupChild = cardRecyclerView.getChildAt(i-1)
                                destinationIndex = cardRecyclerView.getChildAdapterPosition(backupChild) + 1
                                Log.d(TAG, "onUpdateDrag()|destination index adjusted after backtracking one item - $destinationIndex")
                            }
                            //this is a special case, if we come in from the very bottom unlike normal
                            //we don't want to insert the card inplace of the previous one we want to
                            //put it after the last one when we are the last child
                            //TODO combine list logic with main list
                            if (destinationIndex == itemCount - 1 &&
                                    (y.toInt() - cardRecyclerViewDrawingRectParent.top - top) > ((bottom - top) / 2)) {
                                destinationIndex += 1
                            }
                            cardViewUnder = child

                            //This can happen if a recycler is still animating all entries, its best
                            //to try wait out a few iterations until we get a valid value
                            if (destinationIndex < 0) {
                                return
                            }
                            break
                        }
                    }

                    Log.d(TAG, "destination index is ${destinationIndex}")

                    if (destinationIndex < 0) {
                        //TODO in theory this should be impossible now we adjust for margins on cards
                        destinationIndex = cardRecyclerView.adapter.itemCount
                    }
                }
            } else if (destinationIndex == itemCount - 1) {
                //destination index is the last item so we may be coming in from the bottom
                recyclerChildAdjustBounds(cardViewUnder, lm, tempRect)
                if ((y.toInt() - cardRecyclerViewDrawingRectParent.top - tempRect.top) >
                        ((tempRect.bottom - tempRect.top) / 2)) {
                    destinationIndex += 1
                }
            }

            val cardViewHolder: BobBoardListAdapter.CardViewHolder? = if (cardViewUnder != null) {
                cardRecyclerView.getChildViewHolder(cardViewUnder) as BobBoardListAdapter.CardViewHolder
            } else null
            val canCardDropInList = boardViewListener.canCardDropInList(this, holder)
            boardViewListener.onCardDragEnteredList(this, currentListViewHolder, holder, cardViewHolder, destinationIndex)
            if (canCardDropInList) {
                currentListViewHolder?.setIsRecyclable(true)
                currentListViewHolder = holder
                currentListViewHolder?.setIsRecyclable(false)
                currentListIndex = listIndex
                mItemTouchHelper.attachToRecyclerView(holder.recyclerView)
            }
            activeListIndex = listIndex
            activeListViewHolder?.setIsRecyclable(true)
            activeListViewHolder = holder
            activeListViewHolder?.setIsRecyclable(false)
            cardExitedListCallbackCalled = false
        } else {
            mItemTouchHelper.drag(x - cardRecyclerViewDrawingRectParent.left,
                    y - cardRecyclerViewDrawingRectParent.top)
        }
    }

    fun startListDrag(viewHolder: ListViewHolder<BobBoardListAdapter<*>>, x: Float, y: Float) {
        currentDragType = DragType.LIST
        mItemTouchHelper.attachToRecyclerView(listRecyclerView)
        mItemTouchHelper.startDrag(viewHolder, x, y)

        currentListViewHolder = viewHolder
        currentListViewHolder?.setIsRecyclable(false)
        currentListIndex = currentListViewHolder?.adapterPosition!!

        boardViewListener.onListDragStarted(this, viewHolder)
    }

    fun startCardDrag(listViewHolder: ListViewHolder<BobBoardListAdapter<*>>,
                      cardViewHolder: BobBoardListAdapter.CardViewHolder,
                      x: Float, y: Float) {
        currentDragType = DragType.CARD
        currentListIndex = listViewHolder.adapterPosition
        activeListIndex = listViewHolder.adapterPosition
        activeListViewHolder = listViewHolder
        activeListViewHolder!!.setIsRecyclable(false)
        mItemTouchHelper.attachToRecyclerView(listViewHolder.recyclerView)
        mItemTouchHelper.startDrag(cardViewHolder, x, y)
        currentListViewHolder = listViewHolder
        currentListViewHolder!!.setIsRecyclable(false)
        mBobBoardScroller.startTracking()

        boardViewListener.onCardDragStarted(this, listViewHolder, cardViewHolder)
    }

    /**
     * Function to switch the list view holder that is being tracked, this is required if you are
     * going to remove and readd the dragged list in the recyclerview.  This should be called by
     * your adapter implementation when the newly added viewholder is created by the recyclerview
     */
    fun switchListDrag(listViewHolder: ListViewHolder<BobBoardListAdapter<*>>) {
        if (currentDragType == DragType.NONE) {
            return
        }

        currentListViewHolder = listViewHolder
        currentListViewHolder?.setIsRecyclable(false)
        mItemTouchHelper.attachToRecyclerView(listRecyclerView)
        mItemTouchHelper.startDragManual(listViewHolder)
    }

    /**
     * Function to switch the card view holder that is being dragged after it is removed and added
     * to a new list, unlike with list dragging where switching logic is optional this call must
     * be made by your adapter as a card is dragged around it needs to switch lists.  This is something
     * I may remove in the future if I can, but it is a required manual step to support all the
     * events generated to create the custom UI features
     */
    fun switchCardDrag(listViewHolder: ListViewHolder<BobBoardListAdapter<*>>,
                       cardViewHolder: BobBoardListAdapter.CardViewHolder, callCardExitedListCallback: Boolean) {
        if (currentDragType == DragType.NONE) {
            return
        }
        mItemTouchHelper.attachToRecyclerView(listViewHolder.recyclerView)
        mItemTouchHelper.startDragManual(cardViewHolder)
        currentListViewHolder?.setIsRecyclable(true)
        currentListViewHolder = listViewHolder
        currentListViewHolder?.setIsRecyclable(false)

        if (callCardExitedListCallback)
            cardExitedListCallbackCalled = false
    }

    private fun stopDrag() {
        if (currentDragType == DragType.CARD) {
            boardViewListener.onCardDragEnded(this, activeListViewHolder, currentListViewHolder,
                    mItemTouchHelper.selected as BobBoardListAdapter.CardViewHolder)
        } else {
            boardViewListener.onListDragEnded(this, mItemTouchHelper.selected as ListViewHolder<BobBoardListAdapter<*>>?)
        }

        mItemTouchHelper.endDrag()
        mItemTouchHelper.attachToRecyclerView(null)
        currentListViewHolder?.setIsRecyclable(true)
        currentListViewHolder = null
        cardExitedListCallbackCalled = false
        currentListIndex = NO_LIST_ACTIVE
        activeListViewHolder?.setIsRecyclable(true)
        activeListViewHolder = null
        activeListIndex = NO_LIST_ACTIVE
        currentDragType = DragType.NONE
        mBobBoardScroller.stopTracking()
    }

    private fun onItemMove(fromPosition: Int, toPosition: Int) {
        if (currentDragType == DragType.CARD) {
            boardViewListener.onCardMove(this, currentListViewHolder!!,
                    listRecyclerView.getChildAdapterPosition(currentListViewHolder!!.itemView),
                    fromPosition, toPosition)
        } else if (currentDragType == DragType.LIST) {
            boardViewListener.onListMove(this, fromPosition, toPosition)
        }
    }

    enum class DragType {
        NONE, LIST, CARD
    }

    inner class SimpleItemTouchHelperCallback : BobBoardItemTouchHelper.Callback() {
        override fun onMove(recyclerView: RecyclerView, source: RecyclerView.ViewHolder,
                            target: RecyclerView.ViewHolder): Boolean {
            val manager = recyclerView.layoutManager as LinearLayoutManager
            val firstPos = manager.findFirstCompletelyVisibleItemPosition()
            var offsetTop = 0;
            if(firstPos >= 0) {
                val firstView = manager.findViewByPosition(firstPos)
                offsetTop = manager.getDecoratedTop(firstView) - manager.getTopDecorationHeight(firstView)
            }

            onItemMove(source.adapterPosition, target.adapterPosition)

            if(firstPos >= 0) {
                manager.scrollToPositionWithOffset(firstPos, offsetTop)
            }
            return true
        }

        override fun canDropOver(recyclerView: RecyclerView, current: RecyclerView.ViewHolder,
                                 target: RecyclerView.ViewHolder): Boolean {
            if (currentDragType == DragType.LIST) {
                return boardViewListener.canListDropOver(this@BobBoardView, current as ListViewHolder<BobBoardListAdapter<*>>,
                        target as ListViewHolder<BobBoardListAdapter<*>>)
            }
            return true
        }
    }

    interface BobBoardViewListener {
        fun onListDragStarted(boardView: BobBoardView, listViewHolder: ListViewHolder<BobBoardListAdapter<*>>)
        fun onCardDragStarted(boardView: BobBoardView, listViewHolder: ListViewHolder<BobBoardListAdapter<*>>,
                              cardViewHolder: BobBoardListAdapter.CardViewHolder)
        fun onListDragEnded(boardView: BobBoardView, listViewHolder: ListViewHolder<BobBoardListAdapter<*>>?)
        fun onCardDragEnded(boardView: BobBoardView, activeListViewHolder: ListViewHolder<BobBoardListAdapter<*>>?, listViewHolder: ListViewHolder<BobBoardListAdapter<*>>?,
                            cardViewHolder: BobBoardListAdapter.CardViewHolder?)
        fun onListMove(boardView: BobBoardView, fromPosition: Int, toPosition: Int)
        fun onCardMove(boardView: BobBoardView, listViewHolder: BobBoardAdapter.ListViewHolder<*>, listIndex: Int, fromPosition: Int, toPosition: Int)
        fun onCardDragExitedList(boardView: BobBoardView, listViewHolder: ListViewHolder<BobBoardListAdapter<*>>, cardViewHolder: BobBoardListAdapter.CardViewHolder)
        fun canCardDropInList(boardView: BobBoardView, listViewHolder: ListViewHolder<BobBoardListAdapter<*>>): Boolean
        fun canListDropOver(boardView: BobBoardView, listViewHolder: ListViewHolder<BobBoardListAdapter<*>>, otherListViewHolder: ListViewHolder<BobBoardListAdapter<*>>): Boolean

        fun onCardDragEnteredList(boardView: BobBoardView, previousListViewHolder: ListViewHolder<BobBoardListAdapter<*>>?, listViewHolder: ListViewHolder<BobBoardListAdapter<*>>, cardViewHolder: BobBoardListAdapter.CardViewHolder?, toIndex: Int)
        fun onListDragExitedBoardView(boardView: BobBoardView, listViewHolder: ListViewHolder<BobBoardListAdapter<*>>): Boolean
        fun onListDragEnteredBoardView(boardView: BobBoardView, listViewHolder: ListViewHolder<BobBoardListAdapter<*>>?, overIndex: Int): Boolean
    }

    companion object {
        private const val NO_LIST_ACTIVE = -1
        private const val TAG = "BobBoardView"
    }
}
