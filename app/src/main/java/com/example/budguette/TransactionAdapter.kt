package com.example.budguette

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TransactionAdapter(private var transactions: List<Transaction>) :
    RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {
    // Now transactions is mutable, allowing reassignment in the updateList method

    private var allTransactions = ArrayList(transactions)
    private var currentQuery: String? = null


    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.transactionName)
        val typeTextView: TextView = itemView.findViewById(R.id.transactionType)
        val dateTextView: TextView = itemView.findViewById(R.id.transactionDate)
        val costTextView: TextView = itemView.findViewById(R.id.transactionCost)
    }

    fun updateList(newList: List<Transaction>, query: String? = null) {
        transactions = newList
        currentQuery = query
        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]

        val query = currentQuery ?: "" // We'll store the search query globally
        highlightText(holder.nameTextView, transaction.name, query)
        highlightText(holder.typeTextView, transaction.type, query)

        holder.dateTextView.text = android.text.format.DateFormat.format("MMM dd, yyyy", transaction.date)
        holder.costTextView.text = "$${transaction.cost}"

        if (transaction.type.equals("Deposit", ignoreCase = true)) {
            holder.costTextView.setTextColor(holder.itemView.context.getColor(R.color.greenDeposit))
        } else {
            holder.costTextView.setTextColor(holder.itemView.context.getColor(R.color.redExpense))
        }
    }

    private fun highlightText(textView: TextView, fullText: String, query: String) {
        if (query.isEmpty()) {
            textView.text = fullText
            return
        }

        val startIndex = fullText.indexOf(query, ignoreCase = true)
        if (startIndex == -1) {
            textView.text = fullText
            return
        }

        val endIndex = startIndex + query.length
        val spannable = SpannableString(fullText)
        spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            startIndex,
            endIndex,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        textView.text = spannable
    }



    override fun getItemCount() = transactions.size
}

