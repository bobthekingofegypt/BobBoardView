package org.bobstuff.bobboardview.app.util

import android.graphics.Point
import android.graphics.PorterDuff
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import org.bobstuff.bobboardview.app.R

/**
 * Created by bob
 */

fun AppCompatActivity.setStatusBarColor(color: Int, light: Boolean) {
    val window = this.window
    val decorView = window.decorView
    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        window.statusBarColor = color
    }

    if (light) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            decorView.systemUiVisibility = decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }
}

fun AppCompatActivity.getWindowSize(): Point {
    val display = windowManager.defaultDisplay
    val size = Point()
    display.getSize(size)
    return size
}

fun AppCompatActivity.styleToolbarContent(toolbar: Toolbar, color: Int) {
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    supportActionBar?.setDisplayShowHomeEnabled(true)
    val upArrow = ContextCompat.getDrawable(this, R.drawable.abc_ic_ab_back_material)
    upArrow?.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
    supportActionBar?.setHomeAsUpIndicator(upArrow)
    toolbar.setTitleTextColor(color)
}
