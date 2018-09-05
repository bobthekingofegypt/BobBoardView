package org.bobstuff.bobboardview.app.scrum

import android.content.ClipData
import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.core.content.ContextCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.bobstuff.bobboardview.app.R
import android.util.TypedValue
import android.view.MotionEvent
import android.widget.Toast
import org.bobstuff.bobboardview.*
import org.bobstuff.bobboardview.app.util.SimpleShadowBuilder


/**
 * Created by bob on 21/02/18.
 */

class ScrumColumnAdapter(val context: Context, dragOperation: BobBoardDragOperation<UserStory, Column>,
                         cardEventCallbacks: CardEventCallbacks):
        BobBoardListArrayAdapter<ScrumColumnAdapter.ScrumUserStoryViewHolder, UserStory, Column>(cardEventCallbacks, dragOperation) {

    var columnContainingDragTargetRedisplaying = false

    override fun getItemId(position: Int): Long {
        return cards[position].id.hashCode().toLong()
    }

    override fun onBindViewHolder(holder: ScrumUserStoryViewHolder, position: Int) {
        val userStory = cards[position]
        holder.overlay.visibility = View.INVISIBLE
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    holder.itemView.startDragAndDrop(data, shadowBuilder, null, 0)
                } else {
                    @Suppress("DEPRECATION")
                    holder.itemView.startDrag(data, shadowBuilder, null, 0)
                }
                holder.overlay.visibility = View.VISIBLE
            }
        }))

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScrumUserStoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_scrum_user_story, parent, false)
        return ScrumUserStoryViewHolder(view)
    }

    override fun onViewAttachedToWindow(viewHolder: ScrumUserStoryViewHolder) {
        //TODO we want to generalise this redisplay logic so that any views that don't remove the
        //card during drag can have it for free
        if (columnContainingDragTargetRedisplaying && viewHolder.adapterPosition == dragOperation.cardIndex) {
            viewHolder.overlay.visibility = View.VISIBLE
            cardEventCallbacks.cardMovedDuringDrag(viewHolder, false)
            columnContainingDragTargetRedisplaying = false
        }
        if (isCardAddedDuringDrag && addedCardId == viewHolder.itemId) {
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