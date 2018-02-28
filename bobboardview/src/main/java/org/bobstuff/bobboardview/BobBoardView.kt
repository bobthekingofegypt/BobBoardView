package org.bobstuff.bobboardview

import android.content.Context
import android.graphics.PointF
import android.graphics.Rect
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.util.Log
import android.view.DragEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import org.bobstuff.bobboardview.BobBoardAdapter.ListViewHolder

/**
 * Created by bob on 29/12/17.
 */

class BobBoardView : FrameLayout {
    lateinit var listRecyclerView: RecyclerView
        private set
    private lateinit var mBobBoardScroller: BobBoardScroller
    private lateinit var mItemTouchHelper: BobBoardItemTouchHelper
    private var mBoardViewListener: BobBoardViewListener? = null
    private var mBoardAdapater: BobBoardAdapter<out ListViewHolder<*>>? = null
    private val tempRect = Rect()
    private var debugEnabled: Boolean = true
    private var listExitCallbackCalled: Boolean = false
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

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    fun setBoardViewListener(boardViewListener: BobBoardViewListener) {
        this.mBoardViewListener = boardViewListener
    }

    fun setBoardAdapter(boardAdapter: BobBoardAdapter<out ListViewHolder<*>>) {
        if (this.mBoardAdapater !== boardAdapter) {
            mBoardAdapater?.onDetachedFromBoardView(this)
        }
        this.mBoardAdapater = boardAdapter
        listRecyclerView.adapter = boardAdapter
        boardAdapter.onAttachedToBoardView(this)
    }

    private fun init() {
        listRecyclerView = RecyclerView(context)
        listRecyclerView.setHasFixedSize(true)
        listRecyclerView.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)
        val llm = LinearLayoutManager(context,
                LinearLayoutManager.HORIZONTAL, false)
        listRecyclerView.layoutManager = llm

        listRecyclerView.setOnDragListener(OnDragListener { _, event ->
            val action = event.action
            val x = event.x
            val y = event.y

            when (action) {
                DragEvent.ACTION_DRAG_STARTED -> return@OnDragListener true
                DragEvent.ACTION_DRAG_ENTERED -> return@OnDragListener true
                DragEvent.ACTION_DRAG_LOCATION -> {
                    mLastDragPoint.set(x, y)
                    onUpdateDrag(x, y)
                    if (currentDragType == DragType.CARD) {
                        //if moving a card the item touch helper only scrolls vertically inside itself
                        //so we use the autoscrolled to move the main list recycler if needed based on
                        //the drag location
                        mBobBoardScroller.updateDrag(x, y)
                    }
                    return@OnDragListener false
                }
                DragEvent.ACTION_DRAG_EXITED -> return@OnDragListener true
                DragEvent.ACTION_DROP -> return@OnDragListener true
                DragEvent.ACTION_DRAG_ENDED -> {
                    if (currentDragType == DragType.CARD) {
                        //TODO probably shouldn't assume this is what everyone would want, add to callbacks?
                        val listItemOnTopOf = listRecyclerView.findChildViewUnder(mLastDragPoint.x, mLastDragPoint.y)
                        val listIndex = listRecyclerView.getChildAdapterPosition(listItemOnTopOf)
                        //TODO definetly dont want this
                        //listRecyclerView.smoothScrollToPosition(listIndex)
                    }
                    stopDrag()
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
                        Log.d("TEST", "TESTING!!!!")
                        onUpdateDrag(mLastDragPoint.x, mLastDragPoint.y)
                    }
                })

        this.addView(listRecyclerView)
    }

    private fun callCardDragExitedList() {
        mBoardViewListener?.onCardDragExitedList(currentListViewHolder!!, mItemTouchHelper.selected as BobBoardListAdapter.CardViewHolder)
        listExitCallbackCalled = true
        mItemTouchHelper.attachToRecyclerView(null)
        currentListIndex = NO_LIST_ACTIVE
        currentListViewHolder?.setIsRecyclable(true)
        currentListViewHolder = null
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
        //this can return null when we are on the item decoration for the recyclerview because at
        //that point we are between entries
        val listItemViewUnder = listRecyclerView.findChildViewUnder(x, y)
        if (listItemViewUnder == null && !listExitCallbackCalled) {
            callCardDragExitedList()
            return
        } else if (listItemViewUnder == null) {
            return
        }

        val listIndex = listRecyclerView.getChildAdapterPosition(listItemViewUnder)
        val holder = listRecyclerView.getChildViewHolder(listItemViewUnder) as ListViewHolder<*>
        //if we have left our old list view and reached here without touching any deadzone therefor
        //not informing our listeners that the card has left its old list we need to trigger the call now
        if (listIndex != currentListIndex && !listExitCallbackCalled) {
            callCardDragExitedList()
        }

        val cardRecyclerView = holder.recyclerView
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
            if (!listExitCallbackCalled) {
                callCardDragExitedList()
            }
            return
        }

