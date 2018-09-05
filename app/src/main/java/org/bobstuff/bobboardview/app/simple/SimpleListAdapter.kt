package org.bobstuff.bobboardview.app.simple

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.bobstuff.bobboardview.*
import org.bobstuff.bobboardview.app.R
import org.bobstuff.bobboardview.app.util.SimpleShadowBuilder

/**
 * Created by bob
 */
class SimpleListAdapter(val context: Context, dragOperation: BobBoardDragOperation<Card, BoardList>, cardEventCallbacks: BobBoardListAdapter.CardEventCallbacks):
        BobBoardListArrayAdapter<SimpleListAdapter.SimpleCardViewHolder, Card, BoardList>(cardEventCallbacks, dragOperation) {
    var columnContainingDragTargetRedisplaying = false

    override fun onBindViewHolder(holder: SimpleCardViewHolder, position: Int) {
        val card = cards[position]
        holder.overlay.visibility = View.INVISIBLE
        holder.description.text = card.description
        holder.itemView.setOnTouchListener(LongTouchHandler(context, object : OnLongTouchHandlerCallback {
            override fun onClick(e: MotionEvent) {
                //no-op
            }

            override fun onLongPress(event: MotionEvent) {
                val shadowBuilder = SimpleShadowBuilder(holder.itemView, 1.0, 1.0f, event.x, event.y)
                startDrag(holder, shadowBuilder, event.x, event.y)
                holder.overlay.visibility = View.VISIBLE
            }
        }))

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleCardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_simple_card, parent, false)
        return SimpleCardViewHolder(view)
    }



    override fun onViewAttachedToWindow(viewHolder: SimpleCardViewHolder) {
        if (columnContainingDragTargetRedisplaying && viewHolder.adapterPosition == dragOperation.cardIndex) {
            viewHolder.overlay.visibility = View.VISIBLE
            cardEventCallbacks.cardMovedDuringDrag(viewHolder, false)
            columnContainingDragTargetRedisplaying = false
        }
        if (isCardAddedDuringDrag && addedCardId == viewHolder.itemId) {
            viewHolder.overlay.visibility = View.VISIBLE
        }

        super.onViewAttachedToWindow(viewHolder)
    }

    inner class SimpleCardViewHolder(val view: View): BobBoardListAdapter.CardViewHolder(view) {
        var overlay: View = view.findViewById(R.id.overlay)
        val description: TextView = view.findViewById(R.id.description)
    }
}