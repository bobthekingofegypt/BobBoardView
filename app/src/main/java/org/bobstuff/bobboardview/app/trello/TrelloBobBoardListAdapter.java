package org.bobstuff.bobboardview.app.trello;

import android.content.ClipData;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.bobstuff.bobboardview.OnLongTouchHandlerCallback;
import org.bobstuff.bobboardview.R;
import org.bobstuff.bobboardview.BobBoardListAdapter;
import org.bobstuff.bobboardview.LongTouchHandler;
import org.bobstuff.bobboardview.app.trello.model.Card;
import org.bobstuff.bobboardview.app.util.SimpleShadowBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by bob on 17/01/18.
 */

public class TrelloBobBoardListAdapter extends BobBoardListAdapter<TrelloBobBoardListAdapter.TrelloCardViewHolder> {
    public static class TrelloCardViewHolder extends BobBoardListAdapter.CardViewHolder {
        public final TextView mTitle;

        public TrelloCardViewHolder(View view) {
            super(view);

            mTitle = view.findViewById(R.id.title);
        }
    }

    private List<Card> mCards;
    private boolean mAddingView;
    private Context mContext;

    public TrelloBobBoardListAdapter(Context context, CardEventCallbacks cardEventCallbacks) {
        super(cardEventCallbacks);
        mContext = context;
        mCards = new ArrayList<>();
    }

    public void setCards(List<Card> cards) {
        this.mCards.clear();
        this.mCards.addAll(cards);
        this.notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mCards.size();
    }

    @Override
    public void onBindViewHolder(final TrelloBobBoardListAdapter.TrelloCardViewHolder viewHolder, int position) {
        Card card = mCards.get(position);
        viewHolder.mTitle.setText(card.getTitle());

        viewHolder.itemView.setOnTouchListener(new LongTouchHandler(mContext, new OnLongTouchHandlerCallback() {
            @Override
            public void onLongPress(@NotNull MotionEvent event) {
                getCardEventCallbacks().cardSelectedForDrag(viewHolder, event.getX(), event.getY());
                ClipData data = ClipData.newPlainText("", "");
                View.DragShadowBuilder shadowBuilder = new SimpleShadowBuilder(viewHolder.itemView, 1f, 1f, event.getX(), event.getY());
                viewHolder.itemView.startDrag(data, shadowBuilder, null, 0);
                viewHolder.itemView.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onClick(@NotNull MotionEvent e) {
            }
        }));
    }

    public Card delete(int position) {
        Card card = mCards.remove(position);
        notifyItemRemoved(position);
        return card;
    }

    public void add(int position, Card card) {
        mCards.add(position, card);
        setCardAddedDuringDrag(true);
        notifyItemInserted(position);
    }

    @Override
    public TrelloBobBoardListAdapter.TrelloCardViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).
                inflate(R.layout.item_card, viewGroup, false);
        return new TrelloBobBoardListAdapter.TrelloCardViewHolder(view);
    }

    public boolean onItemMove(int fromPosition, int toPosition) {
        Collections.swap(mCards, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }
}
