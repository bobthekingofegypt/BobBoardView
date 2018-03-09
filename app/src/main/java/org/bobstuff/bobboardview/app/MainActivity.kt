package org.bobstuff.bobboardview.app

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import org.bobstuff.bobboardview.app.R
import android.widget.TextView
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import org.bobstuff.bobboardview.app.reuse.ReuseSampleActivity
import org.bobstuff.bobboardview.app.scrum.ScrumActivity
import org.bobstuff.bobboardview.app.trello.BoardActivity


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.my_toolbar)
        toolbar.setTitleTextColor(Color.parseColor("#ffffff"))
        toolbar.title = "BobBoardView Samples"

        val recyclerView = findViewById<RecyclerView>(R.id.options_recycler)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView.addItemDecoration(DividerItemDecoration(recyclerView.context, LinearLayoutManager.VERTICAL))

        val data = listOf("Trello style", "Scrum board view", "Large dataset, reuse")
        val adapter = MyRecyclerViewAdapter(this, data)
        adapter.setClickListener(object: ItemClickListener {
            override fun onItemClick(view: View, position: Int) {
                when (position) {
                    0 -> startActivity(Intent(this@MainActivity, BoardActivity::class.java))
                    1 -> startActivity(Intent(this@MainActivity, ScrumActivity::class.java))
                    2 -> startActivity(Intent(this@MainActivity, ReuseSampleActivity::class.java))
                }
            }
        })

        recyclerView.adapter = adapter
    }

    inner class MyRecyclerViewAdapter(private val context: Context, private val data: List<String>) :
            RecyclerView.Adapter<MyRecyclerViewAdapter.ViewHolder>() {
        private val layoutInflater: LayoutInflater = LayoutInflater.from(context)
        private var mClickListener: ItemClickListener? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.item_option, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val text = data.get(position)
            holder.textView.setText(text)
        }

        override fun getItemCount(): Int {
            return data.size
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
            var textView: TextView = itemView.findViewById(R.id.textview)

            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(view: View) {
                if (mClickListener != null) mClickListener!!.onItemClick(view, adapterPosition)
            }
        }

        fun setClickListener(itemClickListener: ItemClickListener) {
            this.mClickListener = itemClickListener
        }


    }
}

interface ItemClickListener {
    fun onItemClick(view: View, position: Int)
}
