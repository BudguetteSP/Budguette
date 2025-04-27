// TransactionAdapter.kt
package com.example.budguette

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TransactionAdapter(private val transactionList: List<Transaction>) :
    RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val typeTextView: TextView = itemView.findViewById(R.id.typeTextView)
        val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        val costTextView: TextView = itemView.findViewById(R.id.costTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactionList[position]
        holder.typeTextView.text = transaction.type
        holder.costTextView.text = "$${transaction.cost}"

        val sdf = java.text.SimpleDateFormat("MM/dd/yyyy")
        holder.dateTextView.text = sdf.format(java.util.Date(transaction.date))
    }

    override fun getItemCount() = transactionList.size
}
