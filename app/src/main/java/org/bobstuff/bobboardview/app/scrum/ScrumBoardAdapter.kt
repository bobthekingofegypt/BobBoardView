package org.bobstuff.bobboardview.app.scrum

import android.content.ClipData
import android.content.Context
import android.os.Parcelable
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import org.bobstuff.bobboardview.*
import org.bobstuff.bobboardview.app.R
import org.bobstuff.bobboardview.app.util.SimpleShadowBuilder
import java.util.*

/**
 * Created by bob on 21/02/18.
 */

class ScrumBoardAdapter(val context: Context, val width: Int):
        BobBoardAdapter<ScrumBoardAdapter.ScrumListViewHolder>() {
    private val columns: MutableList<Column> = mutableListOf()
    private val columnScrollPositions: MutableMap<Column, Parcelable> = mutableMapOf()
    private var dragListIndex: Int = -1
    private var dragCardIndex: Int = -1

    fun setDragIndex(listIndex: Int, cardIndex: Int) {
        this.dragListIndex = listIndex
        this.dragCardIndex = cardIndex
    }

    override fun getItemCount(): Int {
        return columns.size
    }

    override fun getItemId(position: Int): Long {
        return columns[position].hashCode().toLong()
    }

    override fun onViewRecycled(holder: ScrumListViewHolder) {
        val index = holder.adapterPosition
        columnScrollPositions[columns[index]] = holder.recyclerView.layoutManager.onSaveInstanceState()

        super.onViewRecycled(holder)
    }

    override fun onBindViewHolder(holder: ScrumListViewHolder, position: Int) {
        val column = columns[position]
        holder.descriptionTextView.text = column.description
        holder.recyclerView.contentDescription = "${column.description} cards"
        holder.columnAdapter.setItems(column.userStories)
        holder.columnAdapter.setDragCardIndex(-1)
        holder.itemView.setOnTouchListener(LongTouchHandler(context, object : OnLongTouchHandlerCallback {
            override fun onClick(e: MotionEvent) {
                //no-op
            }

            override fun onLongPress(event: MotionEvent) {
                boardView?.startListDrag(holder as ListViewHolder<BobBoardListAdapter<*>>, event.x, event.y)

                val data = ClipData.newPlainText("", "")
                val shadowBuilder = SimpleShadowBuilder(holder.itemView, 3.0, 0.95f, event.x, event.y)
                holder.itemView.startDrag(data, shadowBuilder, null, 0)
                holder.itemView.visibility = View.INVISIBLE
            }
        }))
        if (columnScrollPositions.containsKey(column)) {
            holder.recyclerView.layoutManager.onRestoreInstanceState(columnScrollPositions[column])
        }
        //if (position == dragListIndex) {
        //    holder.columnAdapter.setDragCardIndex(dragCardIndex)
        //}
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScrumListViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_scrum_column, parent, false)
        val params = view.layoutParams
        params.width = width
        view.layoutParams = params
        return ScrumListViewHolder(view)
    }

    fun setItems(items: List<Column>) {
        columns.clear()
        columns.addAll(items)
        notifyDataSetChanged()
    }

    fun swapItem(fromPosition: Int, toPosition: Int) {
        Collections.swap(columns, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onViewAttachedToWindow(holder: ScrumListViewHolder) {
        if (holder.adapterPosition == dragListIndex) {
            holder.columnAdapter.setDragCardIndex(dragCardIndex)
        }
        super.onViewAttachedToWindow(holder)
    }

    inner class ScrumListViewHolder(view: View): BobBoardAdapter.ListViewHolder<ScrumColumnAdapter>(view) {
        var columnRecyclerView: RecyclerView = view.findViewById(R.id.user_story_recycler)
        var descriptionTextView: TextView = view.findViewById(R.id.description)
        var columnAdapter: ScrumColumnAdapter =
                ScrumColumnAdapter(context, BobBoardListAdapter.DefaultCardEventCallbacks(
                        this@ScrumBoardAdapter, this))

        init {
            columnRecyclerView.adapter = columnAdapter
            val dividerSizeInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f, context.resources.displayMetrics)
            columnRecyclerView.addItemDecoration(BobBoardSimpleDividers(dividerSizeInPixels.toInt(), BobBoardSimpleDividersOrientation.VERTICAL))
            columnRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayout.VERTICAL, false)
        }

        override val listAdapter: ScrumColumnAdapter
            get() {
                return columnAdapter
            }

        override val recyclerView: RecyclerView
            get() {
                return columnRecyclerView
            }
    }
}
