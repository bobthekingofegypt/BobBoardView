package org.bobstuff.bobboardview.app.simple

import android.content.ClipData
import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
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

/**
 * Created by bob
 */

class SimpleBoardAdapter(val context: Context, dragOperation: BobBoardDragOperation<Card, BoardList>,
                         val boardViewInteractionListener: BobBoardViewInteractionListener):
        BobBoardArrayAdapter<SimpleBoardAdapter.SimpleListViewHolderBase, Card, BoardList>(dragOperation) {
    val viewPool = RecyclerView.RecycledViewPool()

    override fun getItemId(position: Int): Long {
        if (position <= lists.size - 1) {
            return lists[position].uniqueId.toLong()
        }
        return 666
    }

    override fun getItemCount(): Int {
        return lists.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        if (position <= lists.size - 1) {
            return 0
        }
        return 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleListViewHolderBase {
        val sizeInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 300f, context.resources.displayMetrics)
        return when (viewType) {
            0 -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_simple_list, parent, false)
                val params = view.layoutParams
                params.width = sizeInPixels.toInt()
                view.layoutParams = params
                SimpleListViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_simple_add_list, parent, false)
                val params = view.layoutParams
                params.width = sizeInPixels.toInt()
                view.layoutParams = params
                SimpleNewListViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: SimpleListViewHolderBase, position: Int) {
        when(holder.itemViewType) {
            0 -> {
                val holder = holder as SimpleListViewHolder
                val list = lists[position]
                holder.descriptionTextView.text = list.description
                holder.simpleListAdapter.setItems(list.cards)
                if (position == dragOperation.listIndex) {
                    holder.listAdapter.columnContainingDragTargetRedisplaying = true
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
            }
            1 -> {
                val holder = holder as SimpleNewListViewHolder
                holder.newList.setOnClickListener {
                    boardViewInteractionListener.addNewList()
                }
            }
        }
    }


    abstract inner class SimpleListViewHolderBase(view: View): BobBoardAdapter.ListViewHolder<SimpleListAdapter>(view)

    inner class SimpleListViewHolder(view: View): SimpleListViewHolderBase(view) {
        var listRecyclerView: RecyclerView = view.findViewById(R.id.card_recycler)
        var descriptionTextView: TextView = view.findViewById(R.id.description)
        var overlayView: View = view.findViewById(R.id.overlay)
        var simpleListAdapter: SimpleListAdapter =
                SimpleListAdapter(context, dragOperation, BobBoardListAdapter.DefaultCardEventCallbacks(
                        this@SimpleBoardAdapter, this))

        init {
            listRecyclerView.adapter = simpleListAdapter
            val dividerSizeInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f, context.resources.displayMetrics)
            listRecyclerView.addItemDecoration(BobBoardSimpleDividers(dividerSizeInPixels.toInt(),
                    BobBoardSimpleDividersOrientation.VERTICAL))
            listRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayout.VERTICAL,
                    false)
            listRecyclerView.recycledViewPool = viewPool
        }

        override val listAdapter: SimpleListAdapter
            get() {
                return simpleListAdapter
            }

        override val recyclerView: RecyclerView
            get() {
                return listRecyclerView
            }
    }

    inner class SimpleNewListViewHolder(view: View) : SimpleListViewHolderBase(view) {
        override val recyclerView: RecyclerView? = null
        override var listAdapter: SimpleListAdapter? = null
        var newList: View = view.findViewById(R.id.new_list_button)
    }

    interface BobBoardViewInteractionListener {
        fun addNewList()
    }
}