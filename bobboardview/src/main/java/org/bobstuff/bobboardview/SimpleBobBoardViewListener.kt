package org.bobstuff.bobboardview

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.Log

/**
 * Created by bob
 */

abstract class SimpleBobBoardViewListener<Card, List>(private val currentDragOperation: BobBoardDragOperation<Card, List>, val config: SimpleBobBoardViewListener.SimpleBobBoardViewListenerConfig): BobBoardView.BobBoardViewListener {

    override fun onListDragStarted(boardView: BobBoardView, listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>) {
        currentDragOperation.startListDrag(listViewHolder.adapterPosition, listViewHolder.itemId)
    }

    override fun onCardDragStarted(boardView: BobBoardView, listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>, cardViewHolder: BobBoardListAdapter.CardViewHolder) {
        currentDragOperation.startCardDrag(listViewHolder.adapterPosition, cardViewHolder.adapterPosition, listViewHolder.listAdapter?.getItemId(cardViewHolder.adapterPosition)!!)
    }

    override fun onListDragEnded(boardView: BobBoardView, listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>?) {
        if (config.removeListWhenDraggingOutsideBoardView && currentDragOperation.orphaned) {
            listViewHolder?.let {
                val listArrayAdapter = it.listAdapter as BobBoardArrayAdapter<*, *, List>
                listArrayAdapter.insertItem(currentDragOperation.listIndex, currentDragOperation.listItem!!)

                addListToDataSource(currentDragOperation.listIndex, currentDragOperation.listItem!!)
            }
        }
        currentDragOperation.reset()
    }

    override fun onCardMove(boardView: BobBoardView, listViewHolder: BobBoardAdapter.ListViewHolder<*>, listIndex: Int, fromPosition: Int, toPosition: Int) {
        Log.d("TEST", "from $fromPosition to: $toPosition listIndex: $listIndex")
        Log.d("TEST", "listViewHolder ${listViewHolder.adapterPosition}")
        listViewHolder?.let {
            val listArrayAdapter = it.listAdapter as BobBoardListArrayAdapter<*, *, *>
            listArrayAdapter.swapItems(fromPosition, toPosition)
        }

        moveCardInDataSource(listIndex, fromPosition, toPosition)

        currentDragOperation.cardIndex = toPosition
    }

    override fun onCardDragEnteredList(boardView: BobBoardView, previousListViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>?, listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>, cardViewHolder: BobBoardListAdapter.CardViewHolder?, toIndex: Int) {
        Log.d("TEST", "onCardDragEnteredList()|1")
        if (!canCardDropInList(boardView, listViewHolder) ||
                (!config.removeItemWhenDraggingBetweenLists && listViewHolder.adapterPosition == currentDragOperation.listIndex)) {
            return
        }
        Log.d("TEST", "onCardDragEnteredList()|2")

        if (config.removeItemWhenDraggingBetweenLists) {
            listViewHolder?.let {
                val listArrayAdapter = it.listAdapter as BobBoardListArrayAdapter<*, Card, *>
                listArrayAdapter.insertItem(toIndex, currentDragOperation.cardItem!!, true)
            }
        } else {
            val card = removeCardFromDataSource(currentDragOperation.listIndex,
                    currentDragOperation.cardIndex)
            previousListViewHolder?.let {
                val listArrayAdapter = it.listAdapter as BobBoardListArrayAdapter<*, *, *>
                listArrayAdapter.removeItem(currentDragOperation.cardIndex)
            }

            currentDragOperation.cardItem = card

            listViewHolder?.let {
                val listArrayAdapter = it.listAdapter as BobBoardListArrayAdapter<*, Card, *>
                listArrayAdapter.insertItem(toIndex, card, true)
            }
        }

        if (cardViewHolder != null) {
            val lm = listViewHolder.recyclerView?.layoutManager as LinearLayoutManager
            val params = cardViewHolder.itemView.layoutParams as RecyclerView.LayoutParams
            lm.scrollToPositionWithOffset(toIndex, cardViewHolder.itemView.top - params.topMargin)
        }

        addCardToDataSource(listViewHolder.adapterPosition, toIndex, currentDragOperation.cardItem!!)

        currentDragOperation.orphaned = false
        currentDragOperation.listIndex = listViewHolder.adapterPosition
        currentDragOperation.cardIndex = toIndex
    }

    abstract fun removeCardFromDataSource(listIndex: Int, cardIndex: Int): Card

    abstract fun removeListFromDataSource(listIndex: Int): List

    abstract fun addListToDataSource(listIndex: Int, boardList: List)

    abstract fun addCardToDataSource(listIndex: Int, cardIndex: Int, card: Card)

    abstract fun moveCardInDataSource(listIndex: Int, fromCardIndex: Int, toCardIndex: Int)

    abstract fun moveListInDataSource(fromListIndex: Int, toListIndex: Int)

