package org.bobstuff.bobboardview

import android.app.Activity
import android.graphics.Rect
import android.support.v7.widget.RecyclerView
import android.view.View
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * Created by bob
 */
@RunWith(RobolectricTestRunner::class)
class BobBoardSimpleDividersTest {
    private lateinit var view: View

    @Before
    fun setup() {
        val activityController = Robolectric.buildActivity(Activity::class.java)
        val activity = activityController.get()
        view = View(activity)
    }

    @Test
    fun horizontal_spacing_is_correct_for_first_item() {
        val recyclerView = Mockito.mock(RecyclerView::class.java)
        val adapter = Mockito.mock(RecyclerView.Adapter::class.java)
        Mockito.`when`(recyclerView.adapter).thenReturn(adapter)
        Mockito.`when`(recyclerView.getChildAdapterPosition(view)).thenReturn(0)
        Mockito.`when`(adapter.itemCount).thenReturn(5)

        val sut = BobBoardSimpleDividers(10, BobBoardSimpleDividersOrientation.HORIZONTAL)
        val rect = Rect()
        sut.getItemOffsets(rect, view, recyclerView, null)

        Assert.assertEquals(10, rect.left)
        Assert.assertEquals(5, rect.right)
        Assert.assertEquals(0, rect.top)
        Assert.assertEquals(0, rect.bottom)
    }

    @Test
    fun horizontal_spacing_is_correct_for_middle_item() {
        val recyclerView = Mockito.mock(RecyclerView::class.java)
        val adapter = Mockito.mock(RecyclerView.Adapter::class.java)
        Mockito.`when`(recyclerView.adapter).thenReturn(adapter)
        Mockito.`when`(recyclerView.getChildAdapterPosition(view)).thenReturn(1)
        Mockito.`when`(adapter.itemCount).thenReturn(5)

        val sut = BobBoardSimpleDividers(10, BobBoardSimpleDividersOrientation.HORIZONTAL)
        val rect = Rect()
        sut.getItemOffsets(rect, view, recyclerView, null)

        Assert.assertEquals(5, rect.left)
        Assert.assertEquals(5, rect.right)
        Assert.assertEquals(0, rect.top)
        Assert.assertEquals(0, rect.bottom)
    }

    @Test
    fun horizontal_spacing_is_correct_for_last_item() {
        val recyclerView = Mockito.mock(RecyclerView::class.java)
        val adapter = Mockito.mock(RecyclerView.Adapter::class.java)
        Mockito.`when`(recyclerView.adapter).thenReturn(adapter)
        Mockito.`when`(recyclerView.getChildAdapterPosition(view)).thenReturn(4)
        Mockito.`when`(adapter.itemCount).thenReturn(5)

        val sut = BobBoardSimpleDividers(10, BobBoardSimpleDividersOrientation.HORIZONTAL)
        val rect = Rect()
        sut.getItemOffsets(rect, view, recyclerView, null)

        Assert.assertEquals(5, rect.left)
        Assert.assertEquals(10, rect.right)
        Assert.assertEquals(0, rect.top)
        Assert.assertEquals(0, rect.bottom)
    }

    @Test
    fun vertical_spacing_is_correct_for_first_item() {
        val recyclerView = Mockito.mock(RecyclerView::class.java)
        val adapter = Mockito.mock(RecyclerView.Adapter::class.java)
        Mockito.`when`(recyclerView.adapter).thenReturn(adapter)
        Mockito.`when`(recyclerView.getChildAdapterPosition(view)).thenReturn(0)
        Mockito.`when`(adapter.itemCount).thenReturn(5)

        val sut = BobBoardSimpleDividers(10, BobBoardSimpleDividersOrientation.VERTICAL)
        val rect = Rect()
        sut.getItemOffsets(rect, view, recyclerView, null)

        Assert.assertEquals(0, rect.left)
        Assert.assertEquals(0, rect.right)
        Assert.assertEquals(10, rect.top)
        Assert.assertEquals(5, rect.bottom)
    }

    @Test
    fun vertical_spacing_is_correct_for_middle_item() {
        val recyclerView = Mockito.mock(RecyclerView::class.java)
        val adapter = Mockito.mock(RecyclerView.Adapter::class.java)
        Mockito.`when`(recyclerView.adapter).thenReturn(adapter)
        Mockito.`when`(recyclerView.getChildAdapterPosition(view)).thenReturn(1)
        Mockito.`when`(adapter.itemCount).thenReturn(5)

        val sut = BobBoardSimpleDividers(10, BobBoardSimpleDividersOrientation.VERTICAL)
        val rect = Rect()
        sut.getItemOffsets(rect, view, recyclerView, null)

        Assert.assertEquals(0, rect.left)
        Assert.assertEquals(0, rect.right)
        Assert.assertEquals(5, rect.top)
        Assert.assertEquals(5, rect.bottom)
    }

    @Test
    fun vertical_spacing_is_correct_for_last_item() {
        val recyclerView = Mockito.mock(RecyclerView::class.java)
        val adapter = Mockito.mock(RecyclerView.Adapter::class.java)
        Mockito.`when`(recyclerView.adapter).thenReturn(adapter)
        Mockito.`when`(recyclerView.getChildAdapterPosition(view)).thenReturn(4)
        Mockito.`when`(adapter.itemCount).thenReturn(5)

        val sut = BobBoardSimpleDividers(10, BobBoardSimpleDividersOrientation.VERTICAL)
        val rect = Rect()
        sut.getItemOffsets(rect, view, recyclerView, null)

        Assert.assertEquals(0, rect.left)
        Assert.assertEquals(0, rect.right)
        Assert.assertEquals(5, rect.top)
        Assert.assertEquals(10, rect.bottom)
    }
}