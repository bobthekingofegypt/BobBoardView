package org.bobstuff.bobboardview.app.trello

import android.content.ClipData
import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import org.bobstuff.bobboardview.OnLongTouchHandlerCallback
import org.bobstuff.bobboardview.app.R
import org.bobstuff.bobboardview.BobBoardListAdapter
import org.bobstuff.bobboardview.LongTouchHandler
import org.bobstuff.bobboardview.app.trello.model.Card
import org.bobstuff.bobboardview.app.util.SimpleShadowBuilder

import java.util.ArrayList
import java.util.Collections

/**
 * Created by bob
 */

class TrelloListAdapter(
        private val mContext: Context,
        cardEventCallbacks: BobBoardListAdapter.CardEventCallbacks
) : BobBoardListAdapter<TrelloListAdapter.TrelloCardViewHolder>(cardEventCallbacks) {

    private val cards: MutableList<Card> = mutableListOf()

    fun setItems(cards: List<Card>) {
        this.cards.clear()
        this.cards.addAll(cards)
        this.notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return cards.size
    }

    fun removeItem(position: Int): Card {
        val card = cards.removeAt(position)
        notifyItemRemoved(position)
        return card
    }

    fun insertItem(position: Int, card: Card) {
        this.insertItem(position, card, true)
    }

    fun insertItem(position: Int, card: Card, isCardAddedDuringDrag: Boolean) {
        cards.add(position, card)
        this.isCardAddedDuringDrag = isCardAddedDuringDrag
        notifyItemInserted(position)
    }

    fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        Collections.swap(cards, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
        return true
    }

    override fun onBindViewHolder(viewHolder: TrelloListAdapter.TrelloCardViewHolder, position: Int) {
        val card = cards[position]
        viewHolder.mTitle.text = card.title
        viewHolder.itemView.visibility = View.VISIBLE

        viewHolder.itemView.setOnTouchListener(LongTouchHandler(mContext, object : OnLongTouchHandlerCallback {
            override fun onLongPress(event: MotionEvent) {
                cardEventCallbacks.cardSelectedForDrag(viewHolder, event.x, event.y)

                val data = ClipData.newPlainText("", "")
                val shadowBuilder = SimpleShadowBuilder(viewHolder.itemView, 1.0, 1f, event.x, event.y)
                viewHolder.itemView.startDrag(data, shadowBuilder, null, 0)
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
        if (isCardAddedDuringDrag) {
            viewHolder.itemView.visibility = View.INVISIBLE
        }
        super.onViewAttachedToWindow(viewHolder)
    }

    class TrelloCardViewHolder(view: View) : BobBoardListAdapter.CardViewHolder(view) {
        val mTitle: TextView = view.findViewById(R.id.title)
    }
}