    override fun onCardDragExitedList(boardView: BobBoardView, listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>, cardViewHolder: BobBoardListAdapter.CardViewHolder) {
        if (!canCardDropInList(boardView, listViewHolder)) {
            return
        }

        if (config.removeItemWhenDraggingBetweenLists) {
            val fromIndex = cardViewHolder.adapterPosition
            currentDragOperation.listIndex = listViewHolder.adapterPosition
            currentDragOperation.cardIndex = cardViewHolder.adapterPosition


            listViewHolder?.let {
                val listArrayAdapter = it.listAdapter as BobBoardListArrayAdapter<*, Card, *>
                listArrayAdapter.removeItem(fromIndex)
            }

            val card = removeCardFromDataSource(listViewHolder.adapterPosition, fromIndex)

            currentDragOperation.cardItem = card
            currentDragOperation.orphaned = true

            boardView.cardRemovedDuringDrag()
        }
    }

    override fun canCardDropInList(boardView: BobBoardView, listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>): Boolean {
        return true
    }

    override fun onCardDragEnded(boardView: BobBoardView, activeListViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>?, listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>?, cardViewHolder: BobBoardListAdapter.CardViewHolder?) {
        if (config.removeItemWhenDraggingBetweenLists && currentDragOperation.orphaned) {
            val viewHolder = boardView.listRecyclerView.findViewHolderForAdapterPosition(currentDragOperation.listIndex) as BobBoardAdapter.ListViewHolder<*>
            val listAdapter = viewHolder.listAdapter as BobBoardListArrayAdapter<*, Card, *>
            listAdapter.insertItem(currentDragOperation.cardIndex, currentDragOperation.cardItem!!)

            viewHolder.recyclerView?.scrollToPosition(currentDragOperation.cardIndex)

            addCardToDataSource(currentDragOperation.listIndex, currentDragOperation.cardIndex, currentDragOperation.cardItem!!)
        } else {
            //val cardViewHolder = cardViewHolder as ScrumColumnAdapter.ScrumUserStoryViewHolder
            //cardViewHolder.overlay.visibility = View.GONE
        }
        currentDragOperation.reset()
    }

    override fun onListMove(boardView: BobBoardView, fromPosition: Int, toPosition: Int) {
        assert(boardView.boardAdapter is BobBoardArrayAdapter<*,*, *>)

        val boardAdapter = boardView.boardAdapter as BobBoardArrayAdapter<*, *, List>
        boardAdapter.swapItem(fromPosition, toPosition)

        moveListInDataSource(fromPosition, toPosition)

        currentDragOperation.listIndex = toPosition
    }

    override fun canListDropOver(boardView: BobBoardView, listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>, otherListViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>): Boolean {
        return true
    }

    override fun onListDragExitedBoardView(boardView: BobBoardView, listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>): Boolean {
        if (config.removeListWhenDraggingOutsideBoardView) {
            boardView.listRemovedDuringDrag()
            removeListFromDataSource(currentDragOperation.listIndex)

            boardView.listRecyclerView.adapter?.let {
                val listAdapter = it as BobBoardArrayAdapter<*, *, List>
                val item = listAdapter.removeItem(currentDragOperation.listIndex)
                currentDragOperation.listItem = item
                currentDragOperation.orphaned = true
            }
            return true
        }
        return false
    }

    override fun onListDragEnteredBoardView(boardView: BobBoardView, listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>?, overIndex: Int): Boolean {
        if (config.removeListWhenDraggingOutsideBoardView) {
            boardView.listRecyclerView.adapter?.let {
                val listAdapter = it as BobBoardArrayAdapter<*, *, List>
                listAdapter.insertItem(overIndex, currentDragOperation.listItem!!, true)
                addListToDataSource(overIndex, currentDragOperation.listItem!!)
                currentDragOperation.listIndex = overIndex
                currentDragOperation.orphaned = false
                boardView.listRecyclerView.scrollToPosition(overIndex)
            }
        }
        return true
    }

    data class SimpleBobBoardViewListenerConfig(
            val removeItemWhenDraggingBetweenLists: Boolean,
            val removeListWhenDraggingOutsideBoardView: Boolean,
            val noDropOverLists: Set<Int>)

    class SimpleBobBoardViewListenerConfigBuilder {
        private var removeItemWhenDraggingBetweenLists = false
        private var removeListWhenDraggingOutsideBoardView = false
        private var noDropOverLists: Set<Int> = emptySet()

        fun removeItemWhenDraggingBetweenLists(value: Boolean) = this.also { it.removeItemWhenDraggingBetweenLists = value }

        fun removeListWhenDraggingOutsideBoardView(value: Boolean) = this.also { it.removeListWhenDraggingOutsideBoardView = value }

        fun noDropOverLists(value: Set<Int>) = this.also { it.noDropOverLists = value }

        fun build() = SimpleBobBoardViewListenerConfig(removeItemWhenDraggingBetweenLists, removeListWhenDraggingOutsideBoardView, noDropOverLists)
    }
}