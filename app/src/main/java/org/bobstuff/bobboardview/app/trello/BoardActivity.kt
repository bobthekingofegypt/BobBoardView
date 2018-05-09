package org.bobstuff.bobboardview.app.trello

import android.animation.Animator
import android.graphics.Color
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.v7.widget.*
import android.util.Log
import android.util.TypedValue
import android.view.DragEvent
import android.view.MenuItem
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.TextView
import android.widget.Toast
import org.bobstuff.bobboardview.*
import org.bobstuff.bobboardview.app.R
import org.bobstuff.bobboardview.app.scrum.ScrumColumnAdapter
import org.bobstuff.bobboardview.app.trello.model.BoardList

import org.bobstuff.bobboardview.app.trello.model.Card
import org.bobstuff.bobboardview.app.util.DragOperation
import org.bobstuff.bobboardview.app.util.getWindowSize
import org.bobstuff.bobboardview.app.util.setStatusBarColor
import org.bobstuff.bobboardview.app.util.styleToolbarContent

import java.util.Collections

class BoardActivity : AppCompatActivity() {
    private lateinit var trelloBoardAdapter: TrelloBoardAdapter
    private lateinit var toolbar: Toolbar
    private lateinit var boardView: BobBoardView
    private var snapHelper: SnapHelper? = null
    private lateinit var archiveView: TextView
    private var board = createTestData()
    private val currentDragOperation = BobBoardDragOperation<Card, BoardList>()
    private var dragOperationInsideArchiveView: Boolean = false
    private var dragOperationEnteredArchiveView: Boolean = false
    private var dragOperationPositionInArchiveView = PointF()
    private var largeScreen: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_board)

        trelloBoardAdapter = TrelloBoardAdapter(this, currentDragOperation, resources.getDimensionPixelSize(R.dimen.trello_item_list_width))
        trelloBoardAdapter.setItems(board.lists)

        val config = SimpleBobBoardViewListener.SimpleBobBoardViewListenerConfigBuilder()
                .removeItemWhenDraggingBetweenLists(true)
                .removeListWhenDraggingOutsideBoardView(true)
                .build()
        val listener = CustomBobBoardViewListener(currentDragOperation, config)

        boardView = findViewById(R.id.board_view)
        boardView.boardAdapter = trelloBoardAdapter
        boardView.setBoardViewListener(listener)
        boardView.listRecyclerView.contentDescription = "column recycler"
        boardView.listRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val dividerSizeInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20f, resources.displayMetrics)
        val dividerItemDecoration = BobBoardSimpleDividers(dividerSizeInPixels.toInt(), BobBoardSimpleDividersOrientation.HORIZONTAL)
        boardView.listRecyclerView.addItemDecoration(dividerItemDecoration)

        val windowSize = getWindowSize()
        val listSize = resources.getDimensionPixelSize(R.dimen.trello_item_list_width)
        if ((windowSize.x / listSize) < 2) {
            snapHelper = LinearSnapHelper()
        }
        if ((windowSize.x / listSize) > 3) {
            largeScreen = true
        }
        snapHelper?.attachToRecyclerView(boardView.listRecyclerView)

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

    inner class CustomBobBoardViewListener(currentDragOperation: BobBoardDragOperation<Card, BoardList>, config: SimpleBobBoardViewListener.SimpleBobBoardViewListenerConfig):
            SimpleBobBoardViewListener<Card, BoardList>(currentDragOperation, config) {

        override fun removeCardFromDataSource(listIndex: Int, cardIndex: Int): Card {
            return board.lists[listIndex].cards.removeAt(cardIndex)
        }

        override fun addListToDataSource(listIndex: Int, boardList: BoardList) {
            board.lists.add(listIndex, boardList)
        }

        override fun addCardToDataSource(listIndex: Int, cardIndex: Int, card: Card) {
            board.lists[listIndex].cards.add(cardIndex, card)
        }

        override fun moveCardInDataSource(listIndex: Int, fromCardIndex: Int, toCardIndex: Int) {
            Collections.swap(board.lists[listIndex].cards, fromCardIndex, toCardIndex)
        }

        override fun moveListInDataSource(fromListIndex: Int, toListIndex: Int) {
            Collections.swap(board.lists, fromListIndex, toListIndex)
        }

        override fun removeListFromDataSource(listIndex: Int): BoardList {
            return board.lists.removeAt(listIndex)
        }

        override fun onCardDragEnded(boardView: BobBoardView, activeListViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>?,
                                     listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>?,
                                     cardViewHolder: BobBoardListAdapter.CardViewHolder?) {
            if (!dragOperationInsideArchiveView) {
                Toast.makeText(this@BoardActivity, "Card moved from col: ${currentDragOperation.startingListIndex}, " +
                        "row: ${currentDragOperation.startingCardIndex} to col: ${currentDragOperation.listIndex}, " +
                        "row: ${currentDragOperation.cardIndex}", Toast.LENGTH_LONG).show()
            }
            if (dragOperationInsideArchiveView && currentDragOperation.orphaned) {
                Toast.makeText(this@BoardActivity, "Card archived", Toast.LENGTH_LONG).show()
                currentDragOperation.orphaned = false
            }

            val finalColumnPosition = currentDragOperation.listIndex

            super.onCardDragEnded(boardView, activeListViewHolder, listViewHolder, cardViewHolder)

            cardViewHolder?.itemView?.visibility = View.VISIBLE

            val anim = centeredCircleAnimation(archiveView, true)
            anim.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}

                override fun onAnimationEnd(animation: Animator) {
                    archiveView.visibility = View.INVISIBLE
                    toolbar.background = ColorDrawable(Color.parseColor("#00407f"))
                    val actionBar = supportActionBar!!
                    actionBar.setDisplayShowHomeEnabled(true)
                    actionBar.setDisplayShowTitleEnabled(true)
                    snapHelper?.attachToRecyclerView(boardView.listRecyclerView)
                }

                override fun onAnimationCancel(animation: Animator) {}

                override fun onAnimationRepeat(animation: Animator) {}
            })
            anim.start()

            boardView.listRecyclerView.smoothScrollToPosition(finalColumnPosition)
        }

        override fun onListDragStarted(boardView: BobBoardView, listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>) {
            super.onListDragStarted(boardView, listViewHolder)

            snapHelper?.attachToRecyclerView(null)
            val actionBar = supportActionBar!!
            actionBar.setDisplayShowHomeEnabled(false)
            actionBar.setDisplayShowTitleEnabled(false)

            val anim = centeredCircleAnimation(archiveView, false)
            archiveView.visibility = View.VISIBLE
            anim.start()

            trelloBoardAdapter.triggerScaleDownAnimationForListDrag(listViewHolder)
        }

        override fun onCardDragStarted(boardView: BobBoardView, listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>, cardViewHolder: BobBoardListAdapter.CardViewHolder) {
            super.onCardDragStarted(boardView, listViewHolder, cardViewHolder)

            snapHelper?.attachToRecyclerView(null)

            val actionBar = supportActionBar!!
            actionBar.setDisplayShowHomeEnabled(false)
            actionBar.setDisplayShowTitleEnabled(false)

            val anim = centeredCircleAnimation(archiveView, false)
            archiveView.visibility = View.VISIBLE
            anim.start()
        }

        override fun onListDragEnded(boardView: BobBoardView, listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>?) {
            Log.d("TEST", "DID THIS THINGY GET CALLED")
            if (dragOperationInsideArchiveView && currentDragOperation.orphaned) {
                currentDragOperation.orphaned = false
            }

            super.onListDragEnded(boardView, listViewHolder)
            listViewHolder?.itemView?.visibility = View.VISIBLE

            val anim = centeredCircleAnimation(archiveView, true)
            anim.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}

                override fun onAnimationEnd(animation: Animator) {
                    archiveView.visibility = View.INVISIBLE
                    val actionBar = supportActionBar!!
                    actionBar.setDisplayShowHomeEnabled(true)
                    actionBar.setDisplayShowTitleEnabled(true)
                    toolbar.background = ColorDrawable(Color.parseColor("#00407f"))

                    snapHelper?.attachToRecyclerView(boardView.listRecyclerView)
                }

                override fun onAnimationCancel(animation: Animator) {}

                override fun onAnimationRepeat(animation: Animator) {}
            })
            anim.start()
            listViewHolder?.let { trelloBoardAdapter.triggerScaleUpAnimationForListDrag(it, !largeScreen) }

            Toast.makeText(this@BoardActivity, "List moved from col: ${currentDragOperation.startingListIndex} " +
                    "to col: ${currentDragOperation.listIndex}", Toast.LENGTH_LONG).show()
        }

        override fun onListDragEnteredBoardView(boardView: BobBoardView, listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>?, overIndex: Int): Boolean {
            return false
        }

        override fun onListDragExitedBoardView(boardView: BobBoardView, listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>): Boolean {
            return false
        }

        override fun canListDropOver(boardView: BobBoardView, listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>, otherListViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>): Boolean {
            if (otherListViewHolder.adapterPosition == board.lists.size) {
                return false
            }
            return true
        }

        override fun canCardDropInList(boardView: BobBoardView, listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>): Boolean {
            if (listViewHolder.itemId == 666L) {
                return false
            }
            return true
        }
    }

    private fun centeredCircleAnimation(view: View, reverse: Boolean): Animator {
        val cx = view.width / 2
        val cy = view.height / 2
        val finalRadius = Math.hypot(cx.toDouble(), cy.toDouble()).toFloat()
        if (reverse) {
            return ViewAnimationUtils.createCircularReveal(view, cx, cy, finalRadius, 0f)
        }
        return ViewAnimationUtils.createCircularReveal(view, cx, cy, 0f, finalRadius)
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
        if (destinationIndex == -1) {
            boardView.listRecyclerView.postOnAnimation {
                insertDragListItem(x, y)
            }
            return
        }
        val index = if (destinationIndex == -1) { currentDragOperation.listIndex } else destinationIndex

        trelloBoardAdapter.insertItem(index, currentDragOperation.listItem!!, true)
        board.lists.add(index, currentDragOperation.listItem!!)
        currentDragOperation.listIndex = index
        currentDragOperation.orphaned = false
        boardView.listRecyclerView.scrollToPosition(index)
    }
}
