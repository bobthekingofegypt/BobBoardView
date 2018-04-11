package org.bobstuff.bobboardview.app.simple

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.view.View
import org.bobstuff.bobboardview.*
import org.bobstuff.bobboardview.app.R
import java.util.*

class SimpleActivity : AppCompatActivity(), SimpleBoardAdapter.BobBoardViewInteractionListener {
    private lateinit var boardView: BobBoardView
    private lateinit var boardAdapter: SimpleBoardAdapter
    private var board: Board = generateTestData()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple)

        val dragOperation = BobBoardDragOperation<Card, BoardList>()

        boardAdapter = SimpleBoardAdapter(this, dragOperation, this)
        boardAdapter.setItems(board.boardLists)

        val config = SimpleBobBoardViewListener.SimpleBobBoardViewListenerConfigBuilder()
                .removeItemWhenDraggingBetweenLists(false)
                .build()
        val listener = CustomBobBoardViewListener(dragOperation, config)

        boardView = findViewById(R.id.board_view)
        boardView.boardAdapter = boardAdapter
        boardView.setBoardViewListener(listener)
        boardView.listRecyclerView.setScrollingTouchSlop(RecyclerView.TOUCH_SLOP_PAGING)
    }

    override fun addNewList() {
        val index = board.boardLists.size
        val newBoard = BoardList(index+1, "A new list", mutableListOf())
        board.boardLists.add(index, newBoard)

        boardAdapter.insertItem(index, newBoard)
        boardView.listRecyclerView.scrollToPosition(index+1)
    }

    inner class CustomBobBoardViewListener(currentDragOperation: BobBoardDragOperation<Card, BoardList>, config: SimpleBobBoardViewListener.SimpleBobBoardViewListenerConfig):
            SimpleBobBoardViewListener<Card, BoardList>(currentDragOperation, config) {

        override fun removeCardFromDataSource(listIndex: Int, cardIndex: Int): Card {
            return board.boardLists[listIndex].cards.removeAt(cardIndex)
        }

        override fun addCardToDataSource(listIndex: Int, cardIndex: Int, card: Card) {
            board.boardLists[listIndex].cards.add(cardIndex, card)
        }

        override fun moveCardInDataSource(listIndex: Int, fromCardIndex: Int, toCardIndex: Int) {
            Collections.swap(board.boardLists[listIndex].cards, fromCardIndex, toCardIndex)
        }

        override fun moveListInDataSource(fromListIndex: Int, toListIndex: Int) {
            Collections.swap(board.boardLists, fromListIndex, toListIndex)
        }

        override fun addListToDataSource(listIndex: Int, boardList: BoardList) {
            //no-op
        }

        override fun removeListFromDataSource(listIndex: Int): BoardList {
            return board.boardLists.removeAt(listIndex)
        }

        override fun onCardDragEnded(boardView: BobBoardView, activeListViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>?,
                                     listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>?,
                                     cardViewHolder: BobBoardListAdapter.CardViewHolder?) {
            super.onCardDragEnded(boardView, activeListViewHolder, listViewHolder, cardViewHolder)
            val cardViewHolder = cardViewHolder as SimpleListAdapter.SimpleCardViewHolder
            cardViewHolder.overlay.visibility = View.INVISIBLE

            activeListViewHolder?.let {
                if (activeListViewHolder.itemId == 2L) {
                    val activeListViewHolder = activeListViewHolder as SimpleBoardAdapter.SimpleListViewHolder
                    activeListViewHolder.overlayView.visibility = View.INVISIBLE
                }
            }
        }

        override fun onListDragEnded(boardView: BobBoardView, listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>?) {
            listViewHolder?.itemView?.visibility = View.VISIBLE
        }

        override fun onCardDragEnteredList(boardView: BobBoardView, previousListViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>?, listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>, cardViewHolder: BobBoardListAdapter.CardViewHolder?, toIndex: Int) {
            super.onCardDragEnteredList(boardView, previousListViewHolder, listViewHolder, cardViewHolder, toIndex)

            if (listViewHolder.itemId == 2L) {
                val listViewHolder = listViewHolder as SimpleBoardAdapter.SimpleListViewHolder
                listViewHolder.overlayView.visibility = View.VISIBLE
            }
        }

        override fun onCardDragExitedList(boardView: BobBoardView, listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>, cardViewHolder: BobBoardListAdapter.CardViewHolder) {
            super.onCardDragExitedList(boardView, listViewHolder, cardViewHolder)

            if (listViewHolder.itemId == 2L) {
                val listViewHolder = listViewHolder as SimpleBoardAdapter.SimpleListViewHolder
                listViewHolder.overlayView.visibility = View.INVISIBLE
            }
        }

        override fun canCardDropInList(boardView: BobBoardView, listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>): Boolean {
            if (listViewHolder.itemId == 2L || listViewHolder.itemId == 666L) {
                return false
            }
            return super.canCardDropInList(boardView, listViewHolder)
        }
    }


}
