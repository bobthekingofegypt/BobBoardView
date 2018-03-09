package org.bobstuff.bobboardview.app.trello

import android.graphics.Point
import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.support.test.uiautomator.By
import android.support.test.uiautomator.UiDevice
import android.support.test.uiautomator.UiObject2
import android.util.Log
import android.view.ViewConfiguration
import com.facebook.testing.screenshot.Screenshot
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.contrib.RecyclerViewActions
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.uiautomator.Direction
import android.support.v7.widget.RecyclerView
import org.bobstuff.bobboardview.BobBoardView
import org.bobstuff.bobboardview.app.R


/**
 * These tests are a little flaky when it comes to different devices.  They are really only meant
 * for me to verify certain features continue to work as I refactor the library a bit.  Technically
 * they will work on other devices but the result from one device to another will vary as screen size
 * varies.  Not that they will be wrong but the final outcome will look different.
 *
 * Created by bob
 */
@RunWith(AndroidJUnit4::class)
class BoardActivityInstrumentedTest {
    @JvmField
    @Rule
    var activityRule = ActivityTestRule(BoardActivity::class.java)
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
    fun ensureDraggedViewReappearsOnQuickTap() {
        /**
         * During testing it was noticed that if you set the list to start drag on touch down a very
         * quick touch up would sometimes cancel the drag before it starts.  You fix this by listening
         * to the return value from startDrag and not progressing boardview when it fails
         */
        val dragView = todoList()

        val x = dragView.visibleCenter.x
        val y = dragView.visibleCenter.y
        device.swipe(x, y, x, y, 0)

        TimeUnit.MILLISECONDS.sleep(600)

        Screenshot.snapActivity(activityRule.activity)
                .record()
    }

    @Test
    fun ensureQuicklyMovingListInAndOutDoesntCrashOrBreakOrder() {
        /**
         * this tests a problem that occurs when you cause all entries in the recycler to be animating
         * at the same time.  This makes them return a position of -1.  We use a lots of tricks to
         * avoid this when it happens so hopefully we end up where we should be without a crash
         */
        val dragView = todoList()

        val edgeX = boardView.visibleBounds.right
        val x = dragView.visibleCenter.x
        val y = dragView.visibleCenter.y
        val tp = toolbarView.visibleCenter

        val points = mutableListOf<Point>()
        points.addAll(MutableList(4) {_ -> Point(x,y)})
        addAllPoints(points,
                Point(edgeX, y),
                Point(edgeX, tp.y),
                Point(tp.x, tp.y),
                Point(1, tp.y),
                Point(1, y))

        device.swipe(points.toTypedArray(), 5)

        TimeUnit.MILLISECONDS.sleep(1600)

        Screenshot.snapActivity(activityRule.activity)
                .record()
    }

    @Test
    fun ensureQuicklyMovingListInAndOutDoesntCrashOrBreakOrderWhenRepeated() {
        /**
         * this is the same as its not repeated partner but here we keep the drag going and go round
         * and round, this is also at a inhuman speed to really push the library
         */
        val dragView = todoList()

        val edgeX = boardView.visibleBounds.right
        val x = dragView.visibleCenter.x
        val y = dragView.visibleCenter.y
        val tp = toolbarView.visibleCenter

        val points = mutableListOf<Point>()
        points.addAll(MutableList(4) {_ -> Point(x,y)})
        val rotation = mutableListOf<Point>(Point(edgeX, y),
                Point(edgeX, tp.y),
                Point(tp.x, tp.y),
                Point(1, tp.y),
                Point(1, y))
        points.addAll(rotation)
        points.addAll(rotation)
        points.addAll(rotation)
        points.addAll(rotation)

        device.swipe(points.toTypedArray(), 5)

        TimeUnit.MILLISECONDS.sleep(600)

        Screenshot.snapActivity(activityRule.activity)
                .record()
    }

    @Test
    fun ensureSimpleDragMoveAndBackWorks() {
        val longPressTimeout = ViewConfiguration.getLongPressTimeout()

        val dragView = todoList()
        val p = dragView.visibleCenter
        val points = mutableListOf(
                        dragView.visibleCenter,
                        dragView.visibleCenter,
                        Point(dragView.visibleBounds.right + 20, p.y),
                        Point(dragView.visibleBounds.left, p.y),
                        Point(dragView.visibleBounds.left, p.y))

        val steps = longPressTimeout / 5

        device.swipe(points.toTypedArray(), steps * 2)

        device.waitForIdle(5000)
        TimeUnit.MILLISECONDS.sleep(600)

        Screenshot.snapActivity(activityRule.activity)
                .record()
    }

