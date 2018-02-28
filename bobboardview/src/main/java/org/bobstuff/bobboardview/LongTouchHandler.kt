package org.bobstuff.bobboardview

import android.content.Context
import android.os.Handler
import android.os.Message
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import java.lang.ref.WeakReference

/**
 * LongTouchHandler, based on the open source code available in the simple gesture detector.
 * This class is required because we need to know the x,y coordinates of the long press and
 * that isn't available using built in long press detector.
 *
 * currently gets confused by multitouch, should probably add that functionality back in
 *
 * Created by bob on 29/01/17.
 */

interface OnLongTouchHandlerCallback {
    fun onLongPress(e: MotionEvent)
    fun onClick(e: MotionEvent)
}

class LongTouchHandler(context: Context, val onLongTouchHandlerCallback: OnLongTouchHandlerCallback) : View.OnTouchListener {
    private val longPressTimeout = ViewConfiguration.getLongPressTimeout()
    private val mHandler: Handler
    private var mDownFocusX: Float = 0.toFloat()
    private var mDownFocusY: Float = 0.toFloat()
    private val mTouchSlopSquare: Int
    private var touchMoved: Boolean = false
    private var inLongPress: Boolean = false
    var mCurrentDownEvent: MotionEvent? = null

    init {
        this.mHandler = GestureHandler(WeakReference(this))

        val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        mTouchSlopSquare = touchSlop * touchSlop
    }

    override fun onTouch(v: View, ev: MotionEvent): Boolean {
        val focusX = ev.getX(0)
        val focusY = ev.getY(0)

        val action = ev.action
        when (action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                mDownFocusX = focusX
                mDownFocusY = focusY

                if (mCurrentDownEvent != null) {
                    mCurrentDownEvent!!.recycle()
                }
                mCurrentDownEvent = MotionEvent.obtain(ev)

                mHandler.removeMessages(LONG_PRESS)
                mHandler.sendEmptyMessageAtTime(LONG_PRESS,
                        mCurrentDownEvent!!.downTime + longPressTimeout)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!touchMoved) {
                    val deltaX = (focusX - mDownFocusX).toInt()
                    val deltaY = (focusY - mDownFocusY).toInt()
                    val distance = deltaX * deltaX + deltaY * deltaY
                    val slopSquare = mTouchSlopSquare
                    if (distance > slopSquare) {
                        touchMoved = true
                        mHandler.removeMessages(LONG_PRESS)
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                if (!touchMoved && !inLongPress) {
                    onLongTouchHandlerCallback.onClick(mCurrentDownEvent!!)
                }
                touchMoved = false
                inLongPress = false
                mHandler.removeMessages(LONG_PRESS)
            }
            MotionEvent.ACTION_CANCEL -> {
                touchMoved = false
                inLongPress = false
                mHandler.removeMessages(LONG_PRESS)
            }
        }

        return true
    }


    companion object {
        private const val LONG_PRESS = 1

        internal class GestureHandler(private val longTouchHandlerRef: WeakReference<LongTouchHandler>) : Handler() {

            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    LONG_PRESS -> {
                        val longTouchHandler = longTouchHandlerRef.get()
                        longTouchHandler?.inLongPress = true
                        longTouchHandler?.mCurrentDownEvent?.let { longTouchHandler.onLongTouchHandlerCallback.onLongPress(it) }
                    }
                    else -> throw RuntimeException("Unknown message " + msg) //never
                }
            }
        }
    }
}
