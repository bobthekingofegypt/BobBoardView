package org.bobstuff.bobboardview.app.trello

import android.graphics.Point
import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso
import android.support.test.espresso.contrib.RecyclerViewActions
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.support.test.uiautomator.By
import android.support.test.uiautomator.UiDevice
import android.support.test.uiautomator.UiObject2
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.ViewConfiguration
import com.facebook.testing.screenshot.Screenshot
import org.bobstuff.bobboardview.BobBoardView
import org.bobstuff.bobboardview.app.R
import org.bobstuff.bobboardview.app.reuse.ReuseSampleActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * Created by bob
 */
@RunWith(AndroidJUnit4::class)
class ReuseActivityInstrumentedTest {
    @JvmField
    @Rule
    var activityRule = ActivityTestRule(ReuseSampleActivity::class.java)
    lateinit var device: UiDevice
    lateinit var boardView: UiObject2
    lateinit var toolbarView: UiObject2

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.waitForIdle()

        boardView = device.findObject(By.desc("board_view"))
        toolbarView = device.findObject(By.desc("toolbar"))
    }

    @Test
    fun ensureScrollPositionMaintainedDuringReuse() {
        val boardView: BobBoardView = activityRule.activity.findViewById(R.id.board_view)
        val vh = boardView.listRecyclerView.findViewHolderForAdapterPosition(0)
        val recyclerView = vh.itemView.findViewById<RecyclerView>(R.id.user_story_recycler)
        val itemCount = recyclerView.adapter.itemCount

        Log.d("Item count is ", itemCount.toString())

        Espresso.onView(ViewMatchers.withContentDescription("List 1 cards"))
                .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(itemCount - 1))

        Espresso.onView(ViewMatchers.withContentDescription("column recycler"))
                .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(5))

        TimeUnit.MILLISECONDS.sleep(200)

        Espresso.onView(ViewMatchers.withContentDescription("column recycler"))
                .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(0))

        TimeUnit.MILLISECONDS.sleep(16000)

    }

    private fun addAllPoints(list: MutableList<Point>, vararg points: Point) {
        list.addAll(points)
    }
}