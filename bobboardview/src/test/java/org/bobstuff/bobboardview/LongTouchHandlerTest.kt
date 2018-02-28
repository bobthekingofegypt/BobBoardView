package org.bobstuff.bobboardview

import android.app.Activity
import android.view.MotionEvent
import android.view.View
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowViewConfiguration
import java.util.concurrent.TimeUnit


/**
 * Created by bob
 */
@RunWith(RobolectricTestRunner::class)
class LongTouchHandlerTest {
    private val longPressTimeout = ShadowViewConfiguration.getLongPressTimeout()
    private lateinit var view: View
    private var touchSlop: Int = 0
    private lateinit var longTouchHandler: LongTouchHandler
    private var triggeredCallback = false
    private var triggerCount = 0

    @Before
    fun setup() {
        triggeredCallback = false
        val activityController = Robolectric.buildActivity(Activity::class.java)
        val activity = activityController.get()
        view = View(activity)
        longTouchHandler = LongTouchHandler(activity, { triggeredCallback = true; triggerCount += 1 })
        touchSlop = ShadowViewConfiguration.get(activity).scaledTouchSlop
    }

    @Test
    fun timeout_passed_triggersCallback() {
        val currentTime = ShadowLooper.getShadowMainLooper().scheduler.currentTime
        val touchEvent = MotionEvent.obtain(currentTime, 0, MotionEvent.ACTION_DOWN,
                1.0f, 2.0f, 0)
        longTouchHandler.onTouch(view, touchEvent)

        ShadowLooper.idleMainLooper(longPressTimeout.toLong(), TimeUnit.MILLISECONDS)

        Assert.assertTrue(triggeredCallback)
    }

    @Test
    fun timeout_not_passed_doesntTriggerCallback() {
        val currentTime = ShadowLooper.getShadowMainLooper().scheduler.currentTime
        val touchEvent = MotionEvent.obtain(currentTime, 0, MotionEvent.ACTION_DOWN,
                1.0f, 2.0f, 0)
        longTouchHandler.onTouch(view, touchEvent)

        ShadowLooper.idleMainLooper(longPressTimeout.toLong()-1, TimeUnit.MILLISECONDS)

        Assert.assertFalse(triggeredCallback)
    }

    @Test
    fun slop_plus_one_movement_doesntTriggerCallback() {
        val currentTime = ShadowLooper.getShadowMainLooper().scheduler.currentTime
        val touchEvent = MotionEvent.obtain(currentTime, 0, MotionEvent.ACTION_DOWN,
                1.0f, 2.0f, 0)
        longTouchHandler.onTouch(view, touchEvent)
        val touchEvent2 = MotionEvent.obtain(currentTime, 0, MotionEvent.ACTION_MOVE,
                1.0f + touchSlop + 1, 2.0f, 0)
        longTouchHandler.onTouch(view, touchEvent2)

        ShadowLooper.idleMainLooper(longPressTimeout.toLong(), TimeUnit.MILLISECONDS)

        Assert.assertFalse(triggeredCallback)
    }

    @Test
    fun slop_movement_triggerCallback() {
        val currentTime = ShadowLooper.getShadowMainLooper().scheduler.currentTime
        val touchEvent = MotionEvent.obtain(currentTime, 0, MotionEvent.ACTION_DOWN,
                1.0f, 2.0f, 0)
        longTouchHandler.onTouch(view, touchEvent)
        val touchEvent2 = MotionEvent.obtain(currentTime, 0, MotionEvent.ACTION_MOVE,
                1.0f + touchSlop, 2.0f, 0)
        longTouchHandler.onTouch(view, touchEvent2)

        ShadowLooper.idleMainLooper(longPressTimeout.toLong(), TimeUnit.MILLISECONDS)

        Assert.assertTrue(triggeredCallback)
    }

    @Test
    fun slop_both_axis_movement_doesntTriggerCallback() {
        val currentTime = ShadowLooper.getShadowMainLooper().scheduler.currentTime
        val touchEvent = MotionEvent.obtain(currentTime, 0, MotionEvent.ACTION_DOWN,
                1.0f, 2.0f, 0);
        longTouchHandler.onTouch(view, touchEvent)
        val touchEvent2 = MotionEvent.obtain(currentTime, 0, MotionEvent.ACTION_MOVE,
                1.0f + touchSlop, 2.0f + touchSlop, 0)
        longTouchHandler.onTouch(view, touchEvent2)

        ShadowLooper.idleMainLooper(longPressTimeout.toLong(), TimeUnit.MILLISECONDS)

        Assert.assertFalse(triggeredCallback)
    }

    @Test
    fun touch_up_cancels_handler_doesntTriggerCallback() {
        val currentTime = ShadowLooper.getShadowMainLooper().scheduler.currentTime
        val touchEvent = MotionEvent.obtain(currentTime, 0, MotionEvent.ACTION_DOWN,
                1.0f, 2.0f, 0)
        longTouchHandler.onTouch(view, touchEvent)
        val touchEvent2 = MotionEvent.obtain(currentTime, 0, MotionEvent.ACTION_UP,
                1.0f, 2.0f, 0)
        longTouchHandler.onTouch(view, touchEvent2)

        ShadowLooper.idleMainLooper(longPressTimeout.toLong(), TimeUnit.MILLISECONDS)

        Assert.assertFalse(triggeredCallback)
    }

    @Test
    fun touch_cancel_cancels_handler_doesntTriggerCallback() {
        val currentTime = ShadowLooper.getShadowMainLooper().scheduler.currentTime
        val touchEvent = MotionEvent.obtain(currentTime, 0, MotionEvent.ACTION_DOWN,
                1.0f, 2.0f, 0)
        longTouchHandler.onTouch(view, touchEvent)
        val touchEvent2 = MotionEvent.obtain(currentTime, 0, MotionEvent.ACTION_CANCEL,
                1.0f, 2.0f, 0)
        longTouchHandler.onTouch(view, touchEvent2)

        ShadowLooper.idleMainLooper(longPressTimeout.toLong(), TimeUnit.MILLISECONDS)

        Assert.assertFalse(triggeredCallback)
    }

}