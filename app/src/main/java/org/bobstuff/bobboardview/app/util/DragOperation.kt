package org.bobstuff.bobboardview.app.util

import org.bobstuff.bobboardview.BobBoardView

/**
 * Created by bob
 */

class DragOperation<T, V> {
    var dragType: BobBoardView.DragType = BobBoardView.DragType.NONE
    var startingListIndex: Int = -1
    var startingCardIndex: Int = -1
    var listIndex: Int = -1
    var cardIndex: Int = -1
    var cardItem: T? = null
    var listItem: V? = null
    var orphaned: Boolean = false

    fun startListDrag(listIndex: Int) {
        this.dragType = BobBoardView.DragType.LIST
        this.startingListIndex = listIndex
        this.listIndex = listIndex
    }

    fun startCardDrag(listIndex: Int, cardIndex: Int) {
        this.dragType = BobBoardView.DragType.CARD
        this.startingListIndex = listIndex
        this.startingCardIndex = cardIndex
        this.listIndex = listIndex
        this.cardIndex = cardIndex
    }

    fun reset() {
        startingListIndex = -1
        startingCardIndex = -1
        listIndex = -1
        cardIndex = -1
        cardItem = null
        listItem = null
        orphaned = false
    }
}