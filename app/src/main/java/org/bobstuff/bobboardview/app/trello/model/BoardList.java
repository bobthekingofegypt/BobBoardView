package org.bobstuff.bobboardview.app.trello.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bob
 */

public class BoardList {
    private String title;
    private List<Card> cards;
    private long id;

    public BoardList(long id, String title) {
        this(id, title, new ArrayList<>());
    }

    public BoardList(final Long id, final String title, final List<Card> cards) {
        this.id = id;
        this.title = title;
        this.cards = cards;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public List<Card> getCards() {
        return cards;
    }

}
