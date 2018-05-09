package org.bobstuff.bobboardview.app.scrum

import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.util.Log
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import org.bobstuff.bobboardview.*
import org.bobstuff.bobboardview.BobBoardSimpleDividersOrientation.HORIZONTAL
import org.bobstuff.bobboardview.app.R
import org.bobstuff.bobboardview.app.simple.SimpleListAdapter
import org.bobstuff.bobboardview.app.trello.model.BoardList
import org.bobstuff.bobboardview.app.util.DragOperation
import org.bobstuff.bobboardview.app.util.getWindowSize
import org.bobstuff.bobboardview.app.util.setStatusBarColor
import org.bobstuff.bobboardview.app.util.styleToolbarContent
import java.util.*

class ScrumActivity : AppCompatActivity() {
    private lateinit var boardView: BobBoardView
    private lateinit var boardAdapter: ScrumBoardAdapter
    private lateinit var project: Project

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scrum)
        setStatusBarColor(Color.parseColor("#2b2b2b"), false)

        project = if (intent.hasExtra("REUSE_DATA")) {
            org.bobstuff.bobboardview.app.reuse.generateTestData()
        } else {
            generateTestData()
        }

        val dragOperation = BobBoardDragOperation<UserStory, Column>()

        val windowSize = getWindowSize()
        val sizeInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 400f, resources.displayMetrics).toInt()
        boardAdapter = ScrumBoardAdapter(this, (Math.min((windowSize.x * 0.86).toInt(), sizeInPixels)), dragOperation)
        boardAdapter.setItems(project.columns)

        val config = SimpleBobBoardViewListener.SimpleBobBoardViewListenerConfigBuilder()
                .removeListWhenDraggingOutsideBoardView(true)
                .build()
        val listener = CustomBobBoardViewListener(dragOperation, config)

        boardView = findViewById(R.id.board_view)
        boardView.boardAdapter = boardAdapter
        boardView.setBoardViewListener(listener)
        boardView.listRecyclerView.setScrollingTouchSlop(RecyclerView.TOUCH_SLOP_PAGING)
        boardView.listRecyclerView.contentDescription = "column recycler"

        val listItemDecoration = BobBoardSimpleDividers((windowSize.x * 0.03).toInt(), BobBoardSimpleDividersOrientation.HORIZONTAL)
        boardView.listRecyclerView.addItemDecoration(listItemDecoration)

        val toolbar = findViewById<Toolbar>(R.id.my_toolbar)
        setSupportActionBar(toolbar)
        styleToolbarContent(toolbar, Color.parseColor("#bbbbbb"))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBundle("boardViewState", boardView.onSaveInstance() as Bundle)

        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)

        savedInstanceState?.run {
            boardView.onRestoreState(savedInstanceState.getBundle("boardViewState"))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    inner class CustomBobBoardViewListener(currentDragOperation: BobBoardDragOperation<UserStory, Column>, config: SimpleBobBoardViewListener.SimpleBobBoardViewListenerConfig):
            SimpleBobBoardViewListener<UserStory, Column>(currentDragOperation, config) {

        override fun removeCardFromDataSource(listIndex: Int, cardIndex: Int): UserStory {
            return project.columns[listIndex].userStories.removeAt(cardIndex)
        }

        override fun addCardToDataSource(listIndex: Int, cardIndex: Int, story: UserStory) {
            project.columns[listIndex].userStories.add(cardIndex, story)
        }

        override fun moveCardInDataSource(listIndex: Int, fromCardIndex: Int, toCardIndex: Int) {
            Collections.swap(project.columns[listIndex].userStories, fromCardIndex, toCardIndex)
        }

        override fun moveListInDataSource(fromListIndex: Int, toListIndex: Int) {
            Collections.swap(project.columns, fromListIndex, toListIndex)
        }

        override fun addListToDataSource(listIndex: Int, boardList: Column) {
            project.columns.add(listIndex, boardList)
        }

        override fun removeListFromDataSource(listIndex: Int): Column {
            return project.columns.removeAt(listIndex)
        }

        override fun onCardDragEnded(boardView: BobBoardView, activeListViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>?,
                                     listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>?,
                                     cardViewHolder: BobBoardListAdapter.CardViewHolder?) {
            super.onCardDragEnded(boardView, activeListViewHolder, listViewHolder, cardViewHolder)
            val cardViewHolder = cardViewHolder as ScrumColumnAdapter.ScrumUserStoryViewHolder
            cardViewHolder.overlay.visibility = View.INVISIBLE
        }

        override fun onListDragEnded(boardView: BobBoardView, listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>?) {
            listViewHolder?.itemView?.visibility = View.VISIBLE
        }

        override fun canCardDropInList(boardView: BobBoardView, listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>): Boolean {
            //if (listViewHolder.itemId != "List 1".hashCode().toLong()) {
           //     return false
            //}
            return super.canCardDropInList(boardView, listViewHolder)
        }
    }
}
