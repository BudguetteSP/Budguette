package com.example.budguette

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.ImageView


class MoreMenuAdapter(
    private val context: Context,
    private val items: List<Pair<String, Int>>
) : ArrayAdapter<Pair<String, Int>>(context, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_more_menu, parent, false)

        val item = items[position]
        val icon = view.findViewById<ImageView>(R.id.itemIcon)
        val text = view.findViewById<TextView>(R.id.itemText)

        icon.setImageResource(item.second)
        text.text = item.first
        text.setTextColor(context.resources.getColor(android.R.color.white, null))

        return view
    }
}
