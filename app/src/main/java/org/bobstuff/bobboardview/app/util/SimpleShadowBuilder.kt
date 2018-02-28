package org.bobstuff.bobboardview.app.util

import android.graphics.Canvas
import android.graphics.Point
import android.view.View
import kotlin.math.PI

/**
 * Created by bob
 */
class SimpleShadowBuilder(
        view: View,
        private val degrees: Double,
        private val scale: Float,
        private val x: Float,
        private val y: Float
) : View.DragShadowBuilder(view) {
    private val radians = degrees * (PI /180)
    private val widthScaled: Int = (view.width * scale).toInt()
    private val heightScaled: Int = (view.height * scale).toInt()
    private val widthAdjusted: Int = ((widthScaled*Math.cos(radians)) - (heightScaled*Math.sin(radians))).toInt()
    private val heightAdjusted: Int = (Math.abs(widthScaled*Math.sin(radians)) + (heightScaled*Math.cos(radians))).toInt()
    private val offsetX: Float = Math.abs(widthAdjusted - widthScaled).toFloat()
    private val offsetY: Float = Math.abs(heightAdjusted - heightScaled).toFloat()

    override fun onProvideShadowMetrics(size: Point, touch: Point) {
        var xScaled = x * scale
        var yScaled = y * scale
        var tX = (xScaled*Math.cos(radians)) - (yScaled*Math.sin(radians))
        var tY = Math.abs(xScaled*Math.sin(radians)) + (yScaled*Math.cos(radians))

        size.set(widthScaled + offsetX.toInt(), heightScaled + offsetY.toInt())
        touch.set((tX + offsetX).toInt(), (tY).toInt())
    }

    override fun onDrawShadow(canvas: Canvas) {
        if (view != null) {
            canvas.save()
            canvas.translate(offsetX, 0f)
            canvas.scale(scale, scale)
            canvas.rotate(degrees.toFloat(), 0f, 0f)
            super.onDrawShadow(canvas)
            canvas.restore()
        }
    }
}