        if (currentListIndex == NO_LIST_ACTIVE) {
            if (debugEnabled) {
                Log.d(TAG, "onUpdateDrag()| currentListIndex was NO_LIST_ACTIVE but we are on " +
                        "top of a list, scanning for destination index")
            }
            Log.d("TEST", "x: ${x - cardRecyclerViewDrawingRectParent.left}, y: ${y - cardRecyclerViewDrawingRectParent.top}")
            val cardViewUnder = cardRecyclerView.findChildViewUnder(x - cardRecyclerViewDrawingRectParent.left, y - cardRecyclerViewDrawingRectParent.top)

            var destinationIndex = cardRecyclerView.getChildAdapterPosition(cardViewUnder)

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
                        Log.d(TAG, "onUpdateDrag()| destinationIndex was -1 on view check, scanning all children for bounds collision")
                    }
                    destinationIndex = 0
                } else {
                    val itemCount = cardRecyclerView.adapter.itemCount
                    //lets loop over all the visible children and see which items bounds contains the touch
                    // point
                    val lm = cardRecyclerView.layoutManager
                    for (i in 0 until childCount) {
                        val child = cardRecyclerView.getChildAt(i)
                        lm.calculateItemDecorationsForChild(child, tempRect)
                        val vlp = child.layoutParams as MarginLayoutParams
                        val top = child.top - tempRect.top - vlp.topMargin
                        val bottom = child.top + child.height + tempRect.bottom + vlp.bottomMargin
                        val left = child.left - tempRect.left - vlp.leftMargin
                        val right = child.left + tempRect.right + child.width + vlp.rightMargin

                        tempRect.set(left, top, right, bottom)
                        val contains = tempRect.contains((x - cardRecyclerViewDrawingRectParent.left).toInt(), (y - cardRecyclerViewDrawingRectParent.top).toInt())
                        if (debugEnabled) Log.d(TAG, "onUpdateDrag()| scanned child rect: $tempRect; touch point: (${(x - cardRecyclerViewDrawingRectParent.left).toInt()}, ${(y - cardRecyclerViewDrawingRectParent.top).toInt()}); contains: $contains")
                        if (contains) {
                            destinationIndex = cardRecyclerView.getChildAdapterPosition(child)
                            //this is a special case, if we come in from the very bottom unlike normal
                            //we don't want to insert the card inplace of the previous one we want to
                            //put it after the last one when we are the last child
                            if (destinationIndex == itemCount -1 &&
                                    (y.toInt() - cardRecyclerViewDrawingRectParent.top - top) > ((bottom - top) / 2)) {
                                destinationIndex += 1
                            }
                            break
                        }
                    }

                    if (destinationIndex == -1) {
                        //TODO in theory this should be impossible now we adjust for margins on cards
                        destinationIndex = cardRecyclerView.adapter.itemCount
                    }
                }
            }

            currentListViewHolder = holder
            currentListViewHolder?.setIsRecyclable(false)
            currentListIndex = listIndex
            mBoardViewListener?.onCardMoveList(holder, destinationIndex)
        } else {
            mItemTouchHelper.drag(x - cardRecyclerViewDrawingRectParent.left, y - cardRecyclerViewDrawingRectParent.top)
        }

    }

    fun startListDrag(viewHolder: ListViewHolder<BobBoardListAdapter<*>>, x: Float, y: Float) {
        currentDragType = DragType.LIST
        mItemTouchHelper!!.attachToRecyclerView(listRecyclerView)
        mItemTouchHelper!!.startDrag(viewHolder, x, y)

        currentListViewHolder = viewHolder
        currentListViewHolder?.setIsRecyclable(false)
        currentListIndex = currentListViewHolder?.adapterPosition!!

        if (mBoardViewListener != null) {
            mBoardViewListener?.onListDragStarted(viewHolder)
        }
    }

    fun startCardDrag(listViewHolder: ListViewHolder<BobBoardListAdapter<*>>,
                      cardViewHolder: BobBoardListAdapter.CardViewHolder,
                      x: Float, y: Float) {
        currentDragType = DragType.CARD
        currentListIndex = listViewHolder.adapterPosition
        mItemTouchHelper.attachToRecyclerView(listViewHolder.recyclerView)
        mItemTouchHelper.startDrag(cardViewHolder, x, y)
        currentListViewHolder = listViewHolder
        currentListViewHolder!!.setIsRecyclable(false)
        mBobBoardScroller.startTracking()

        if (mBoardViewListener != null) {
            mBoardViewListener?.onCardDragStarted(listViewHolder, cardViewHolder)
        }
    }

    fun switchCardDrag(recyclerView: RecyclerView,
                       cardViewHolder: BobBoardListAdapter.CardViewHolder) {
        if (currentDragType == DragType.NONE) {
            return
        }

        mBoardViewListener?.onCardDragEnteredList(currentListViewHolder!!, cardViewHolder)
        mItemTouchHelper.attachToRecyclerView(recyclerView)
        mItemTouchHelper.startDragManual(cardViewHolder)
        listExitCallbackCalled = false
    }

    private fun stopDrag() {
        Log.d("TEST", "stopDrag on bobboardview")
        if (currentDragType == DragType.CARD) {
            mBoardViewListener?.onCardDragEnded(currentListViewHolder, mItemTouchHelper.selected as BobBoardListAdapter.CardViewHolder)
        } else {
            mBoardViewListener?.onListDragEnded(mItemTouchHelper.selected as ListViewHolder<BobBoardListAdapter<*>>?)
        }

        mItemTouchHelper.endDrag()
        mItemTouchHelper.attachToRecyclerView(null)
        currentListViewHolder?.setIsRecyclable(true)
        currentListViewHolder = null
        listExitCallbackCalled = false
        currentListIndex = NO_LIST_ACTIVE
        currentDragType = DragType.NONE
        mBobBoardScroller.stopTracking()
    }

    private fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        if (currentDragType == DragType.CARD) {
            if (mBoardViewListener != null) {
                mBoardViewListener!!.onCardMove(currentListViewHolder!!, listRecyclerView.getChildAdapterPosition(currentListViewHolder!!.itemView), fromPosition, toPosition)
            }
        } else if (currentDragType == DragType.LIST) {
            if (mBoardViewListener != null) {
                mBoardViewListener!!.onListMove(fromPosition, toPosition)
            }
        }
        return true
    }

    enum class DragType {
        NONE, LIST, CARD
    }

    inner class SimpleItemTouchHelperCallback : BobBoardItemTouchHelper.Callback() {
        override fun onMove(recyclerView: RecyclerView, source: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            onItemMove(source.adapterPosition, target.adapterPosition)
            return true
        }
    }

    interface BobBoardViewListener {
        fun onListDragStarted(listViewHolder: ListViewHolder<BobBoardListAdapter<*>>)
        fun onCardDragStarted(listViewHolder: ListViewHolder<BobBoardListAdapter<*>>,
                              cardViewHolder: BobBoardListAdapter.CardViewHolder)
        fun onListDragEnded(listViewHolder: ListViewHolder<BobBoardListAdapter<*>>?)
        fun onCardDragEnded(listViewHolder: ListViewHolder<BobBoardListAdapter<*>>?,
                            cardViewHolder: BobBoardListAdapter.CardViewHolder?)
        fun onListMove(fromPosition: Int, toPosition: Int)
        fun onCardMove(listViewHolder: BobBoardAdapter.ListViewHolder<*>, listIndex: Int, fromPosition: Int, toPosition: Int)
        fun onCardMoveList(toViewHolder: BobBoardAdapter.ListViewHolder<*>, toIndex: Int)
        fun onCardDragExitedList(listViewHolder: ListViewHolder<BobBoardListAdapter<*>>, cardViewHolder: BobBoardListAdapter.CardViewHolder)
        fun onCardDragEnteredList(listViewHolder: ListViewHolder<BobBoardListAdapter<*>>, cardViewHolder: BobBoardListAdapter.CardViewHolder)
        //fun canDropOnList(listIndex: Int)
        //fun canDropOnItem(listIndex: Int, itemIndex: Int)
    }

    companion object {
        private const val NO_LIST_ACTIVE = -1
        private const val TAG = "BobBoardView"
    }
}
