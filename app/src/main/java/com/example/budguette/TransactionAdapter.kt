package com.example.budguette

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TransactionAdapter(private val transactions: List<Transaction>) :
    RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.transactionName)
        val typeTextView: TextView = itemView.findViewById(R.id.transactionType)
        val dateTextView: TextView = itemView.findViewById(R.id.transactionDate)
        val costTextView: TextView = itemView.findViewById(R.id.transactionCost)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.nameTextView.text = transaction.name
        holder.typeTextView.text = transaction.type
        holder.dateTextView.text = android.text.format.DateFormat.format("MMM dd, yyyy", transaction.date)
        holder.costTextView.text = "$${transaction.cost}"

        if (transaction.type.equals("Deposit", ignoreCase = true)) {
            holder.costTextView.setTextColor(holder.itemView.context.getColor(R.color.greenDeposit))
            holder.costTextView.setTypeface(null, Typeface.BOLD)
        } else {
            holder.costTextView.setTextColor(holder.itemView.context.getColor(R.color.redExpense))
            holder.costTextView.setTypeface(null, Typeface.BOLD)
        }
    }

    override fun getItemCount() = transactions.size
}

