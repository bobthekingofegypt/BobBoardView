package org.bobstuff.bobboardview.app.reuse

import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import org.bobstuff.bobboardview.*
import org.bobstuff.bobboardview.app.R
import org.bobstuff.bobboardview.app.scrum.*
import org.bobstuff.bobboardview.app.util.DragOperation
import org.bobstuff.bobboardview.app.util.getWindowSize
import org.bobstuff.bobboardview.app.util.setStatusBarColor
import org.bobstuff.bobboardview.app.util.styleToolbarContent
import java.util.*

/**
 * This sample reuses the scrum design but simply adds lots and lots of test data to test scroll
 * positions during reuse
 */
class ReuseSampleActivity : AppCompatActivity(), BobBoardView.BobBoardViewListener {
    private lateinit var boardView: BobBoardView
    private lateinit var boardAdapter: ScrumBoardAdapter
    private var project: Project = generateTestData()
    private val currentDragOperation = DragOperation<UserStory, Column>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reuse_sample)
        setStatusBarColor(Color.parseColor("#2b2b2b"), false)

        val windowSize = getWindowSize()

        boardAdapter = ScrumBoardAdapter(this, (windowSize.x * 0.86).toInt())
        boardAdapter.setItems(project.columns)

        boardView = findViewById(R.id.board_view)
        boardView.setBoardAdapter(boardAdapter)
        boardView.setBoardViewListener(this)
        boardView.listRecyclerView.setScrollingTouchSlop(RecyclerView.TOUCH_SLOP_PAGING)
        boardView.listRecyclerView.contentDescription = "column recycler"

        val listItemDecoration = BobBoardSimpleDividers((windowSize.x * 0.03).toInt(), BobBoardSimpleDividersOrientation.HORIZONTAL)
        boardView.listRecyclerView.addItemDecoration(listItemDecoration)

        val toolbar = findViewById<Toolbar>(R.id.my_toolbar)
        setSupportActionBar(toolbar)
        styleToolbarContent(toolbar, Color.parseColor("#bbbbbb"))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onListDragStarted(listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>) {
        currentDragOperation.startListDrag(listViewHolder.adapterPosition)
    }

    override fun onCardDragStarted(listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>, cardViewHolder: BobBoardListAdapter.CardViewHolder) {
        currentDragOperation.startCardDrag(listViewHolder.adapterPosition, cardViewHolder.adapterPosition)
    }

    override fun onCardDragEnded(listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>?, cardViewHolder: BobBoardListAdapter.CardViewHolder?) {
        Toast.makeText(this, "Card moved from col: ${currentDragOperation.startingListIndex}, " +
                "row: ${currentDragOperation.startingCardIndex} to col: ${currentDragOperation.listIndex}, " +
                "row: ${currentDragOperation.cardIndex}", Toast.LENGTH_LONG).show()
        if (currentDragOperation.dragType == BobBoardView.DragType.CARD && currentDragOperation.orphaned) {
            val viewHolder = boardView.listRecyclerView.findViewHolderForAdapterPosition(currentDragOperation.listIndex) as ScrumBoardAdapter.ScrumListViewHolder
            val listAdapter = viewHolder.listAdapter
            listAdapter.insertItem(currentDragOperation.cardIndex, currentDragOperation.cardItem!!, false)

            if (currentDragOperation.cardIndex == 0 || currentDragOperation.cardIndex == listAdapter.itemCount-1) {
                viewHolder.columnRecyclerView.scrollToPosition(currentDragOperation.cardIndex)
            }

            project.columns[currentDragOperation.listIndex].userStories.add(currentDragOperation.cardIndex, currentDragOperation.cardItem!!)
        } else {
            val cardViewHolder = cardViewHolder as ScrumColumnAdapter.ScrumUserStoryViewHolder
            cardViewHolder.overlay.visibility = View.GONE
        }
        currentDragOperation.reset()
    }

    override fun onListDragEnded(listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>?) {
        listViewHolder?.itemView?.visibility = View.VISIBLE
        Toast.makeText(this, "List moved from col: ${currentDragOperation.startingListIndex} " +
                "to col: ${currentDragOperation.listIndex}", Toast.LENGTH_LONG).show()
        currentDragOperation.reset()
    }

    override fun onCardDragExitedList(listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>, cardViewHolder: BobBoardListAdapter.CardViewHolder) {
        val fromIndex = cardViewHolder.adapterPosition
        currentDragOperation.listIndex = listViewHolder.adapterPosition
        currentDragOperation.cardIndex = cardViewHolder.adapterPosition

        (listViewHolder as ScrumBoardAdapter.ScrumListViewHolder).listAdapter.removeItem(fromIndex)
        val userStory = project.columns[listViewHolder.getAdapterPosition()].userStories.removeAt(fromIndex)
        currentDragOperation.cardItem = userStory
        currentDragOperation.orphaned = true
    }

    override fun onListMove(fromPosition: Int, toPosition: Int) {
        boardAdapter.swapItem(fromPosition, toPosition)
        Collections.swap(project.columns, fromPosition, toPosition)
        currentDragOperation.listIndex = toPosition
    }

    override fun onCardMove(listViewHolder: BobBoardAdapter.ListViewHolder<out BobBoardListAdapter<*>>,
                            listIndex: Int, fromPosition: Int, toPosition: Int) {
        var viewHolder = listViewHolder as ScrumBoardAdapter.ScrumListViewHolder
        viewHolder.columnAdapter.swapItems(fromPosition, toPosition)
        Collections.swap(project.columns[listIndex].userStories, fromPosition, toPosition)
        currentDragOperation.cardIndex = toPosition
    }

    override fun onCardMoveList(toViewHolder: BobBoardAdapter.ListViewHolder<out BobBoardListAdapter<*>>,
                                toIndex: Int) {
        val viewHolder = toViewHolder as ScrumBoardAdapter.ScrumListViewHolder
        viewHolder.listAdapter.insertItem(toIndex, currentDragOperation.cardItem!!)

        if (toIndex == 0 || toIndex == viewHolder.listAdapter.itemCount-1) {
            toViewHolder.columnRecyclerView.scrollToPosition(toIndex)
        }

        project.columns[toViewHolder.adapterPosition].userStories.add(toIndex, currentDragOperation.cardItem!!)

        currentDragOperation.orphaned = false
        currentDragOperation.listIndex = toViewHolder.adapterPosition
        currentDragOperation.cardIndex = toIndex
    }


    override fun onCardDragEnteredList(previousListViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>?, listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>, toIndex: Int) {
    }

    override fun onListDragEnteredBoardView(listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>?, overIndex: Int): Boolean {
        Log.d("TEST", "list entered board view over $overIndex")
        return true
    }

    override fun onListDragExitedBoardView(listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>): Boolean {
        Log.d("TEST", "list exited board view")
        return false
    }

    override fun canCardDropInList(listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        return true
    }

    override fun canListDropOver(listViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>, otherListViewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>): Boolean {
        return true
    }
}
