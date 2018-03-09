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
import android.widget.LinearLayout
import org.bobstuff.bobboardview.BobBoardAdapter.ListViewHolder

/**
 * Created by bob
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
    private var activeListIndex = NO_LIST_ACTIVE
    private var reenteringBoardView: Boolean = false
    private var initialEntryEventProcessed: Boolean = false
    var disableDragEvents: Boolean = false
        set(value) {
            if (value) {
                mItemTouchHelper.attachToRecyclerView(null)
            }
            field = value
        }

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
        //listRecyclerView.setHasFixedSize(true)
        listRecyclerView.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)
        val llm = LinearLayoutManager(context,
                LinearLayoutManager.HORIZONTAL, false)
        listRecyclerView.layoutManager = llm


        listRecyclerView.setOnDragListener(OnDragListener { _, event ->
            val action = event.action
            val x = event.x
            val y = event.y

            //Log.d("TEST", "(x,y): ($x, $y)")

            if (disableDragEvents) {
                return@OnDragListener true
            }

            when (action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    isDragging = true
                    return@OnDragListener true
                }
                DragEvent.ACTION_DRAG_ENTERED -> {
                    Log.d("TEST", "ACTION_DRAG_ENTERED")
                    if (!initialEntryEventProcessed) {
                        initialEntryEventProcessed = true
                        return@OnDragListener true
                    }
                    if (currentDragType == DragType.LIST) {
                        reenteringBoardView = true
                    }
                    return@OnDragListener true
                }
                DragEvent.ACTION_DRAG_LOCATION -> {
                    //Log.d("TEST", "ACTION_DRAG_LOCATION")
                    mLastDragPoint.set(x, y)

                    if (currentDragType == DragType.LIST && reenteringBoardView) {
                        val resume = notifyListenerListEnteredBoardView(x, y)
                        if (!resume) {
                            return@OnDragListener true
                        }
                    }
                    onUpdateDrag(x, y)
                    if (currentDragType == DragType.CARD) {
                        //if moving a card the item touch helper only scrolls vertically inside itself
                        //so we use the autoscrolled to move the main list recycler if needed based on
                        //the drag location
                        mBobBoardScroller.updateDrag(x, y)
                    }
                    return@OnDragListener false
                }
                DragEvent.ACTION_DRAG_EXITED -> {
                    Log.d("TEST", "ACTION_DRAG_EXITED")
                    if (reenteringBoardView) {
                        //dont process another exit as we haven't processed the last enter yet
                        reenteringBoardView = false
                        return@OnDragListener true
                    }
                    if (currentDragType == DragType.LIST) {
                        notifyListenerListExitedBoardView()
                    }
                    return@OnDragListener true
                }
                DragEvent.ACTION_DROP -> return@OnDragListener true
                DragEvent.ACTION_DRAG_ENDED -> {
                    Log.d("TEST", "ACTION_DRAG_DROP/ENDED")
                    initialEntryEventProcessed = false
                    if (currentDragType == DragType.CARD) {
                        //TODO probably shouldn't assume this is what everyone would want, add to callbacks?
                        val listItemOnTopOf = listRecyclerView.findChildViewUnder(mLastDragPoint.x, mLastDragPoint.y)
                        val listIndex = listRecyclerView.getChildAdapterPosition(listItemOnTopOf)
                        //TODO definetly dont want this
                        //listRecyclerView.smoothScrollToPosition(listIndex)
                    }
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
                        //Log.d("TEST", "TESTING!!!!")
                        onUpdateDrag(mLastDragPoint.x, mLastDragPoint.y)
                    }
                })

        this.addView(listRecyclerView)
    }

    private fun notifyListenerListEnteredBoardView(x: Float, y: Float): Boolean {
        if (currentDragType != DragType.LIST) {
            return false
        }

        reenteringBoardView = false
        var destinationIndex = -1
        val listItemViewUnder = listRecyclerView.findChildViewUnder(x, y)
        if (listItemViewUnder == null) {
            //we are definetly ontop of the list view, we got the notification after all lefts adjust
            //for margin/padding/itemdecor and find out where we are
            val itemCount = listRecyclerView.childCount
            val lm = listRecyclerView.layoutManager
            for (i in 0 until childCount) {
                val child = listRecyclerView.getChildAt(i)
                lm.calculateItemDecorationsForChild(child, tempRect)
                val vlp = child.layoutParams as MarginLayoutParams
                val top = child.top - tempRect.top - vlp.topMargin
                val bottom = child.top + child.height + tempRect.bottom + vlp.bottomMargin
                val left = child.left - tempRect.left - vlp.leftMargin
                val right = child.left + tempRect.right + child.width + vlp.rightMargin

                tempRect.set(left, top, right, bottom)
                val contains = tempRect.contains(x.toInt(), y.toInt())
                if (debugEnabled) Log.d(TAG, "notifyListenerListEnteredBoardView()| scanned child rect: $tempRect; touch point: (($x, $y); contains: $contains")
                Log.d("TEST", "scanned child adapter position: ${listRecyclerView.getChildAdapterPosition(listRecyclerView.getChildAt(i))}")
                if (contains) {
                    //THIS IS A FLAW, A BIG FLAW.  The view is actually deleted but it still present to its position
                    //is no position
                    //right this is a problem I need to solve better, this can happen when the item we are
                    //on top of is actually the currently exiting animation view.
                    //we can check if there is a visible view before or after to get an adapter position and hope for the best
                    destinationIndex = listRecyclerView.getChildAdapterPosition(child)
                    for (i in 0 until childCount) {
                        val c = listRecyclerView.getChildAt(i)
                        val vh = listRecyclerView.getChildViewHolder(child)
                        val lp = c.layoutParams as RecyclerView.LayoutParams
                        Log.d("TEST", "itemId: ${vh.itemId}")
                        Log.d("TEST", "isremoved: ${lp.isItemRemoved} invalid: ${lp.isViewInvalid} changed: ${lp.isItemChanged} ")
                        Log.d("TEST", "layoutposition: ${vh.layoutPosition} oldPosition: ${vh.oldPosition}")
                        Log.d("TEST", "scanned child adapter position: ${listRecyclerView.getChildAdapterPosition(listRecyclerView.getChildAt(i))}")
                    }
                    if (i != 0 && i<childCount) {
                        val backupChild = listRecyclerView.getChildAt(i+1)
                        destinationIndex = listRecyclerView.getChildAdapterPosition(backupChild) - 1
                    } else if (i != 0) {
                        val backupChild = listRecyclerView.getChildAt(i-1)
                        destinationIndex = listRecyclerView.getChildAdapterPosition(backupChild) + 1
                    }
                    //this is a special case, if we come in from the very bottom unlike normal
                    //we don't want to insert the card inplace of the previous one we want to
                    //put it after the last one when we are the last child
                    if (destinationIndex == itemCount -1 &&
                            (y.toInt() - top) > ((right - left) / 2)) {
                        destinationIndex += 1
                    }
                    break
                }
            }
        } else {
            destinationIndex = listRecyclerView.getChildAdapterPosition(listItemViewUnder)
        }
        val resume = mBoardViewListener?.onListDragEnteredBoardView(currentListViewHolder, destinationIndex)?: true
        if (resume) {
            mItemTouchHelper.attachToRecyclerView(listRecyclerView)
        }
        return resume
    }

    fun listAdapterPositionAtPoint(x: Float, y: Float): Int {
        var destinationIndex = -1
        val listItemViewUnder = listRecyclerView.findChildViewUnder(x, y)
        Log.d("TEST", "listItemViewUnder - $listItemViewUnder; (x,y): ($x, $y)")
        if (listItemViewUnder == null) {
            //we are definetly ontop of the list view, we got the notification after all lefts adjust
            //for margin/padding/itemdecor and find out where we are
            val itemCount = listRecyclerView.childCount
            val lm = listRecyclerView.layoutManager
            for (i in 0 until childCount) {
                val child = listRecyclerView.getChildAt(i)
                lm.calculateItemDecorationsForChild(child, tempRect)
                val vlp = child.layoutParams as MarginLayoutParams
                val top = child.top - tempRect.top - vlp.topMargin
                val bottom = child.top + child.height + tempRect.bottom + vlp.bottomMargin
                val left = child.left - tempRect.left - vlp.leftMargin
                val right = child.left + tempRect.right + child.width + vlp.rightMargin

                tempRect.set(left, top, right, bottom)
                val contains = tempRect.contains(x.toInt(), y.toInt())
                if (debugEnabled) Log.d(TAG, "notifyListenerListEnteredBoardView()| scanned child rect: $tempRect; touch point: (($x, $y); contains: $contains")
                Log.d("TEST", "scanned child adapter position: ${listRecyclerView.getChildAdapterPosition(listRecyclerView.getChildAt(i))}")
                if (contains) {
                    //THIS IS A FLAW, A BIG FLAW.  The view is actually deleted but it still present to its position
                    //is no position
                    //right this is a problem I need to solve better, this can happen when the item we are
                    //on top of is actually the currently exiting animation view.
                    //we can check if there is a visible view before or after to get an adapter position and hope for the best
                    destinationIndex = listRecyclerView.getChildAdapterPosition(child)
                    for (i in 0 until childCount) {
                        val c = listRecyclerView.getChildAt(i)
                        val vh = listRecyclerView.getChildViewHolder(child)
                        val lp = c.layoutParams as RecyclerView.LayoutParams
                        Log.d("TEST", "itemId: ${vh.itemId}")
                        Log.d("TEST", "isremoved: ${lp.isItemRemoved} invalid: ${lp.isViewInvalid} changed: ${lp.isItemChanged} ")
                        Log.d("TEST", "layoutposition: ${vh.layoutPosition} oldPosition: ${vh.oldPosition}")
                        Log.d("TEST", "scanned child adapter position: ${listRecyclerView.getChildAdapterPosition(listRecyclerView.getChildAt(i))}")
                    }
                    if (i != 0 && i<childCount) {
                        val backupChild = listRecyclerView.getChildAt(i+1)
                        destinationIndex = listRecyclerView.getChildAdapterPosition(backupChild) - 1
                    } else if (i != 0) {
                        val backupChild = listRecyclerView.getChildAt(i-1)
                        destinationIndex = listRecyclerView.getChildAdapterPosition(backupChild) + 1
                    }
                    //this is a special case, if we come in from the very bottom unlike normal
                    //we don't want to insert the card inplace of the previous one we want to
                    //put it after the last one when we are the last child
                    Log.d("TEST", "boardview - destinationIndex: $destinationIndex, itemCount: $itemCount, y: $y, top: $top, ")
                    //TODO this is wrong, horizontal vs vertical
                    if (destinationIndex == itemCount - 1 &&
                            (y.toInt() - top) > ((bottom - top) / 2)) {
                        Log.d("TEST", "DOES THIS HAPPEN???")
                        destinationIndex += 1
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

    private fun notifyListenerListExitedBoardView() {
        val boardViewListener = mBoardViewListener?: return

        val removed = boardViewListener.onListDragExitedBoardView(currentListViewHolder!!)
        if (removed) {
            currentListViewHolder?.setIsRecyclable(true)
            currentListViewHolder = null
            mItemTouchHelper.attachToRecyclerView(null)
            currentListIndex = NO_LIST_ACTIVE
        }
    }

    private fun callCardDragExitedList() {
        Log.d("TEST", "WTF is this being called?????????????????????????????")
        mBoardViewListener?.onCardDragExitedList(currentListViewHolder!!, mItemTouchHelper.selected as BobBoardListAdapter.CardViewHolder)
        listExitCallbackCalled = true
        //mItemTouchHelper.attachToRecyclerView(null)
        //currentListIndex = NO_LIST_ACTIVE
        activeListIndex = NO_LIST_ACTIVE
        //TODO we do not want to do this, if a view is in a recycler list we should keep that list alive
        //so the drag view is still visibile at all times when we return??????
        //currentListViewHolder?.setIsRecyclable(true)
        //currentListViewHolder = null
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
        //Log.d("TEST", "1")
        val listItemViewUnder = listRecyclerView.findChildViewUnder(x, y)
        if (listItemViewUnder == null && !listExitCallbackCalled) {
            Log.d("TEST", "2")
            callCardDragExitedList()
            return
        } else if (listItemViewUnder == null) {
            return
        }

        //Log.d("TEST", "3")
        val listIndex = listRecyclerView.getChildAdapterPosition(listItemViewUnder)
        val holder = listRecyclerView.getChildViewHolder(listItemViewUnder) as ListViewHolder<*>
        //if we have left our old list view and reached here without touching any deadzone therefor
        //not informing our listeners that the card has left its old list we need to trigger the call now
        Log.d("TEST", "listIndex - $listIndex; activeListIndex: $activeListIndex; currentListIndex: $currentListIndex")
        if (listIndex != activeListIndex && !listExitCallbackCalled) {
            callCardDragExitedList()
        }

        if (holder.recyclerView == null) {
            //TODO this is a bit stupid, you can't drop in a column without a recyclerview so shouldn't really call this
            val canCardDropInList = mBoardViewListener!!.canCardDropInList(holder)
            mBoardViewListener?.onCardDragEnteredList(currentListViewHolder, holder, -1)
            currentListViewHolder?.setIsRecyclable(true)
            currentListViewHolder = holder
            currentListViewHolder?.setIsRecyclable(false)
            currentListIndex = listIndex
            activeListIndex = listIndex
            listExitCallbackCalled = false
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
            if (!listExitCallbackCalled) {
                callCardDragExitedList()
            }
            return
        }

        //Log.d("TEST", "4")
        if (activeListIndex == NO_LIST_ACTIVE) {
            Log.d("TEST", "5")
            if (debugEnabled) {
                Log.d(TAG, "onUpdateDrag()| currentListIndex was NO_LIST_ACTIVE but we are on " +
                        "top of a list, scanning for destination index")
            }
            Log.d("TEST", "x: ${x - cardRecyclerViewDrawingRectParent.left}, y: ${y - cardRecyclerViewDrawingRectParent.top}")
            val cardViewUnder = cardRecyclerView.findChildViewUnder(x - cardRecyclerViewDrawingRectParent.left, y - cardRecyclerViewDrawingRectParent.top)

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
                        Log.d(TAG, "onUpdateDrag()| destinationIndex was -1 on view check, scanning all children for bounds collision")
                    }
                    destinationIndex = 0
                } else {
                    //lets loop over all the visible children and see which items bounds contains the touch
                    // point
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
                            if (destinationIndex == -1) {
                                //right this is a problem I need to solve better, this can happen when the item we are
                                //on top of is actually the currently exiting animation view.
                                //we can check if there is a visible view before or after to get an adapter position and hope for the best
                                if (i != 0 && i<childCount) {
                                    val backupChild = cardRecyclerView.getChildAt(i+1)
                                    destinationIndex = cardRecyclerView.getChildAdapterPosition(backupChild) - 1
                                } else if (i != 0) {
                                    val backupChild = cardRecyclerView.getChildAt(i-1)
                                    destinationIndex = cardRecyclerView.getChildAdapterPosition(backupChild) + 1
                                }
                            }
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
            } else if (destinationIndex == itemCount - 1) {
                //destination index is the last item so we may be coming in from the bottom
                lm.calculateItemDecorationsForChild(cardViewUnder, tempRect)
                val vlp = cardViewUnder.layoutParams as MarginLayoutParams
                val top = cardViewUnder.top - tempRect.top - vlp.topMargin
                val bottom = cardViewUnder.top + cardViewUnder.height + tempRect.bottom + vlp.bottomMargin
                val left = cardViewUnder.left - tempRect.left - vlp.leftMargin
                val right = cardViewUnder.left + tempRect.right + cardViewUnder.width + vlp.rightMargin
                if ((y.toInt() - cardRecyclerViewDrawingRectParent.top - top) > ((bottom - top) / 2)) {
                    destinationIndex += 1
                }
            }

            val canCardDropInList = mBoardViewListener!!.canCardDropInList(holder)
            mBoardViewListener?.onCardDragEnteredList(currentListViewHolder, holder, destinationIndex)
            currentListViewHolder?.setIsRecyclable(true)
            currentListViewHolder = holder
            currentListViewHolder?.setIsRecyclable(false)
            if (canCardDropInList) {
                currentListIndex = listIndex
            }
            activeListIndex = listIndex
            listExitCallbackCalled = false

            //mBoardViewListener?.onCardMoveList(holder, destinationIndex)
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
        activeListIndex = listViewHolder.adapterPosition
        mItemTouchHelper.attachToRecyclerView(listViewHolder.recyclerView)
        mItemTouchHelper.startDrag(cardViewHolder, x, y)
        currentListViewHolder = listViewHolder
        currentListViewHolder!!.setIsRecyclable(false)
        mBobBoardScroller.startTracking()

        if (mBoardViewListener != null) {
            mBoardViewListener?.onCardDragStarted(listViewHolder, cardViewHolder)
        }
    }

    fun switchListDrag(listViewHolder: ListViewHolder<BobBoardListAdapter<*>>) {
        if (currentDragType == DragType.NONE) {
            return
        }

        currentListViewHolder = listViewHolder
        currentListViewHolder?.setIsRecyclable(false)
        mItemTouchHelper.attachToRecyclerView(listRecyclerView)
        mItemTouchHelper.startDragManual(listViewHolder)
    }

    fun switchCardDrag(listViewHolder: ListViewHolder<BobBoardListAdapter<*>>,
                       cardViewHolder: BobBoardListAdapter.CardViewHolder, d: Boolean) {
        if (currentDragType == DragType.NONE) {
            return
        }
        Log.d("TEST", "IS THIS WHAT IS HAPPENING")
        //mBoardViewListener?.onCardDragEnteredList(currentListViewHolder!!, cardViewHolder)
        mItemTouchHelper.attachToRecyclerView(listViewHolder.recyclerView)
        mItemTouchHelper.startDragManual(cardViewHolder)
        currentListViewHolder?.setIsRecyclable(true)
        currentListViewHolder = listViewHolder
        currentListViewHolder?.setIsRecyclable(false)

        if (d)
            listExitCallbackCalled = false
    }

    fun stopDragEventIfStillRunning() {
        if (currentDragType != DragType.NONE && !isDragging) {
            stopDrag()
        }
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
        activeListIndex = NO_LIST_ACTIVE
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
            val manager = recyclerView.layoutManager as LinearLayoutManager
            val firstPos = manager.findFirstCompletelyVisibleItemPosition()
            var offsetTop = 0;
            if(firstPos >= 0) {
                val firstView = manager.findViewByPosition(firstPos)
                offsetTop = manager.getDecoratedTop(firstView) - manager.getTopDecorationHeight(firstView)
            }

            onItemMove(source.adapterPosition, target.adapterPosition)
            if(firstPos >= 0) {
                Log.d("TEST", "************ ${source.adapterPosition} ${target.adapterPosition}")
                manager.scrollToPositionWithOffset(firstPos, offsetTop)
            }
            //currentListViewHolder?.recyclerView?.scrollToPosition(source.adapterPosition)
            return true
        }

        override fun canDropOver(recyclerView: RecyclerView, current: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            if (currentDragType == DragType.LIST) {
                return mBoardViewListener!!.canListDropOver(current as ListViewHolder<BobBoardListAdapter<*>>, target as ListViewHolder<BobBoardListAdapter<*>>)
            }
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
        fun canCardDropInList(listViewHolder: ListViewHolder<BobBoardListAdapter<*>>): Boolean
        fun canListDropOver(listViewHolder: ListViewHolder<BobBoardListAdapter<*>>, otherListViewHolder: ListViewHolder<BobBoardListAdapter<*>>): Boolean

        fun onCardDragEnteredList(previousListViewHolder: ListViewHolder<BobBoardListAdapter<*>>?, listViewHolder: ListViewHolder<BobBoardListAdapter<*>>, toIndex: Int)
        fun onListDragExitedBoardView(listViewHolder: ListViewHolder<BobBoardListAdapter<*>>): Boolean
        fun onListDragEnteredBoardView(listViewHolder: ListViewHolder<BobBoardListAdapter<*>>?, overIndex: Int): Boolean


        //fun canDropOnList(listIndex: Int)
        //fun canDropOnItem(listIndex: Int, itemIndex: Int)
    }

    companion object {
        private const val NO_LIST_ACTIVE = -1
        private const val TAG = "BobBoardView"
    }
}