    @Test
    fun ensureSimpleDragMoveWorks() {
        //TODO this test is flawed, worse than the others.  If on a big/small device because the
        //columns are fixed size this move can happen with or without triggering autoscroll.
        //technically the screenshot should be fine but eye checking the screenshots is hard as
        //you don't know where it should be, but a stored verified shot would be fine.
        // Example on nexus4 this kind of movement can move column 0 to 1 but not on say a pixel2
        val longPressTimeout = ViewConfiguration.getLongPressTimeout()

        val dragView = todoList()
        val p = dragView.visibleCenter
        val points = mutableListOf(
                dragView.visibleCenter,
                dragView.visibleCenter,
                Point(dragView.visibleBounds.right + 20, p.y),
                Point(dragView.visibleBounds.right + 20, p.y))

        val steps = longPressTimeout / 5

        device.swipe(points.toTypedArray(), steps * 2)

        TimeUnit.MILLISECONDS.sleep(600)

        Screenshot.snapActivity(activityRule.activity)
                .record()
    }

    @Test
    fun ensureDragSecondBottomOptionOverBottomDoesntCrash() {
        val boardView: BobBoardView = activityRule.activity.findViewById(R.id.board_view)
        val vh = boardView.listRecyclerView.findViewHolderForAdapterPosition(0)
        val recyclerView = vh.itemView.findViewById<RecyclerView>(R.id.card_recycler)
        val itemCount = recyclerView.adapter.itemCount

        Espresso.onView(ViewMatchers.withContentDescription("todo cards"))
                .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(itemCount - 1))

        TimeUnit.MILLISECONDS.sleep(600)

        val item = device.findObject(By
                .text("Filler 11"))

        val destinationItem = device.findObject(By
                .text("Filler 12"))

        val p = item.visibleCenter
        val d = destinationItem.visibleCenter
        val db = destinationItem.visibleBounds

        val points = mutableListOf<Point>()
        points.addAll(MutableList(15) {_ -> item.visibleCenter})
        val sp = arrayOf(
                Point(p.x, d.y + db.height()+30),
                Point(p.x, d.y + db.height()+30),
                Point(p.x, d.y),
                Point(p.x, d.y),
                Point(p.x, d.y + db.height()+30),
                Point(p.x, d.y + db.height()+30),
                Point(p.x, d.y),
                Point(p.x, d.y))
        points.addAll(sp)
        points.addAll(sp)
        points.addAll(sp)
        points.addAll(sp)
        points.addAll(sp)
        device.swipe(points.toTypedArray(), 35)

        TimeUnit.MILLISECONDS.sleep(600)

