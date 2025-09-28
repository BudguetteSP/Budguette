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

    // Current filters
    private var searchQuery: String = ""
    private var frequencyFilter: String = "All"

    inner class SubscriptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.subscriptionName)
        val costText: TextView = itemView.findViewById(R.id.subscriptionCost)
        val frequencyText: TextView = itemView.findViewById(R.id.subscriptionFrequency)
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
        holder.frequencyText.text = "Frequency: ${subscription.frequency}"
        holder.dueText.text = "Due: ${subscription.startDate}"
        holder.notesText.text = "Notes: ${subscription.notes}"

        holder.dueText.setOnClickListener {
            showDatePickerDialog(holder.dueText.context) { selectedDate ->
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
        applyFilters()
    }

    fun applySearch(query: String) {
        searchQuery = query
        applyFilters()
    }

    fun applyFrequency(frequency: String) {
        frequencyFilter = frequency
        applyFilters()
    }

    private fun applyFilters() {
        filteredList = originalList.filter { subscription ->
            val matchesFrequency = (frequencyFilter == "All") || (subscription.frequency == frequencyFilter)
            val matchesSearch = subscription.name.contains(searchQuery, ignoreCase = true)
            matchesFrequency && matchesSearch
        }.toMutableList()
        notifyDataSetChanged()
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(query: CharSequence?): FilterResults {
                val q = query?.toString() ?: ""
                applySearch(q)
                return FilterResults().apply { values = filteredList }
            }

            override fun publishResults(query: CharSequence?, results: FilterResults?) {}
        }
    }

    private fun showDatePickerDialog(context: Context, onDateSelected: (String) -> Unit) {
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






