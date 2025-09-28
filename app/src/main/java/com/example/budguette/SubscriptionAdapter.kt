package com.example.budguette

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filterable
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class SubscriptionAdapter(
    private val onDueDateSelected: (Subscription, String) -> Unit
) : RecyclerView.Adapter<SubscriptionAdapter.SubscriptionViewHolder>(), Filterable {

    private val originalList: MutableList<Subscription> = mutableListOf()
    private var filteredList: MutableList<Subscription> = mutableListOf()

    private var searchQuery: String = ""
    private var frequencyFilter: String = "All"
    private var amountFilter: String = "All"
    private var dueFilter: String = "All"

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

    fun applyAmount(amount: String) {
        amountFilter = amount
        applyFilters()
    }

    fun applyDue(due: String) {
        dueFilter = due
        applyFilters()
    }

    private fun applyFilters() {
        val today = Calendar.getInstance().time
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        filteredList = originalList.filter { subscription ->
            val matchesFrequency = (frequencyFilter == "All") || (subscription.frequency == frequencyFilter)
            val matchesSearch = subscription.name.contains(searchQuery, ignoreCase = true)
            val matchesAmount = when (amountFilter) {
                "<5" -> subscription.amount < 5
                "5-20" -> subscription.amount in 5.0..20.0
                "20-50" -> subscription.amount in 20.0..50.0
                ">50" -> subscription.amount > 50
                else -> true
            }

            val matchesDue = when (dueFilter) {
                "Today" -> subscription.startDate == sdf.format(today)
                "This Week" -> {
                    val cal = Calendar.getInstance()
                    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                    val startOfWeek = Calendar.getInstance()
                    startOfWeek.add(Calendar.DAY_OF_MONTH, -(dayOfWeek - 1))
                    val endOfWeek = Calendar.getInstance()
                    endOfWeek.add(Calendar.DAY_OF_MONTH, 7 - dayOfWeek)
                    val subDate = sdf.parse(subscription.startDate) ?: today
                    subDate in startOfWeek.time..endOfWeek.time
                }
                "This Month" -> {
                    val cal = Calendar.getInstance()
                    val month = cal.get(Calendar.MONTH)
                    val year = cal.get(Calendar.YEAR)
                    val subDate = sdf.parse(subscription.startDate) ?: today
                    val subCal = Calendar.getInstance()
                    subCal.time = subDate
                    subCal.get(Calendar.MONTH) == month && subCal.get(Calendar.YEAR) == year
                }
                else -> true
            }

            matchesFrequency && matchesSearch && matchesAmount && matchesDue
        }.toMutableList()

        notifyDataSetChanged()
    }

    override fun getFilter(): android.widget.Filter {
        return object : android.widget.Filter() {
            override fun performFiltering(query: CharSequence?): android.widget.Filter.FilterResults {
                applySearch(query?.toString() ?: "")
                return android.widget.Filter.FilterResults().apply { values = filteredList }
            }

            override fun publishResults(query: CharSequence?, results: android.widget.Filter.FilterResults?) {}
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






