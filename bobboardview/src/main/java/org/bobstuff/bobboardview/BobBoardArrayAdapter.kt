package org.bobstuff.bobboardview

import android.content.ClipData
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.View
import android.view.ViewGroup
import java.util.*

/**
 * Created by bob
 */

// handle all functionality from trello and scrum adapters

abstract class BobBoardArrayAdapter<T : BobBoardAdapter.ListViewHolder<*>, X, V>(val dragOperation: BobBoardDragOperation<X, V>):
        BobBoardAdapter<T>() {

    val lists: MutableList<V> = mutableListOf()
    private val columnScrollPositions: MutableMap<Long, Parcelable> = mutableMapOf()

    override fun onSaveInstanceState(): Parcelable? {
        val bundle = Bundle()
        for ((key, value) in columnScrollPositions) {
            bundle.putParcelable(key.toString(), value)
        }
        return bundle
    }

    override fun onRestoreInstanceState(bundle: Bundle) {
        for (key in bundle.keySet()) {
            Log.d("TEST", "key ${key}")
            columnScrollPositions[key.toLong()] = bundle.getParcelable(key)
        }

        notifyDataSetChanged()
    }

    fun startDrag(holder: ListViewHolder<BobBoardListAdapter<*>>,
            dragShadowBuilder: View.DragShadowBuilder, x: Float, y: Float): Boolean {

        val data = ClipData.newPlainText("", "")
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            holder.itemView.startDragAndDrop(data, dragShadowBuilder, null, 0)
        } else {
            @Suppress("DEPRECATION")
            holder.itemView.startDrag(data, dragShadowBuilder, null, 0)
        }

        if (result) {
            boardView?.startListDrag(holder, x, y)
        }
        Log.d("TEST", "RESULT $result")
        return result
    }

    fun removeItem(index: Int): V {
        val item = lists.removeAt(index)
        notifyItemRemoved(index)
        return item
    }

    fun setItems(items: List<V>) {
        lists.clear()
        lists.addAll(items)
        notifyDataSetChanged()
    }

    fun swapItem(fromPosition: Int, toPosition: Int) {
        val gap = toPosition - fromPosition
        if (Math.abs(gap) > 1) {
            val min = Math.min(fromPosition, toPosition)
            val max = Math.max(fromPosition, toPosition)
            Collections.rotate(lists.subList(min, max+1), gap)
            for (i in min..max) {
                Log.d("TEST", "$i")
                var end = i
                for (j in 1..Math.abs(gap)) {
                    val step = if (gap < 0) { -1 } else 1
                    end = when {
                        end + step < min -> {
                            max
                        }
                        end + step <= max -> end + step
                        else -> {
                            min
                        }
                    }
                }
                Log.d("TEST", "end $end")
                notifyItemMoved(i, end)
            }
        } else {
            Collections.swap(lists, toPosition, fromPosition)
            notifyItemMoved(toPosition, fromPosition)
        }
    }

    fun insertItem(index: Int, boardList: V) {
        this.insertItem(index, boardList, false)
    }

    fun insertItem(index: Int, boardList: V, isListAddedDuringDrag: Boolean) {
        lists.add(index, boardList)
        this.isListAddedDuringDrag = isListAddedDuringDrag
        if (isListAddedDuringDrag) {
            this.addedListId = getItemId(index)
        }
        notifyItemInserted(index)
    }

    override fun getItemCount(): Int {
        return lists.size
    }

    override fun getItemId(position: Int): Long {
        return lists[position]?.hashCode()?.toLong() ?: -1
    }

    override fun onBindViewHolder(holder: T, position: Int) {
        val key = getItemId(position)
        Log.d("TEST", "rebind, columns scroll positions for model ${columnScrollPositions.containsKey(key)}")
        if (columnScrollPositions.containsKey(key)) {
            holder.recyclerView?.layoutManager?.onRestoreInstanceState(columnScrollPositions[key])
        }
    }

    override fun onViewRecycled(holder: T) {
        val index = holder.adapterPosition
        if (index != -1) {
            holder.recyclerView?.run {
                columnScrollPositions[getItemId(index)] =
                        layoutManager!!.onSaveInstanceState()!!
            }
        }

        super.onViewRecycled(holder)
    }

    override fun onViewDetachedFromWindow(holder: T) {
        if (holder.itemId == dragOperation.listId) {
            isListAddedDuringDrag = true
            addedListId = dragOperation.listId
        }
        super.onViewDetachedFromWindow(holder)
    }

}