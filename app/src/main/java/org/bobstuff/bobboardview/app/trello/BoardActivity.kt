package org.bobstuff.bobboardview.app.trello

import android.animation.Animator
import android.graphics.Color
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearSnapHelper
import android.support.v7.widget.SnapHelper
import android.support.v7.widget.Toolbar
import android.util.Log
import android.util.TypedValue
import android.view.DragEvent
import android.view.MenuItem
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_board.*
import org.bobstuff.bobboardview.*
import org.bobstuff.bobboardview.app.R
import org.bobstuff.bobboardview.app.trello.model.BoardList

import org.bobstuff.bobboardview.app.trello.model.Card
import org.bobstuff.bobboardview.app.util.DragOperation
import org.bobstuff.bobboardview.app.util.setStatusBarColor
import org.bobstuff.bobboardview.app.util.styleToolbarContent

import java.util.Collections

class BoardActivity : AppCompatActivity(), BobBoardView.BobBoardViewListener {
    private lateinit var trelloBoardAdapter: TrelloBoardAdapter
    private lateinit var toolbar: Toolbar
    private lateinit var boardView: BobBoardView
    private lateinit var snapHelper: SnapHelper
    private lateinit var archiveView: TextView
    private var board = createTestData()
    private val currentDragOperation = DragOperation<Card, BoardList>()
    private var dragOperationInsideArchiveView: Boolean = false
    private var dragOperationEnteredArchiveView: Boolean = false
    private var dragOperationPositionInArchiveView = PointF()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_board)

        trelloBoardAdapter = TrelloBoardAdapter(baseContext, resources.getDimensionPixelSize(R.dimen.trello_item_list_width))
        trelloBoardAdapter.setHasStableIds(true)
        trelloBoardAdapter.setItems(board.lists)

        boardView = findViewById(R.id.board_view)
        boardView.setBoardAdapter(trelloBoardAdapter)
        boardView.setBoardViewListener(this)
        boardView.listRecyclerView.contentDescription = "column recycler"

        val dividerSizeInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20f, resources.displayMetrics)
        val dividerItemDecoration = BobBoardSimpleDividers(dividerSizeInPixels.toInt(), BobBoardSimpleDividersOrientation.HORIZONTAL)
        boardView.listRecyclerView.addItemDecoration(dividerItemDecoration)

        snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(boardView.listRecyclerView)

        toolbar = findViewById(R.id.my_toolbar)
        setSupportActionBar(toolbar)
        styleToolbarContent(toolbar, Color.parseColor("#ffffff"))
        setStatusBarColor(Color.parseColor("#003365"), false)

        archiveView = findViewById(R.id.archive_view)
        archiveView.visibility = View.INVISIBLE
        archiveView.setOnDragListener { _, event ->
            val action = event.action
            when (action) {
                DragEvent.ACTION_DRAG_ENTERED -> {
                    dragOperationEnteredArchiveView = true
                }
                DragEvent.ACTION_DRAG_LOCATION -> {
                    dragOperationPositionInArchiveView.set(event.x, event.y)
                    if (dragOperationEnteredArchiveView) {
                        dragOperationEnteredArchiveView = false
                        dragOperationInsideArchiveView = true
                        toolbar.background = ColorDrawable(Color.parseColor("#00ff00"))
                        if (currentDragOperation.dragType == BobBoardView.DragType.LIST) {
                            removeDragListItem()
                        }
                    }
                }
                DragEvent.ACTION_DRAG_EXITED -> {
                    dragOperationEnteredArchiveView = false
                    if (dragOperationInsideArchiveView) {
                        dragOperationInsideArchiveView = false
                        toolbar.background = ColorDrawable(Color.parseColor("#00407f"))
                        if (currentDragOperation.dragType == BobBoardView.DragType.LIST) {
                            insertDragListItem(dragOperationPositionInArchiveView.x, dragOperationPositionInArchiveView.y)
                        }
                    }
                }
            }
            true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onListDragStarted(listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>) {
        snapHelper.attachToRecyclerView(null)
        val actionBar = supportActionBar!!
        actionBar.setDisplayShowHomeEnabled(false)
        actionBar.setDisplayShowTitleEnabled(false)

        val cx = archiveView.width / 2
        val cy = archiveView.height / 2
        val finalRadius = Math.hypot(cx.toDouble(), cy.toDouble()).toFloat()
        val anim = ViewAnimationUtils.createCircularReveal(archiveView, cx, cy, 0f, finalRadius)
        archiveView.visibility = View.VISIBLE
        anim.start()

        trelloBoardAdapter.triggerScaleDownAnimationForListDrag(listViewHolder)
        currentDragOperation.startListDrag(listViewHolder.adapterPosition)
    }

    override fun onCardDragStarted(listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>, cardViewHolder: BobBoardListAdapter.CardViewHolder) {
        snapHelper.attachToRecyclerView(null)
        currentDragOperation.startCardDrag(listViewHolder.adapterPosition, cardViewHolder.adapterPosition)

        val actionBar = supportActionBar!!
        actionBar.setDisplayShowHomeEnabled(false)
        actionBar.setDisplayShowTitleEnabled(false)
        val cx = archiveView.width / 2
        val cy = archiveView.height / 2
        val finalRadius = Math.hypot(cx.toDouble(), cy.toDouble()).toFloat()
        val anim = ViewAnimationUtils.createCircularReveal(archiveView, cx, cy, 0f, finalRadius)
        archiveView.visibility = View.VISIBLE
        anim.start()
    }

    override fun onListDragEnded(listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>?) {
        Log.d("TEST", "onlistdragended")
        if (!dragOperationInsideArchiveView && currentDragOperation.orphaned) {
            Log.d("TEST", "drag operation not inside archive view")
            trelloBoardAdapter.insertItem(currentDragOperation.listIndex, currentDragOperation.listItem!!, false)


            board.lists.add(currentDragOperation.listIndex, currentDragOperation.listItem!!)
        } else {
            Log.d("TEST", "list operation view")
            listViewHolder?.itemView?.visibility = View.VISIBLE
        }
        val finalPosition = currentDragOperation.listIndex
        val cx = archiveView.width / 2
        val cy = archiveView.height / 2
        val finalRadius = Math.hypot(cx.toDouble(), cy.toDouble()).toFloat()
        val anim = ViewAnimationUtils.createCircularReveal(archiveView, cx, cy, 0f, finalRadius)
        anim.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                archiveView.visibility = View.INVISIBLE
                val actionBar = supportActionBar!!
                actionBar.setDisplayShowHomeEnabled(true)
                actionBar.setDisplayShowTitleEnabled(true)
                toolbar.background = ColorDrawable(Color.parseColor("#00407f"))

                snapHelper.attachToRecyclerView(boardView.listRecyclerView)
            }

            override fun onAnimationCancel(animation: Animator) {}

            override fun onAnimationRepeat(animation: Animator) {}
        })
        anim.start()
        listViewHolder?.let { trelloBoardAdapter.triggerScaleUpAnimationForListDrag(it) }

        Toast.makeText(this, "List moved from col: ${currentDragOperation.startingListIndex} " +
                "to col: ${currentDragOperation.listIndex}", Toast.LENGTH_LONG).show()
        currentDragOperation.reset()


        val offset = boardView.listRecyclerView.scrollX
        //boardView.listRecyclerView.smoothScrollToPosition(finalPosition)
    }

    override fun onCardDragEnded(listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>?, cardViewHolder: BobBoardListAdapter.CardViewHolder?) {
        if (!dragOperationInsideArchiveView) {
            Toast.makeText(this, "Card moved from col: ${currentDragOperation.startingListIndex}, " +
                    "row: ${currentDragOperation.startingCardIndex} to col: ${currentDragOperation.listIndex}, " +
                    "row: ${currentDragOperation.cardIndex}", Toast.LENGTH_LONG).show()
        }
        snapHelper.attachToRecyclerView(boardView.listRecyclerView)
        if (currentDragOperation.dragType == BobBoardView.DragType.CARD && currentDragOperation.orphaned && !dragOperationInsideArchiveView) {
            val viewHolder = boardView.listRecyclerView.findViewHolderForAdapterPosition(currentDragOperation.listIndex) as TrelloBoardAdapter.TrelloListViewHolder
            val listAdapter = viewHolder.listAdapter
            listAdapter.insertItem(currentDragOperation.cardIndex, currentDragOperation.cardItem!!, false)

            viewHolder.recyclerView.scrollToPosition(currentDragOperation.cardIndex)

            board.lists[currentDragOperation.listIndex].cards.add(currentDragOperation.cardIndex, currentDragOperation.cardItem!!)
        } else if (currentDragOperation.dragType == BobBoardView.DragType.CARD && currentDragOperation.orphaned && dragOperationInsideArchiveView) {
            Toast.makeText(this, "Card archived", Toast.LENGTH_LONG).show()
        } else {
            val cardViewHolder = cardViewHolder as BobBoardListAdapter.CardViewHolder
            cardViewHolder.itemView.visibility = View.VISIBLE
        }
        currentDragOperation.reset()

        val cx = archiveView.width / 2
        val cy = archiveView.height / 2
        val finalRadius = Math.hypot(cx.toDouble(), cy.toDouble()).toFloat()
        val anim = ViewAnimationUtils.createCircularReveal(archiveView, cx, cy, 0f, finalRadius)
        anim.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                archiveView.visibility = View.INVISIBLE
                toolbar.background = ColorDrawable(Color.parseColor("#00407f"))
                val actionBar = supportActionBar!!
                actionBar.setDisplayShowHomeEnabled(true)
                actionBar.setDisplayShowTitleEnabled(true)
            }

            override fun onAnimationCancel(animation: Animator) {}

            override fun onAnimationRepeat(animation: Animator) {}
        })
        anim.start()
    }

    override fun onListMove(fromPosition: Int, toPosition: Int) {
        trelloBoardAdapter.swapItem(fromPosition, toPosition)
        Collections.swap(board.lists, fromPosition, toPosition)
        currentDragOperation.listIndex = toPosition
        //boardView!!.listRecyclerView.scrollToPosition(toPosition)
    }

    override fun onCardMove(listViewHolder: BobBoardAdapter.ListViewHolder<*>, listIndex: Int, fromPosition: Int, toPosition: Int) {
        (listViewHolder as TrelloBoardAdapter.TrelloListViewHolder).listAdapter.onItemMove(fromPosition, toPosition)

        Collections.swap(board!!.lists[listIndex].cards, fromPosition, toPosition)
        currentDragOperation.cardIndex = toPosition
    }

    override fun onCardMoveList(toViewHolder: BobBoardAdapter.ListViewHolder<out BobBoardListAdapter<*>>,
                                toIndex: Int) {
        val viewHolder = toViewHolder as TrelloBoardAdapter.TrelloListViewHolder
        viewHolder.listAdapter.insertItem(toIndex, currentDragOperation.cardItem!!)

        toViewHolder.recyclerView.scrollToPosition(toIndex)

        board.lists[toViewHolder.adapterPosition].cards.add(toIndex, currentDragOperation.cardItem!!)

        currentDragOperation.orphaned = false
        currentDragOperation.listIndex = toViewHolder.adapterPosition
        currentDragOperation.cardIndex = toIndex
    }

    override fun onCardDragExitedList(listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>, cardViewHolder: BobBoardListAdapter.CardViewHolder) {
        if (listViewHolder.adapterPosition == 4) {
            return
        }

        if (listViewHolder.itemViewType == 1) {
            return
        }
        Log.d("TEST", "onCardDragExitedList(): listviewholder position - ${listViewHolder.adapterPosition}, cardViewHolder position: ${cardViewHolder.adapterPosition}")
        val fromIndex = cardViewHolder.adapterPosition
        currentDragOperation.listIndex = listViewHolder.adapterPosition
        currentDragOperation.cardIndex = cardViewHolder.adapterPosition

        (listViewHolder as TrelloBoardAdapter.TrelloListViewHolder).listAdapter.removeItem(fromIndex)
        val userStory = board.lists[listViewHolder.getAdapterPosition()].cards.removeAt(fromIndex)
        currentDragOperation.cardItem = userStory
        currentDragOperation.orphaned = true
        boardView.cardRemovedDuringDrag()
    }

    override fun onCardDragEnteredList(previousListViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>?, listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>, toIndex: Int) {
        if (listViewHolder.adapterPosition == 4) {
            return
        }
        if (listViewHolder.itemViewType == 1) {
            return
        }
        Log.d("TEST", "onCardDragEnteredList(): toIndex - $toIndex")
        val viewHolder = listViewHolder as TrelloBoardAdapter.TrelloListViewHolder
        viewHolder.listAdapter.insertItem(toIndex, currentDragOperation.cardItem!!)

        listViewHolder.recyclerView.scrollToPosition(toIndex)

        board.lists[listViewHolder.adapterPosition].cards.add(toIndex, currentDragOperation.cardItem!!)

        currentDragOperation.orphaned = false
        currentDragOperation.listIndex = listViewHolder.adapterPosition
        currentDragOperation.cardIndex = toIndex
    }

    override fun onListDragEnteredBoardView(listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>?, overIndex: Int): Boolean {
        Log.d("TEST", "list entered board view over $overIndex")
        Log.d("TEST", "onListDragEnteredBoardView: Over index was $overIndex")
        //TODO work out better idea for this.  It is required because during fast movement all children
        //of the recycler are animating and have no active index so the only safe thing to do it fallback
        // to what we know, this is possibly not where they intended but an out of order column is better
        //that a crash.  To trigger this you really have to try very hard to move really quickly in
        //and out of the archive logic
        //val index = if (overIndex == -1) { currentDragOperation.listIndex } else overIndex
        //boardView.disableDragEvents = true
        //TODO insert and create crazy tests
        //trelloBoardAdapter.insertItem(index, currentDragOperation.listItem!!)
        //board.lists.add(index, currentDragOperation.listItem!!)
        //currentDragOperation.listIndex = index
        //currentDragOperation.orphaned = false
        //boardView.listRecyclerView.scrollToPosition(index)
        return false
    }

    override fun onListDragExitedBoardView(listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>): Boolean {
        Log.d("TEST", "list exited board view")
        return false
        //board.lists.removeAt(listViewHolder.adapterPosition)
        //currentDragOperation.listItem = trelloBoardAdapter.removeItem(listViewHolder.adapterPosition)
        //currentDragOperation.orphaned = true
    }

    fun removeDragListItem() {
        boardView.listRemovedDuringDrag()
        board.lists.removeAt(currentDragOperation.listIndex)
        currentDragOperation.listItem = trelloBoardAdapter.removeItem(currentDragOperation.listIndex)
        currentDragOperation.orphaned = true

    }

    fun insertDragListItem(x: Float, y: Float) {
        val offsetViewBounds = Rect()
        archiveView.getDrawingRect(offsetViewBounds)
        toolbar.offsetDescendantRectToMyCoords(archiveView, offsetViewBounds)
        val destinationIndex = boardView.listAdapterPositionAtPoint(x + offsetViewBounds.left, 0f)
        Log.d("TEST", "insertDragListItem index - $destinationIndex")
        val index = if (destinationIndex == -1) { currentDragOperation.listIndex } else destinationIndex

        trelloBoardAdapter.insertItem(index, currentDragOperation.listItem!!)
        board.lists.add(index, currentDragOperation.listItem!!)
        currentDragOperation.listIndex = index
        currentDragOperation.orphaned = false
        boardView.listRecyclerView.scrollToPosition(index)
    }

    override fun canCardDropInList(listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>): Boolean {
        if (listViewHolder.adapterPosition != 4)
            return true
        return false
    }

    override fun canListDropOver(listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>, otherListViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>): Boolean {
        return otherListViewHolder.adapterPosition != board.lists.size
    }
}
