package org.bobstuff.bobboardview.app.trello;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by bob on 29/01/18.
 */

public class CustomDividers {
    public static class ListItemDecoration extends RecyclerView.ItemDecoration {
        private int mDividerSize;

        public ListItemDecoration(final int dividerSize) {
            this.mDividerSize = dividerSize;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            if (position == 0) {
                outRect.set(mDividerSize, 0, mDividerSize, 0);
            } else {
                outRect.set(0, 0, mDividerSize, 0);
            }
        }
    }

    public static class CardItemDecoration extends RecyclerView.ItemDecoration {
        private int mDividerSize;

        public CardItemDecoration(final int dividerSize) {
            this.mDividerSize = dividerSize;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            if (position == 0) {
                outRect.set(mDividerSize, mDividerSize, mDividerSize, mDividerSize);
            } else {
                outRect.set(mDividerSize, 0, mDividerSize, mDividerSize);
            }
        }
    }
}
