package org.bobstuff.bobboardview.app.trello.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bob on 16/01/18.
 */

public class BoardList {
    private String title;
    private List<Card> cards;

    public BoardList(String title) {
        this(title, new ArrayList<Card>());
    }

    public BoardList(final String title, final List<Card> cards) {
        this.title = title;
        this.cards = cards;
    }

    public String getTitle() {
        return title;
    }

    public List<Card> getCards() {
        return cards;
    }

}
