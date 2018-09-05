package org.bobstuff.bobboardview.app.trello

import com.facebook.testing.screenshot.ScreenshotRunner
import android.os.Bundle
import com.facebook.testing.screenshot.ScreenshotRunner.onCreate
import androidx.test.runner.AndroidJUnitRunner


/**
 * Created by bob
 */
class CustomTestRunner : AndroidJUnitRunner() {
    override fun onCreate(args: Bundle) {
        ScreenshotRunner.onCreate(this, args)
        super.onCreate(args)
    }

    override fun finish(resultCode: Int, results: Bundle) {
        ScreenshotRunner.onDestroy()
        super.finish(resultCode, results)
    }
}