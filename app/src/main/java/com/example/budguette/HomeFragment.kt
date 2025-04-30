package com.example.budguette

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class HomeFragment : Fragment() {

    private lateinit var totalExpensesTextView: TextView
    private lateinit var categoryBreakdownTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        totalExpensesTextView = view.findViewById(R.id.totalExpensesTextView)
        categoryBreakdownTextView = view.findViewById(R.id.categoryBreakdownTextView)

        loadExpenseSummary()

        return view
    }

    private fun loadExpenseSummary() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .document(userId)
            .collection("transactions")
            .whereEqualTo("type", "Expense")
            .get()
            .addOnSuccessListener { querySnapshot ->
                var totalCost = 0.0
                val categoryMap = mutableMapOf<String, Double>()

                for (doc in querySnapshot) {
                    val cost = doc.getDouble("cost") ?: 0.0
                    val category = doc.getString("category") ?: "Uncategorized"

                    totalCost += cost
                    categoryMap[category] = categoryMap.getOrDefault(category, 0.0) + cost
                }

                // Update total
                totalExpensesTextView.text = "Total Expenses: $${"%.2f".format(totalCost)}"

                // Update category breakdown
                val breakdown = categoryMap.entries.joinToString("\n") { (category, sum) ->
                    "$category: $${"%.2f".format(sum)}"
                }
                categoryBreakdownTextView.text = breakdown
            }
            .addOnFailureListener {
                totalExpensesTextView.text = "Failed to load expenses."
                categoryBreakdownTextView.text = ""
            }
    }
}
