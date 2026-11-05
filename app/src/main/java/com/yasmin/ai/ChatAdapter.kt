package com.yasmin.ai

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class Msg(val role: String, val text: String)

class ChatAdapter(private val items: List<Msg>) : RecyclerView.Adapter<ChatAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val text: TextView = v.findViewById(R.id.text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val m = items[position]
        val mine = m.role == "user"
        h.text.text = m.text
        h.text.setTextColor(if (mine) Color.WHITE else Color.parseColor("#1B1B1F"))
        h.text.background = GradientDrawable().apply {
            cornerRadius = 36f
            setColor(Color.parseColor(if (mine) "#6750A4" else "#E8DEF8"))
        }
        (h.text.layoutParams as FrameLayout.LayoutParams).gravity =
            if (mine) Gravity.END else Gravity.START
    }
}
