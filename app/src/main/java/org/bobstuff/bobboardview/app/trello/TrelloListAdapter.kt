package org.bobstuff.bobboardview.app.trello

import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.bobstuff.bobboardview.*

import org.bobstuff.bobboardview.app.R
import org.bobstuff.bobboardview.app.trello.model.BoardList
import org.bobstuff.bobboardview.app.trello.model.Card
import org.bobstuff.bobboardview.app.util.SimpleShadowBuilder

/**
 * Created by bob
 */

class TrelloListAdapter(
        private val mContext: Context,
        dragOperation: BobBoardDragOperation<Card, BoardList>,
        cardEventCallbacks: BobBoardListAdapter.CardEventCallbacks
) : BobBoardListArrayAdapter<TrelloListAdapter.TrelloCardViewHolder, Card, BoardList>(cardEventCallbacks, dragOperation) {

    override fun getItemId(position: Int): Long {
        return cards[position].id.hashCode().toLong()
    }

    override fun onBindViewHolder(viewHolder: TrelloListAdapter.TrelloCardViewHolder, position: Int) {
        val card = cards[position]
        viewHolder.mTitle.text = card.title
        viewHolder.itemView.visibility = View.VISIBLE

        viewHolder.itemView.setOnTouchListener(LongTouchHandler(mContext, object : OnLongTouchHandlerCallback {
            override fun onLongPress(event: MotionEvent) {
                val shadowBuilder = SimpleShadowBuilder(viewHolder.itemView, 1.0, 1f, event.x, event.y)
                startDrag(viewHolder, shadowBuilder, event.x, event.y)
                cardEventCallbacks.cardSelectedForDrag(viewHolder, event.x, event.y)
                viewHolder.itemView.visibility = View.INVISIBLE
            }

            override fun onClick(e: MotionEvent) {}
        }))
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): TrelloListAdapter.TrelloCardViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.item_card, viewGroup, false)
        return TrelloListAdapter.TrelloCardViewHolder(view)
    }

    override fun onViewAttachedToWindow(viewHolder: TrelloCardViewHolder) {
        if (isCardAddedDuringDrag && addedCardId == viewHolder.itemId) {
            viewHolder.itemView.visibility = View.INVISIBLE
        }
        super.onViewAttachedToWindow(viewHolder)
    }

    class TrelloCardViewHolder(view: View) : BobBoardListAdapter.CardViewHolder(view) {
        val mTitle: TextView = view.findViewById(R.id.title)
    }
}
