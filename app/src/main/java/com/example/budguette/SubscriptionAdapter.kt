package com.example.budguette

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.app.DatePickerDialog
import android.content.Context
import java.util.*

class SubscriptionAdapter(private val subscriptions: List<Subscription>, private val onDueDateSelected: (Subscription, String) -> Unit) :
    RecyclerView.Adapter<SubscriptionAdapter.SubscriptionViewHolder>() {

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
        val subscription = subscriptions[position]
        holder.nameText.text = subscription.name
        holder.costText.text = "Cost: $${subscription.amount}"
        holder.dueText.text = "Due: ${subscription.startDate}"  // assuming dueDate is the startDate

        holder.dueText.setOnClickListener {
            // Show the DatePickerDialog when the user clicks on the due date
            showDatePickerDialog(holder.dueText.context, subscription) { selectedDate ->
                // Update the TextView with the selected date
                holder.dueText.text = "Due: $selectedDate"
                // Callback to pass the selected date back to the activity/fragment
                onDueDateSelected(subscription, selectedDate)
            }
        }

        holder.notesText.text = "Notes: ${subscription.notes}"
    }

    override fun getItemCount(): Int = subscriptions.size

    // Function to show the DatePickerDialog
    private fun showDatePickerDialog(context: Context, subscription: Subscription, onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                // Format the selected date
                val formattedDate = "$selectedYear-${(selectedMonth + 1).toString().padStart(2, '0')}-${selectedDayOfMonth.toString().padStart(2, '0')}"
                onDateSelected(formattedDate)
            },
            year, month, day
        )
        datePickerDialog.show()
    }
}


