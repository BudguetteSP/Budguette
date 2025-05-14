package com.example.budguette

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

class HomeFragment : Fragment() {

    private lateinit var totalExpensesTextView: TextView
    private lateinit var categoryBreakdownTextView: TextView
    private lateinit var totalSubscriptionsTextView: TextView
    private lateinit var subscriptionBreakdownTextView: TextView
    private lateinit var expensePieChart: PieChart
    private lateinit var subscriptionPieChart: PieChart

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Initialize TextViews
        totalExpensesTextView = view.findViewById(R.id.totalExpensesTextView)
        categoryBreakdownTextView = view.findViewById(R.id.categoryBreakdownTextView)
        totalSubscriptionsTextView = view.findViewById(R.id.totalSubscriptionsTextView)
        subscriptionBreakdownTextView = view.findViewById(R.id.subscriptionBreakdownTextView)

        // Initialize PieCharts
        expensePieChart = view.findViewById(R.id.expensePieChart)
        subscriptionPieChart = view.findViewById(R.id.subscriptionPieChart)

        loadExpenseSummary()
        loadSubscriptionSummary()

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
                val pieEntries = mutableListOf<PieEntry>()

                for (doc in querySnapshot) {
                    val cost = doc.getDouble("cost") ?: 0.0
                    val category = doc.getString("category") ?: "Uncategorized"

                    totalCost += cost
                    categoryMap[category] = categoryMap.getOrDefault(category, 0.0) + cost
                }

                // Update total expenses
                totalExpensesTextView.text = "Total Expenses: $${"%.2f".format(totalCost)}"

                // Update category breakdown
                val breakdown = categoryMap.entries.joinToString("\n") { (category, sum) ->
                    "$category: $${"%.2f".format(sum)}"
                }
                categoryBreakdownTextView.text = breakdown

                // Update Pie Chart
                categoryMap.entries.forEach {
                    pieEntries.add(PieEntry(it.value.toFloat(), it.key))
                }
                updatePieChart(expensePieChart, pieEntries)
            }
            .addOnFailureListener {
                totalExpensesTextView.text = "Failed to load expenses."
                categoryBreakdownTextView.text = ""
            }
    }

    private fun loadSubscriptionSummary() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("subscriptions")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                var totalCost = 0.0
                val frequencyMap = mutableMapOf<String, Double>()
                val pieEntries = mutableListOf<PieEntry>()

                for (doc in querySnapshot) {
                    val cost = doc.getDouble("amount") ?: 0.0
                    val frequency = doc.getString("frequency") ?: "Unknown"

                    totalCost += cost
                    frequencyMap[frequency] = frequencyMap.getOrDefault(frequency, 0.0) + cost
                }

                // Update total subscriptions
                totalSubscriptionsTextView.text = "Total Subscriptions: $${"%.2f".format(totalCost)}"

                // Update frequency breakdown
                val breakdown = frequencyMap.entries.joinToString("\n") { (frequency, sum) ->
                    "$frequency: $${"%.2f".format(sum)}"
                }
                subscriptionBreakdownTextView.text = breakdown

                // Update Pie Chart
                frequencyMap.entries.forEach {
                    pieEntries.add(PieEntry(it.value.toFloat(), it.key))
                }
                updatePieChart(subscriptionPieChart, pieEntries)
            }
            .addOnFailureListener {
                totalSubscriptionsTextView.text = "Failed to load subscriptions."
                subscriptionBreakdownTextView.text = ""
            }
    }

    private fun updatePieChart(pieChart: PieChart, entries: List<PieEntry>) {
        val dataSet = PieDataSet(entries, "")

        // Use themed colors
        val chartColors = listOf(
            android.graphics.Color.parseColor("#FFD700"), // gold
            android.graphics.Color.parseColor("#D3D3D3"), // light grey
            android.graphics.Color.parseColor("#FFFFFF"), // white
            android.graphics.Color.parseColor("#000000"), // black
            android.graphics.Color.parseColor("#0A1F44")  // navy blue
        )
        dataSet.colors = chartColors

        // Value appearance
        dataSet.valueTextColor = android.graphics.Color.BLACK
        dataSet.valueTextSize = 14f

        val data = PieData(dataSet)
        pieChart.data = data

        // Center hole color and text
        pieChart.setUsePercentValues(false)
        pieChart.description.isEnabled = false
        pieChart.setDrawEntryLabels(true)
        pieChart.setEntryLabelColor(android.graphics.Color.BLACK)
        pieChart.setEntryLabelTextSize(12f)

        // Center styling
        pieChart.isDrawHoleEnabled = true
        pieChart.setHoleColor(android.graphics.Color.parseColor("#0A1F44")) // navy blue
        pieChart.holeRadius = 45f
        pieChart.transparentCircleRadius = 50f

        // Legend styling
        val legend = pieChart.legend
        legend.isEnabled = true
        legend.textColor = android.graphics.Color.WHITE
        legend.textSize = 12f
        legend.form = com.github.mikephil.charting.components.Legend.LegendForm.CIRCLE
        legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
        legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER

        // Animate
        pieChart.animateY(1000)

        // Refresh
        pieChart.invalidate()
    }

}


