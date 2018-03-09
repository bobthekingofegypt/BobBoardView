package org.bobstuff.bobboardview.app.trello

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.ClipData
import android.content.Context
import android.graphics.Rect
import android.os.Parcelable
import android.renderscript.Sampler
import android.support.v7.widget.CardView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.OrientationHelper
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.DRAG_FLAG_OPAQUE
import android.view.ViewGroup
import android.widget.TextView
import org.bobstuff.bobboardview.*

import org.bobstuff.bobboardview.BobBoardListAdapter.DefaultCardEventCallbacks
import org.bobstuff.bobboardview.app.R
import org.bobstuff.bobboardview.app.scrum.ScrumBoardAdapter
import org.bobstuff.bobboardview.app.trello.model.Board
import org.bobstuff.bobboardview.app.trello.model.BoardList
import org.bobstuff.bobboardview.app.trello.model.Card
import org.bobstuff.bobboardview.app.util.SimpleShadowBuilder

import java.util.Collections
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Created by bob
 */

class TrelloBoardAdapter(private val context: Context, private val width: Int) :
        BobBoardAdapter<TrelloBoardAdapter.TrelloListViewHolderBase>() {
    private val lists: MutableList<BoardList> = mutableListOf()
    private val animatingViewHolders: MutableSet<TrelloListViewHolderBase> = CopyOnWriteArraySet()
    private var listDraggingActivated: Boolean = false
    private var currentlyScaled: Boolean = false
    private var currentlyAnimating: Boolean = false
    private val scaledWidth: Int = (width * SCALE_FACTOR).toInt()
    private val columnScrollPositions: MutableMap<BoardList, Parcelable> = mutableMapOf()
    var isListAddedDuringDrag: Boolean = false
    var itemAddedDuringDrag: Long = -1
    var animator: ValueAnimator? = null
    var orientationHelper: OrientationHelper? = null
    val viewPool = RecyclerView.RecycledViewPool()

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        orientationHelper = OrientationHelper.createHorizontalHelper(recyclerView.layoutManager)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        orientationHelper = null
    }

    override fun getItemId(position: Int): Long {
        if (position <= lists.size - 1) {
            return lists[position].hashCode().toLong()
        }
        return 666
    }

    fun setItems(items: List<BoardList>) {
        this.lists.clear()
        this.lists.addAll(items)
        this.notifyDataSetChanged()
    }

    fun removeItem(index: Int): BoardList {
        val viewHolder = boardView!!.listRecyclerView.findViewHolderForAdapterPosition(index)
        val holder = viewHolder as TrelloListViewHolder
        columnScrollPositions[lists[index]] = holder.recyclerView.layoutManager.onSaveInstanceState()
        val boardList = lists.removeAt(index)
        Log.d("TEST", "remove item b: ${boardList.title}")
        notifyItemRemoved(index)
        return boardList
    }

    fun insertItem(index: Int, boardList: BoardList) {
        insertItem(index, boardList, true)
    }

    fun insertItem(index: Int, boardList: BoardList, isListAddedDuringDrag: Boolean) {
        Log.d("TEST", "Inserting item b: ${boardList.title}")
        lists.add(index, boardList)
        this.isListAddedDuringDrag = isListAddedDuringDrag
        itemAddedDuringDrag = boardList.hashCode().toLong()
        notifyItemInserted(index)
    }

    fun swapItem(fromPosition: Int, toPosition: Int): Boolean {
        Collections.swap(lists, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
        return true
    }

    override fun getItemCount(): Int {
        return lists.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        if (position <= lists.size - 1) {
            return 0
        }
        return 1
    }

    override fun onViewRecycled(holder: TrelloListViewHolderBase) {
        val index = holder.adapterPosition
        if (index != -1) {
            holder.recyclerView?.let {
                columnScrollPositions[lists[index]] = it.layoutManager.onSaveInstanceState()
            }
        }

        super.onViewRecycled(holder)
    }

    override fun onBindViewHolder(holder: TrelloListViewHolderBase, position: Int) {
        when(holder.itemViewType) {
            0 -> {
                val holder = holder as TrelloListViewHolder
                val boardList = lists[position]
                val title = boardList.title
                holder.title.text = title
                holder.recyclerView.contentDescription = "$title cards"
                holder.id = boardList.title

                holder.itemView.findViewById<View>(R.id.simple_text2).setOnClickListener {
                    val a = holder.listAdapter as TrelloListAdapter
                    (holder.listAdapter as TrelloListAdapter).onItemMove(a.itemCount - 2, a.itemCount -1)
                }

                holder.title.setOnTouchListener(object : View.OnTouchListener {
                    override fun onTouch(v: View, event: MotionEvent): Boolean {
                        val action = event.action
                        when (action and MotionEvent.ACTION_MASK) {
                            MotionEvent.ACTION_DOWN -> {

                                val data = ClipData.newPlainText("", "")
                                val shadowBuilder = SimpleShadowBuilder(holder.itemView, 3.0, 0.7f, event.x, event.y)
                                val r = holder.itemView.startDrag(data, shadowBuilder, null, DRAG_FLAG_OPAQUE)
                                //Log.d("TEST", "drag return value : " + r)

                                //if (r) {
                                boardView?.startListDrag(holder as ListViewHolder<*>, event.x * SCALE_FACTOR, event.y)
                                holder.itemView.visibility = View.INVISIBLE
                                //}
                            }
                            MotionEvent.ACTION_CANCEL -> {
                                //boardView?.stopDragEventIfStillRunning()
                                //Log.d("TEST", "CANCEL")
                            }

                            MotionEvent.ACTION_UP -> {
                                // boardView?.stopDragEventIfStillRunning()
                                Log.d("TEST", "UP")
                            }
                        }
                        return true
                    }
                })

                holder.listAdapter.setItems(boardList.cards)

                if (columnScrollPositions.containsKey(boardList)) {
                    holder.recyclerView.layoutManager.onRestoreInstanceState(columnScrollPositions[boardList])
                }
            }
            1 -> {
                val holder = holder as TrelloNewListViewHolder
                //TODO add onclick listener
            }
        }

        val lp = holder.itemView.layoutParams
        val child = holder.cardView
        var ratio = 1.0f
        var currentWidth = width

        if (listDraggingActivated) {
            currentWidth = scaledWidth
            ratio = scaledWidth / width.toFloat()
        }

        lp.width = currentWidth
        holder.itemView.layoutParams = lp
        child.scaleY = ratio
        child.scaleX = ratio

        /*
        holder.itemView.setOnTouchListener(LongTouchHandler(context, object : OnLongTouchHandlerCallback {
            override fun onLongPress(event: MotionEvent) {
                boardView?.startListDrag(holder as ListViewHolder<*>, event.x * SCALE_FACTOR, event.y)

                val data = ClipData.newPlainText("", "")
                val shadowBuilder = SimpleShadowBuilder(holder.itemView, 3.0, 0.7f, event.x, event.y)
                holder.itemView.startDrag(data, shadowBuilder, null, 0)
                holder.itemView.visibility = View.INVISIBLE
            }

            override fun onClick(e: MotionEvent) {

            }
        }))
        */

    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): TrelloListViewHolderBase {
        return when (viewType) {
            0 -> {
                val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.item_list, viewGroup, false)
                val params = view.layoutParams
                params.height = boardView!!.listRecyclerView.layoutParams.height
                view.layoutParams = params
                TrelloListViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.item_trello_add_list, viewGroup, false)
                TrelloNewListViewHolder(view)
            }
        }
    }

    fun triggerScaleDownAnimationForListDrag(viewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>) {
        if (listDraggingActivated || boardView == null) {
            return  //this shouldn't happen but hey ho it might
        }

        val boardView = boardView!!
        val recyclerView = boardView.listRecyclerView

        listDraggingActivated = true
        currentlyAnimating = true
        val childCount = recyclerView.childCount
        var i = 0
        while (i < childCount) {
            val viewHolder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i)) as TrelloListViewHolderBase
            animatingViewHolders.add(viewHolder)
            ++i
        }

        val lm = recyclerView.layoutManager
        val rect = Rect()
        lm.calculateItemDecorationsForChild(viewHolder.itemView, rect)
        val fullWidth = width + rect.left + rect.right
        val offset = (width * 1.5) - (width * 1.5 * 0.7)

        animator = ValueAnimator.ofInt(width, scaledWidth)
        animator?.addUpdateListener(LayoutWidthUpdateListener())
        animator?.addUpdateListener(ScrollInTimeUpdateListener(recyclerView.scrollX, offset.toInt(), false))
        animator?.addListener(ViewHolderAnimatorListener())
        animator?.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                currentlyScaled = true
                currentlyAnimating = false
                animatingViewHolders.clear()
            }
        })
        animator?.start()
    }

    fun triggerScaleUpAnimationForListDrag(viewHolder: BobBoardAdapter.ListViewHolder<BobBoardListAdapter<*>>) {
        val boardView = boardView!!
        val recyclerView = boardView.listRecyclerView

        currentlyAnimating = true
        val childCount = recyclerView.childCount
        var i = 0
        while (i < childCount) {
            val child = recyclerView.getChildAt(i)
            val lp = child.layoutParams as RecyclerView.LayoutParams
            val removed = lp.isItemRemoved
            if (!removed) {
                val viewHolder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i)) as TrelloListViewHolderBase
                //Log.d("TEST", "triggerScaleUpAnimation viewholder ${viewHolder.title.text}")
               // Log.d("TEST", "triggerScaleUpAnimation poistion ${viewHolder.adapterPosition}")
               // Log.d("TEST", "triggerScaleUpAnimation removed ${removed}")
                animatingViewHolders.add(viewHolder)
            }
            ++i
        }

        val playTime = animator?.currentPlayTime
        var startValue = scaledWidth
        if (animator?.isRunning!!) {
            startValue = animator?.animatedValue as Int
            animator?.cancel()
        }
        val lm = recyclerView.layoutManager

        val orientationHelper = orientationHelper!!

        val childCenter = orientationHelper.getDecoratedStart(viewHolder.itemView) +
                orientationHelper.getDecoratedMeasurement(viewHolder.itemView) / 2
        val containerCenter: Int
        if (lm.clipToPadding) {
            containerCenter = orientationHelper.getStartAfterPadding() + orientationHelper.getTotalSpace() / 2
        } else {
            containerCenter = orientationHelper.getEnd() / 2
        }
        val distanceToCenter = childCenter - containerCenter
        Log.d("TEST", "distance to center: $distanceToCenter")
        Log.d("TEST", "left edge: ${viewHolder.itemView.left}")
        val rect = Rect()
        lm.calculateItemDecorationsForChild(viewHolder.itemView, rect)
        val fullWidth = width + rect.left + rect.right
        val offset = (((width * 1) - (startValue * 1))) / 2

        val animator = ValueAnimator.ofInt(startValue, width)
        animator.addUpdateListener(LayoutWidthUpdateListener())
        //animator.addUpdateListener(ScrollInTimeUpdateListener(recyclerView.scrollX, offset.toInt(), true))
        animator.addUpdateListener(ScrollInTimeUpdateListener2(viewHolder.itemView))
        animator.addListener(ViewHolderAnimatorListener())
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                currentlyAnimating = false
                listDraggingActivated = false
                currentlyScaled = false
                animatingViewHolders.clear()
            }
        })
        animator.start()
    }

    override fun onViewDetachedFromWindow(holder: TrelloListViewHolderBase) {
        Log.d("TEST", "View detatched from window")
        super.onViewDetachedFromWindow(holder)
    }

    override fun onViewAttachedToWindow(viewHolder: TrelloListViewHolderBase) {
        Log.d("TEST", "onViewAttachedToWindow hash:${viewHolder.itemId}; added:${itemAddedDuringDrag} isListAddedDuringDrag:$isListAddedDuringDrag")
        if (isListAddedDuringDrag && viewHolder.itemId == itemAddedDuringDrag) {
            isListAddedDuringDrag = false
            itemAddedDuringDrag = -1
            boardView?.switchListDrag(viewHolder!!)
            boardView?.disableDragEvents = false
            viewHolder?.itemView?.visibility = View.INVISIBLE
        }
        if (currentlyAnimating) {
            viewHolder!!.setIsRecyclable(false)
            animatingViewHolders.add(viewHolder)
        } else if (currentlyScaled) {
            val child = viewHolder.cardView
            val currentWidth = scaledWidth
            val ratio = scaledWidth / width.toFloat()

            val lp = viewHolder.itemView.layoutParams
            lp.width = currentWidth
            viewHolder.itemView.layoutParams = lp
            child.scaleY = ratio
            child.scaleX = ratio
        } else {
            val child = viewHolder.cardView
            val ratio = 1.0f
            val currentWidth = width

            val lp = viewHolder.itemView.layoutParams
            lp.width = currentWidth
            viewHolder.itemView.layoutParams = lp
            child.scaleY = ratio
            child.scaleX = ratio
        }
    }


    private inner class LayoutWidthUpdateListener : ValueAnimator.AnimatorUpdateListener {
        override fun onAnimationUpdate(animation: ValueAnimator) {
            val animationValue = animation.animatedValue as Int
            val scale = animationValue / width.toFloat()
            for (vh in animatingViewHolders) {
                val lp = vh.itemView.layoutParams
                lp.width = animationValue
                vh.itemView.layoutParams = lp

                val child = vh.cardView
                child.scaleY = scale
                child.scaleX = scale
            }
        }
    }

    private inner class ScrollInTimeUpdateListener(val startingX: Int, val offset: Int, val reverse: Boolean) : ValueAnimator.AnimatorUpdateListener {
        var total = 0
        override fun onAnimationUpdate(animation: ValueAnimator) {
            val progress = animation.animatedFraction
            val step = (progress * offset).toInt()
            var scrollBy = step - total
            total = total + scrollBy

            scrollBy = scrollBy * (if (reverse) { 1 } else -1)

            Log.d("TEST", "Step: $step; progress: $progress; scrollBy: $scrollBy; total: $total; offset: $offset")

            boardView?.listRecyclerView?.scrollBy(scrollBy, 0)
        }
    }

    private inner class ScrollInTimeUpdateListener2(val view: View) : ValueAnimator.AnimatorUpdateListener {
        var total = 0
        override fun onAnimationUpdate(animation: ValueAnimator) {
            val lm = boardView!!.listRecyclerView.layoutManager
            val orientationHelper = orientationHelper!!
            val childCenter = orientationHelper.getDecoratedStart(view) +
                    orientationHelper.getDecoratedMeasurement(view) / 2
            val containerCenter: Int
            if (lm.clipToPadding) {
                containerCenter = orientationHelper.getStartAfterPadding() + orientationHelper.getTotalSpace() / 2
            } else {
                containerCenter = orientationHelper.getEnd() / 2
            }
            val distanceToCenter = childCenter - containerCenter
            Log.d("TEST", "distancetocenter: $distanceToCenter")

            boardView?.listRecyclerView?.scrollBy(distanceToCenter, 0)
        }
    }

    internal inner class ViewHolderAnimatorListener : AnimatorListenerAdapter() {
        override fun onAnimationStart(animation: Animator) {
            for (vh in animatingViewHolders) {
                vh.setIsRecyclable(false)
            }
        }

        override fun onAnimationEnd(animation: Animator) {
            reset()
        }

        override fun onAnimationCancel(animation: Animator) {
            reset()
        }

        private fun reset() {
            for (vh in animatingViewHolders) {
                vh.setIsRecyclable(true)
            }
        }
    }

    companion object {
        private const val SCALE_FACTOR = 0.7f
    }

    abstract inner class TrelloListViewHolderBase(view: View): BobBoardAdapter.ListViewHolder<TrelloListAdapter>(view) {
        val cardView: View = view.findViewById(R.id.cardview)

        init {
            cardView.pivotX = 0f
            cardView.pivotY = 0f
        }
    }

    inner class TrelloListViewHolder(view: View) : TrelloListViewHolderBase(view) {
        var id: String? = null
        val title: TextView = view.findViewById(R.id.title)
        override val recyclerView: RecyclerView = view.findViewById(R.id.card_recycler)
        override var listAdapter: TrelloListAdapter =
                TrelloListAdapter(context, DefaultCardEventCallbacks(
                        this@TrelloBoardAdapter, this))

        init {
            recyclerView.layoutManager = LinearLayoutManager(recyclerView.context, LinearLayoutManager.VERTICAL, false)
            recyclerView.adapter = listAdapter
            recyclerView.recycledViewPool = viewPool

            //val dividerSizeInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, context.resources.displayMetrics)
            //recyclerView.addItemDecoration(BobBoardSimpleDividers(dividerSizeInPixels.toInt(), BobBoardSimpleDividersOrientation.VERTICAL))
        }
    }

    inner class TrelloNewListViewHolder(view: View) : TrelloListViewHolderBase(view) {
        override val recyclerView: RecyclerView? = null
        override var listAdapter: TrelloListAdapter? = null
    }
}
