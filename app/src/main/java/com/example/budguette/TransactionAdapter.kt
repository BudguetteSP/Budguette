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

    private var currentQuery: String? = null

    // Define a click listener type
    private var onItemClickListener: ((Transaction) -> Unit)? = null

    // ViewHolder class for each transaction item
    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.transactionName)
        val typeTextView: TextView = itemView.findViewById(R.id.transactionType)
        val dateTextView: TextView = itemView.findViewById(R.id.transactionDate)
        val costTextView: TextView = itemView.findViewById(R.id.transactionCost)
    }

    // This method allows you to set the item click listener
    fun setOnItemClickListener(listener: (Transaction) -> Unit) {
        onItemClickListener = listener
    }

    // Method to update the transaction list and refresh the adapter
    fun updateList(newList: List<Transaction>, query: String? = null) {
        transactions = newList
        currentQuery = query
        notifyDataSetChanged()
    }

    // Called to create a ViewHolder when a new item is needed
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    // Bind the data to the views in each ViewHolder
    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]

        // Highlight text based on the query
        val query = currentQuery ?: ""
        highlightText(holder.nameTextView, transaction.name, query)
        highlightText(holder.typeTextView, transaction.type, query)

        holder.dateTextView.text = android.text.format.DateFormat.format("MMM dd, yyyy", transaction.date)
        holder.costTextView.text = "$${transaction.cost}"

        // Color the text based on the transaction type
        if (transaction.type.equals("Deposit", ignoreCase = true)) {
            holder.costTextView.setTextColor(holder.itemView.context.getColor(R.color.greenDeposit))
        } else {
            holder.costTextView.setTextColor(holder.itemView.context.getColor(R.color.redExpense))
        }

        // Handle item click
        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(transaction)  // Trigger the listener with the clicked transaction
        }
    }

    // Method to highlight the matching text based on the search query
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


