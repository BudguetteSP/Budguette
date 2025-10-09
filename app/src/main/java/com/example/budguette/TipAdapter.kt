package com.example.budguette

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TipAdapter : RecyclerView.Adapter<TipAdapter.TipViewHolder>() {

    private val tips = mutableListOf<Tip>()

    fun submitList(newTips: List<Tip>) {
        tips.clear()
        tips.addAll(newTips)
        notifyDataSetChanged()
    }

    class TipViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.tip_title)
        val contentText: TextView = itemView.findViewById(R.id.tip_content)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TipViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tip, parent, false)
        return TipViewHolder(view)
    }

    override fun onBindViewHolder(holder: TipViewHolder, position: Int) {
        val tip = tips[position]
        holder.titleText.text = tip.title
        holder.contentText.text = tip.content
    }

    override fun getItemCount(): Int = tips.size
}
