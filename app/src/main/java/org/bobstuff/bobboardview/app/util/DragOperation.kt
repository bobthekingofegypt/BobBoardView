package org.bobstuff.bobboardview.app.util

import org.bobstuff.bobboardview.BobBoardView

/**
 * Created by bob
 */

class DragOperation<T> {
    var dragType: BobBoardView.DragType = BobBoardView.DragType.NONE
    var listIndex: Int = -1
    var cardIndex: Int = -1
    var cardItem: T? = null
    var listItem: T? = null
    var orphaned: Boolean = false

    fun reset() {
        listIndex = -1
        cardIndex = -1
        cardItem = null
        listItem = null
        orphaned = false
    }
}