package org.bobstuff.bobboardview

import android.content.ClipData
import android.os.Build
import android.util.Log
import android.view.View
import java.util.*

/**
 * Created by bob
 */

abstract class BobBoardListArrayAdapter<T : BobBoardListAdapter.CardViewHolder, V, X>(
        cardEventCallbacks: BobBoardListAdapter.CardEventCallbacks, val dragOperation: BobBoardDragOperation<V, X>):
        BobBoardListAdapter<T>(cardEventCallbacks) {

    val cards: MutableList<V> = mutableListOf()

    fun startDrag(holder: BobBoardListAdapter.CardViewHolder,
                  dragShadowBuilder: View.DragShadowBuilder, x: Float, y: Float) {
        cardEventCallbacks.cardSelectedForDrag(holder, x, y)

        val data = ClipData.newPlainText("", "")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            holder.itemView.startDragAndDrop(data, dragShadowBuilder, null, 0)
        } else {
            @Suppress("DEPRECATION")
            holder.itemView.startDrag(data, dragShadowBuilder, null, 0)
        }
    }

    fun setItems(items: List<V>) {
        cards.clear()
        cards.addAll(items)
        notifyDataSetChanged()
    }

    fun removeItem(index: Int): V {
        val item = cards.removeAt(index)
        notifyItemRemoved(index)
        return item
    }

    open fun swapItems(fromPosition: Int, toPosition: Int) {
        if (Math.abs(fromPosition - toPosition) > 1) {
            val min = Math.min(fromPosition, toPosition)
            val max = Math.max(fromPosition, toPosition)
            val gap = toPosition - fromPosition
            Collections.rotate(cards.subList(min, max+1), toPosition - fromPosition)
            for (i in min..max) {
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
                notifyItemMoved(i, end)
            }
        } else {
            Collections.swap(cards, toPosition, fromPosition)
            notifyItemMoved(toPosition, fromPosition)
        }
    }

    fun insertItem(index: Int, card: V) {
        this.insertItem(index, card, false)
    }

    fun insertItem(index: Int, card: V, isCardAddedDuringDrag: Boolean) {
        cards.add(index, card)
        this.isCardAddedDuringDrag = isCardAddedDuringDrag
        if (isCardAddedDuringDrag) {
            this.addedCardId = getItemId(index)
        }
        notifyItemInserted(index)
    }

    override fun getItemId(position: Int): Long {
        return cards[position]?.hashCode()?.toLong() ?: -1
    }

    override fun getItemCount(): Int {
        return cards.size
    }

    override fun onViewDetachedFromWindow(holder: T) {
        if (holder.itemId == dragOperation.cardId) {
            isCardAddedDuringDrag = true
            addedCardId = dragOperation.cardId
        }
        super.onViewDetachedFromWindow(holder)
    }
}