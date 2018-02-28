package org.bobstuff.bobboardview.app.trello.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bob on 16/01/18.
 */

public class Board {
    private final List<BoardList> lists;

    public Board() {
        this(new ArrayList<BoardList>());
    }

    public Board(final List<BoardList> lists) {
        this.lists = lists;
    }

    public List<BoardList> getLists() {
        return lists;
    }
}
