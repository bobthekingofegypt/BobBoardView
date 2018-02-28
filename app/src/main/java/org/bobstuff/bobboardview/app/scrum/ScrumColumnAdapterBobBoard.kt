package org.bobstuff.bobboardview.app.scrum

import android.content.ClipData
import android.content.Context
import android.graphics.Color
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.bobstuff.bobboardview.BobBoardListAdapter
import org.bobstuff.bobboardview.R
import android.util.TypedValue
import android.view.MotionEvent
import org.bobstuff.bobboardview.LongTouchHandler
import java.util.*
import android.widget.Toast
import org.bobstuff.bobboardview.OnLongTouchHandlerCallback
import org.bobstuff.bobboardview.app.util.SimpleShadowBuilder


/**
 * Created by bob on 21/02/18.
 */

class ScrumColumnAdapterBobBoard(val context: Context, cardEventCallbacks: CardEventCallbacks):
        BobBoardListAdapter<ScrumColumnAdapterBobBoard.ScrumUserStoryViewHolder>(cardEventCallbacks, true) {

    val userStories: MutableList<UserStory> = mutableListOf()

    fun setItems(items: List<UserStory>) {
        userStories.clear()
        userStories.addAll(items)
        notifyDataSetChanged()
    }

    fun swapItems(fromPosition: Int, toPosition: Int) {
        Collections.swap(userStories, toPosition, fromPosition)
        notifyItemMoved(toPosition, fromPosition)
    }

    fun removeItem(index: Int): UserStory {
        val userStory = userStories.removeAt(index)
        notifyItemRemoved(index)
        return userStory
    }

    fun insertItem(index: Int, userStory: UserStory) {
        this.insertItem(index, userStory, true)
    }

    fun insertItem(index: Int, userStory: UserStory, isCardAddedDuringDrag: Boolean) {
        userStories.add(index, userStory)
        this.isCardAddedDuringDrag = isCardAddedDuringDrag
        notifyItemInserted(index)
    }

    override fun getItemCount(): Int {
        return userStories.size
    }

    override fun onBindViewHolder(holder: ScrumUserStoryViewHolder, position: Int) {
        val userStory = userStories[position]
        holder.overlay.visibility = View.GONE
        holder.description.text = userStory.description
        when(userStory.priority) {
            Priority.HIGH -> holder.priorityBar.setBackgroundColor(Color.parseColor("#8d5a2f"))
            Priority.MEDIUM -> holder.priorityBar.setBackgroundColor(Color.parseColor("#2f4f8d"))
            Priority.LOW -> holder.priorityBar.setBackgroundColor(Color.parseColor("#2f4f1b"))
        }
        holder.storyId.text = userStory.id
        if (userStory.image != null) {
            holder.userStoryImage.setImageDrawable(ContextCompat.getDrawable(context, userStory.image))
            val lp = holder.userStoryImage.layoutParams
            val r = context.resources
            val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150f, r.getDisplayMetrics())
            lp.height = px.toInt()
            holder.userStoryImage.layoutParams = lp
        } else {
            holder.userStoryImage.setImageDrawable(null)
            val lp = holder.userStoryImage.layoutParams
            lp.height = 0
            holder.userStoryImage.layoutParams = lp
        }
        holder.itemView.setOnTouchListener(LongTouchHandler(context, object : OnLongTouchHandlerCallback {
            override fun onClick(e: MotionEvent) {
                Toast.makeText(context, "Clicked on story ${userStory.id}",
                        Toast.LENGTH_LONG).show()
            }

            override fun onLongPress(event: MotionEvent) {
                cardEventCallbacks.cardSelectedForDrag(holder, event.x, event.y)

                val data = ClipData.newPlainText("", "")
                val shadowBuilder = SimpleShadowBuilder(holder.itemView, 1.0, 1.0f, event.x, event.y)
                holder.itemView.startDrag(data, shadowBuilder, null, 0)
                holder.overlay.visibility = View.VISIBLE
            }
        }))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScrumUserStoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_scrum_user_story, parent, false)
        return ScrumUserStoryViewHolder(view)
    }

    override fun onViewAttachedToWindow(viewHolder: ScrumUserStoryViewHolder) {
        if (isCardAddedDuringDrag) {
            viewHolder.overlay.visibility = View.VISIBLE
        }
        super.onViewAttachedToWindow(viewHolder)
    }

    inner class ScrumUserStoryViewHolder(val view: View): BobBoardListAdapter.CardViewHolder(view) {
        var overlay: View = view.findViewById(R.id.overlay)
        val description: TextView = view.findViewById(R.id.description)
        val priorityBar: View = view.findViewById(R.id.priority_bar)
        val storyId: TextView = view.findViewById(R.id.story_id)
        val userStoryImage: ImageView = view.findViewById(R.id.user_story_image)
    }
}