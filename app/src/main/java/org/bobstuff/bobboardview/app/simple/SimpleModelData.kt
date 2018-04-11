package org.bobstuff.bobboardview.app.simple

import org.bobstuff.bobboardview.app.R

/**
 * Created by bob
 */


data class Card(val description: String, val id: String)

data class BoardList(val uniqueId: Int, val description: String, val cards: MutableList<Card>)

data class Board(val name: String, val boardLists: MutableList<BoardList>)

fun generateTestData(): Board {
    val story1 = Card(description = "Investigate how to make a scrum board",
            id = "BOB-12")

    val story2 = Card(description = "Make the world a better place by solving all it's problems",
            id = "BOB-1")

    val story3 = Card(description = "Test data for the win",
            id = "BOB-2")

    val story4 = Card(description = "Lets do this thing",
            id = "BOB-3")

    val story5 = Card(description = "Lots of words that don't say anything but take up space",
            id = "BOB-4")

    val story6 = Card(description = "Something smart",
            id = "BOB-5")


    val boardList1 = BoardList(1, "List 1", mutableListOf(story1, story2))
    val boardList2 = BoardList(2, "List 2", mutableListOf(story3))
    val boardList3 = BoardList(3, "List 3", mutableListOf(story4, story5, story6))
    val boardList4 = BoardList(4, "List 4", mutableListOf())
    val boardList5 = BoardList(5, "List 5", mutableListOf())

    return Board("Simple Board", mutableListOf(boardList1, boardList2, boardList3, boardList4, boardList5))
}