        Screenshot.snapActivity(activityRule.activity)
                .record()
    }

    @Test
    fun ensureDragFirstCardOptionOutRoundAndInTopSetsAsFirst() {
        val item = device.findObject(By
                .text("Infinite Scrolling"))

        val r = todoListRecycler()

        val p = item.visibleCenter
        val pb = item.visibleBounds

        val points = mutableListOf<Point>()
        points.addAll(MutableList(15) {_ -> item.visibleCenter})
        val sp = arrayOf(
                Point(pb.right+30, p.y),
                Point(pb.right+30, p.y),
                Point(pb.right+30, r.visibleBounds.top - 20),
                Point(pb.right+30, r.visibleBounds.top - 20),
                Point(p.x, r.visibleBounds.top- 20),
                Point(p.x, r.visibleBounds.top - 20),
                Point(p.x, r.visibleBounds.top+10),
                Point(p.x, r.visibleBounds.top+10))
        points.addAll(sp)
        device.swipe(points.toTypedArray(), 35)

        TimeUnit.MILLISECONDS.sleep(600)

        Screenshot.snapActivity(activityRule.activity)
                .record()
    }

    @Test
    fun ensureDragSecondBottomOptionOutRoundAndInBottomSetsAsLast() {
        val boardView: BobBoardView = activityRule.activity.findViewById(R.id.board_view)
        val vh = boardView.listRecyclerView.findViewHolderForAdapterPosition(0)
        val recyclerView = vh.itemView.findViewById<RecyclerView>(R.id.card_recycler)
        val itemCount = recyclerView.adapter.itemCount

        Espresso.onView(ViewMatchers.withContentDescription("todo cards"))
                .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(itemCount - 1))

        TimeUnit.MILLISECONDS.sleep(600)

        val item = device.findObject(By
                .text("Filler 11"))

        val destinationItem = device.findObject(By
                .text("Filler 12"))

        val r = todoListRecycler()

        val p = item.visibleCenter
        val pb = item.visibleBounds
        val d = destinationItem.visibleCenter
        val db = destinationItem.visibleBounds

        val points = mutableListOf<Point>()
        points.addAll(MutableList(15) {_ -> item.visibleCenter})
        val sp = arrayOf(
                Point(pb.right+30, p.y),
                Point(pb.right+30, p.y),
                Point(pb.right+30, r.visibleBounds.bottom + 20),
                Point(pb.right+30, r.visibleBounds.bottom + 20),
                Point(p.x, r.visibleBounds.bottom + 20),
                Point(p.x, r.visibleBounds.bottom + 20),
                Point(p.x, r.visibleBounds.bottom-10),
                Point(p.x, r.visibleBounds.bottom-10))
        points.addAll(sp)
        device.swipe(points.toTypedArray(), 35)

        TimeUnit.MILLISECONDS.sleep(600)

        Screenshot.snapActivity(activityRule.activity)
                .record()
    }

    @Test
    fun ensureDragFirstOverSecondItemSwaps() {
        val item = device.findObject(By
                .text("Infinite Scrolling"))

        val destinationItem = device.findObject(By
                .text("Independent controller logic"))

        val p = item.visibleCenter
        val d = destinationItem.visibleCenter
        val db = destinationItem.visibleBounds

        val points = mutableListOf<Point>()
        points.addAll(MutableList(10) {_ -> item.visibleCenter})
        val sp = arrayOf(
                Point(p.x, d.y + db.height()),
                Point(p.x, d.y + db.height()))
        points.addAll(sp)
        device.swipe(points.toTypedArray(), 35)

        TimeUnit.MILLISECONDS.sleep(600)

        Screenshot.snapActivity(activityRule.activity)
                .record()
    }

    @Test
    fun ensureDragToEndAndBackDoesntDuplicateItem() {
        val item = device.findObject(By
                .text("Infinite Scrolling"))

        val destinationItem = device.findObject(By
                .text("Independent controller logic"))

        var w = device.displayWidth
        val p = item.visibleCenter
        val d = destinationItem.visibleCenter

        val points = mutableListOf<Point>()
        points.addAll(MutableList(10) {_ -> item.visibleCenter})
        val sp = arrayOf(
                Point(w-10, p.y),
                Point(w-10, p.y))
        for (i in 1..20) points.addAll(sp)
        val rsp = arrayOf(
                Point(10, p.y),
                Point(10, p.y))
        for (i in 1..20) points.addAll(rsp)
        points.addAll(sp)
        points.add(d)
        device.swipe(points.toTypedArray(), 35)

        TimeUnit.MILLISECONDS.sleep(600)

        Screenshot.snapActivity(activityRule.activity)
                .record()
    }

    @Test
    fun ensureDragToEndAndBackAllowsReorderingStill() {
        val item = device.findObject(By
                .text("Infinite Scrolling"))

        val destinationItem = device.findObject(By
                .text("Independent controller logic"))

        var w = device.displayWidth
        val p = item.visibleCenter
        val d = destinationItem.visibleCenter

        val points = mutableListOf<Point>()
        points.addAll(MutableList(10) {_ -> item.visibleCenter})
        val sp = arrayOf(
                Point(w-10, p.y),
                Point(w-10, p.y))
        for (i in 1..20) points.addAll(sp)
        val rsp = arrayOf(
                Point(30, p.y),
                Point(30, p.y))
        for (i in 1..20) points.addAll(rsp)
        points.add(Point(d.x, d.y+200))
        device.swipe(points.toTypedArray(), 35)

        TimeUnit.MILLISECONDS.sleep(600)

        Screenshot.snapActivity(activityRule.activity)
                .record()
    }

    @Test
    fun ensureDropOnDeadColumnReturnsToPreviousLocation() {
        Espresso.onView(ViewMatchers.withContentDescription("column recycler"))
                .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(5))

        val item = device.findObject(By
                .text("Add room support"))

        val p = item.visibleCenter

        val points = mutableListOf<Point>()
        points.addAll(MutableList(10) {_ -> item.visibleCenter})
        val sp = arrayOf(
                Point(10, p.y),
                Point(10, p.y))
        for (i in 1..2) points.addAll(sp)
        device.swipe(points.toTypedArray(), 35)

        TimeUnit.MILLISECONDS.sleep(600)

        Screenshot.snapActivity(activityRule.activity)
                .record()
    }

    @Test
    fun ensureLeaveDeadColumnReturnsToPreviousLocationNotCrash() {
        Espresso.onView(ViewMatchers.withContentDescription("column recycler"))
                .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(5))

        val item = device.findObject(By
                .text("Add room support"))

        val p = item.visibleCenter

        val points = mutableListOf<Point>()
        points.addAll(MutableList(10) {_ -> item.visibleCenter})
        val sp = arrayOf(
                Point(10, p.y),
                Point(10, p.y))
        for (i in 1..2) points.addAll(sp)
        points.add(Point(10, p.y + 200))
        device.swipe(points.toTypedArray(), 35)

        TimeUnit.MILLISECONDS.sleep(600)

        Screenshot.snapActivity(activityRule.activity)
                .record()
    }

    @Test
    fun ensureArchiveDropRemovesList() {
        val dragView = todoList()

        val p = dragView.visibleCenter
        val points = mutableListOf(
                dragView.visibleCenter,
                dragView.visibleCenter,
                toolbarView.visibleCenter)

        device.swipe(points.toTypedArray(), 50)

        TimeUnit.MILLISECONDS.sleep(1600)

        Screenshot.snapActivity(activityRule.activity)
                .record()
    }

    @Test
    fun ensureEnterAndExitArchiveDoesntRemoveList() {
        val dragView = todoList()

        val p = dragView.visibleCenter
        val points = mutableListOf(
                dragView.visibleCenter,
                dragView.visibleCenter,
                toolbarView.visibleCenter,
                dragView.visibleCenter)

        device.swipe(points.toTypedArray(), 50)

        TimeUnit.MILLISECONDS.sleep(1600)

        Screenshot.snapActivity(activityRule.activity)
                .record()
    }

    @Test
    fun ensureOrphanedListViewDoestArchive() {
        val dragView = todoList()

        val points = mutableListOf(
                dragView.visibleCenter,
                dragView.visibleCenter,
                Point(10, toolbarView.visibleBounds.bottom - 20))

        device.swipe(points.toTypedArray(), 50)

        TimeUnit.MILLISECONDS.sleep(1600)

        Screenshot.snapActivity(activityRule.activity)
                .record()
    }

    @Test
    fun ensureListLeavingEdgeOfArchiveButtonUsesButtonLocationForReentryShouldJumpToIndex1() {
        val dragView = todoList()

        val points = mutableListOf(
                dragView.visibleCenter,
                dragView.visibleCenter,
                toolbarView.visibleCenter,
                toolbarView.visibleCenter,
                toolbarView.visibleCenter,
                toolbarView.visibleCenter,
                toolbarView.visibleCenter,
                toolbarView.visibleCenter,
                Point(toolbarView.visibleBounds.right - 10, toolbarView.visibleCenter.y))

        device.swipe(points.toTypedArray(), 50)

        TimeUnit.MILLISECONDS.sleep(1600)

        Screenshot.snapActivity(activityRule.activity)
                .record()
    }

    @Test
    fun ensureCardDropInArchiveDisappearsFromList() {
        val item = device.findObject(By
                .text("Infinite Scrolling"))

        var p = item.visibleCenter

        val points = mutableListOf<Point>()
        points.addAll(MutableList(10) {_ -> p})
        val sp = arrayOf(toolbarView.visibleCenter)
        for (i in 1..4) points.addAll(sp)
        device.swipe(points.toTypedArray(), 30)

        TimeUnit.MILLISECONDS.sleep(1600)

        Screenshot.snapActivity(activityRule.activity)
                .record()
    }

    @Test
    fun ensureCardOrphanedReturnsHome() {
        val item = device.findObject(By
                .text("Infinite Scrolling"))
        val todoList = todoList()

        var p = item.visibleCenter

        val points = mutableListOf<Point>()
        points.addAll(MutableList(10) {_ -> p})
        val sp = arrayOf(todoList.visibleCenter)
        for (i in 1..4) points.addAll(sp)
        device.swipe(points.toTypedArray(), 30)

        TimeUnit.MILLISECONDS.sleep(1600)

        Screenshot.snapActivity(activityRule.activity)
                .record()
    }

    @Test
    fun ensureScrollPositionMaintainedDuringReuse() {
        val boardView: BobBoardView = activityRule.activity.findViewById(R.id.board_view)
        val vh = boardView.listRecyclerView.findViewHolderForAdapterPosition(0)
        val recyclerView = vh.itemView.findViewById<RecyclerView>(R.id.card_recycler)
        val itemCount = recyclerView.adapter.itemCount

        Espresso.onView(ViewMatchers.withContentDescription("todo cards"))
                .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(itemCount - 1))

        Espresso.onView(ViewMatchers.withContentDescription("column recycler"))
                .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(6))

        TimeUnit.MILLISECONDS.sleep(500)

        Espresso.onView(ViewMatchers.withContentDescription("column recycler"))
                .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(0))

        TimeUnit.MILLISECONDS.sleep(500)
        Screenshot.snapActivity(activityRule.activity)
                .record()

    }

    private fun todoList(): UiObject2 {
        return device.findObject(By
                .text("todo")
                .clazz("android.widget.TextView"))
    }

    private fun todoListRecycler(): UiObject2 {
        return device.findObject(By
                .desc("todo cards"))
    }

    private fun inProgressList(): UiObject2 {
        return device.findObject(By
                .text("in progress")
                .clazz("android.widget.TextView"))
    }

    private fun addAllPoints(list: MutableList<Point>, vararg points: Point) {
        list.addAll(points)
    }
}