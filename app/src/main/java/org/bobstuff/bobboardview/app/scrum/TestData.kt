package org.bobstuff.bobboardview.app.scrum

import org.bobstuff.bobboardview.R

/**
 * Created by bob
 */

enum class Priority { LOW, MEDIUM, HIGH }

data class UserStory(val description: String, val id: String, val assignee: String, val points: Int, val priority: Priority, val image: Int?)

data class Column(val description: String, val userStories: MutableList<UserStory>)

data class Project(val name: String, val columns: List<Column>)

public fun generateTestData(): Project {
    val story1 = UserStory(description = "Investigate how to make a scrum board",
            id = "BOB-12",
            assignee = "link to image",
            points = 5,
            priority = Priority.LOW,
            image = null)

    val story2 = UserStory(description = "Make the world a better place by solving all it's problems",
            id = "BOB-1",
            assignee = "link to image",
            points = 99,
            priority = Priority.HIGH,
            image = null)

    val story3 = UserStory(description = "Make a cup of tea",
            id = "BOB-2",
            assignee = "link to image",
            points = 1,
            priority = Priority.HIGH,
            image = null)

    val story4 = UserStory(description = "Story with a bunch of text and an image of awesomeness just for something different",
            id = "BOB-2",
            assignee = "link to image",
            points = 1,
            priority = Priority.HIGH,
            image = R.drawable.landscape_image)

    val story5 = UserStory(description = "Description for story 20",
            id = "BOB-20",
            assignee = "link to image",
            points = 1,
            priority = Priority.HIGH,
            image = null)

    val story6 = UserStory(description = "Description for story 21",
            id = "BOB-21",
            assignee = "link to image",
            points = 1,
            priority = Priority.MEDIUM,
            image = null)

    val story7 = UserStory(description = "Description for story 22",
            id = "BOB-22",
            assignee = "link to image",
            points = 1,
            priority = Priority.MEDIUM,
            image = null)

    val story8 = UserStory(description = "Description for story 23",
            id = "BOB-23",
            assignee = "link to image",
            points = 1,
            priority = Priority.HIGH,
            image = null)

    val story9 = UserStory(description = "Description for story 24",
            id = "BOB-24",
            assignee = "link to image",
            points = 1,
            priority = Priority.HIGH,
            image = null)

    val column1 = Column("TODO", mutableListOf(story1, story2, story3, story5, story6, story7, story8, story9))
    val column2 = Column("IN PROGRESS", mutableListOf(story4))
    val column3 = Column("REVIEW", mutableListOf())
    val column4 = Column("TESTING", mutableListOf())
    val column5 = Column("COMPLETE", mutableListOf())
    val column6 = Column("DONE", mutableListOf())

    return Project("Create a scrum board", listOf(column1, column2, column3, column4, column5, column6))
}