package org.bobstuff.bobboardview.app.reuse

import com.thedeanda.lorem.LoremIpsum
import org.bobstuff.bobboardview.app.scrum.Column
import org.bobstuff.bobboardview.app.scrum.Priority
import org.bobstuff.bobboardview.app.scrum.Project
import org.bobstuff.bobboardview.app.scrum.UserStory
import java.util.*


/**
 * Created by bob
 */

public fun generateTestData(): Project {

    val random = Random(1234546)
    val lorem = LoremIpsum(123456L)

    val columns = mutableListOf<Column>()
    for (i in 1..30) {
        val stories = mutableListOf<UserStory>()
        for (j in 1..random.nextInt(50)+1) {
            val story = UserStory(description = lorem.getWords(random.nextInt(20)+1),
                    id = "BOB-$i$j",
                    assignee = "link to image",
                    points = 5,
                    priority = Priority.MEDIUM,
                    image = null)

            stories.add(story)
        }

        columns.add(Column("List $i", stories))
    }

    return Project("Create a scrum board", columns)
}