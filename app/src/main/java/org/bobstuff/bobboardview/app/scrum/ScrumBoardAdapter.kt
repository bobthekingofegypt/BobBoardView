package org.bobstuff.bobboardview.app.scrum

import android.content.ClipData
import android.content.Context
import android.os.Build
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

class ScrumBoardAdapter(val context: Context, val width: Int, dragOperation: BobBoardDragOperation<UserStory, Column>):
        BobBoardArrayAdapter<ScrumBoardAdapter.ScrumListViewHolder, UserStory, Column>(dragOperation)  {
    val viewPool = RecyclerView.RecycledViewPool()

    override fun getItemId(position: Int): Long {
        return lists[position].description.hashCode().toLong()
    }

    override fun onBindViewHolder(holder: ScrumListViewHolder, position: Int) {
        val column = lists[position]
        holder.descriptionTextView.text = column.description
        holder.recyclerView.contentDescription = "${column.description} cards"
        holder.columnAdapter.setItems(column.userStories)

        if (position == dragOperation.listIndex) {
            holder.columnAdapter.columnContainingDragTargetRedisplaying = true
        }
        holder.itemView.setOnTouchListener(LongTouchHandler(context, object : OnLongTouchHandlerCallback {
            override fun onClick(e: MotionEvent) {
                //no-op
            }

            override fun onLongPress(event: MotionEvent) {
                val shadowBuilder = SimpleShadowBuilder(holder.itemView, 3.0, 0.95f, event.x, event.y)
                startDrag(holder, shadowBuilder, event.x, event.y)
                holder.itemView.visibility = View.INVISIBLE
            }
        }))

        super.onBindViewHolder(holder, position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScrumListViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_scrum_column, parent, false)
        val params = view.layoutParams
        params.width = width
        view.layoutParams = params
        return ScrumListViewHolder(view)
    }

    inner class ScrumListViewHolder(view: View): BobBoardAdapter.ListViewHolder<ScrumColumnAdapter>(view) {
        var columnRecyclerView: RecyclerView = view.findViewById(R.id.user_story_recycler)
        var descriptionTextView: TextView = view.findViewById(R.id.description)
        var columnAdapter: ScrumColumnAdapter =
                ScrumColumnAdapter(context, dragOperation, BobBoardListAdapter.DefaultCardEventCallbacks(
                        this@ScrumBoardAdapter, this))

        init {
            columnRecyclerView.adapter = columnAdapter
            val dividerSizeInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f, context.resources.displayMetrics)
            columnRecyclerView.addItemDecoration(BobBoardSimpleDividers(dividerSizeInPixels.toInt(), BobBoardSimpleDividersOrientation.VERTICAL))
            columnRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayout.VERTICAL, false)
            columnRecyclerView.recycledViewPool = viewPool
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
