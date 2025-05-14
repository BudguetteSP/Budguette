package com.example.budguette

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class SubscriptionAdapter(
    private val onDueDateSelected: (Subscription, String) -> Unit
) : RecyclerView.Adapter<SubscriptionAdapter.SubscriptionViewHolder>(), Filterable {

    private val originalList: MutableList<Subscription> = mutableListOf()
    private var filteredList: MutableList<Subscription> = mutableListOf()

    inner class SubscriptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.subscriptionName)
        val costText: TextView = itemView.findViewById(R.id.subscriptionCost)
        val dueText: TextView = itemView.findViewById(R.id.subscriptionDueDate)
        val notesText: TextView = itemView.findViewById(R.id.subscriptionNotes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubscriptionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subscription, parent, false)
        return SubscriptionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubscriptionViewHolder, position: Int) {
        val subscription = filteredList[position]
        holder.nameText.text = subscription.name
        holder.costText.text = "Cost: $${subscription.amount}"
        holder.dueText.text = "Due: ${subscription.startDate}"
        holder.notesText.text = "Notes: ${subscription.notes}"

        holder.dueText.setOnClickListener {
            showDatePickerDialog(holder.dueText.context, subscription) { selectedDate ->
                holder.dueText.text = "Due: $selectedDate"
                onDueDateSelected(subscription, selectedDate)
            }
        }

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, SubscriptionDetailActivity::class.java)
            intent.putExtra("subscription", subscription)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = filteredList.size

    fun setSubscriptions(newList: List<Subscription>) {
        originalList.clear()
        originalList.addAll(newList)

        filteredList.clear()
        filteredList.addAll(newList)

        notifyDataSetChanged()
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(query: CharSequence?): FilterResults {
                val resultList = if (query.isNullOrEmpty()) {
                    originalList
                } else {
                    val lowerQuery = query.toString().lowercase(Locale.getDefault())
                    originalList.filter {
                        it.name.lowercase(Locale.getDefault()).contains(lowerQuery)
                    }
                }

                val filterResults = FilterResults()
                filterResults.values = resultList
                return filterResults
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(query: CharSequence?, results: FilterResults?) {
                filteredList = (results?.values as? List<Subscription>)?.toMutableList() ?: mutableListOf()
                notifyDataSetChanged()
            }
        }
    }

    private fun showDatePickerDialog(context: Context, subscription: Subscription, onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                val formattedDate = "$selectedYear-${(selectedMonth + 1).toString().padStart(2, '0')}-${selectedDayOfMonth.toString().padStart(2, '0')}"
                onDateSelected(formattedDate)
            },
            year, month, day
        )
        datePickerDialog.show()
    }
}




