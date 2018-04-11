package org.bobstuff.bobboardview.app.trello.model;

/**
 * Created by bob
 */

public class Card {
    private String id;
    private String title;

    public Card(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public String getId() {
        return id;
    }
}
