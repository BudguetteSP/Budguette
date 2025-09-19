package com.example.budguette

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var totalExpensesTextView: TextView
    private lateinit var categoryBreakdownTextView: TextView
    private lateinit var totalSubscriptionsTextView: TextView
    private lateinit var subscriptionBreakdownTextView: TextView
    private lateinit var expensePieChart: PieChart
    private lateinit var subscriptionPieChart: PieChart

    // Budget related
    private lateinit var monthlyBudgetEditText: EditText
    private lateinit var saveBudgetButton: Button
    private lateinit var displayBudgetTextView: TextView
    private lateinit var remainingBudgetTextView: TextView
    private lateinit var budgetPieChart: PieChart
    private var monthlyBudget: Double = 0.0
    private var totalExpenses: Double = 0.0
    private var totalSubscriptions: Double = 0.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Initialize TextViews and other UI elements
        totalExpensesTextView = view.findViewById(R.id.totalExpensesTextView)
        categoryBreakdownTextView = view.findViewById(R.id.categoryBreakdownTextView)
        totalSubscriptionsTextView = view.findViewById(R.id.totalSubscriptionsTextView)
        subscriptionBreakdownTextView = view.findViewById(R.id.subscriptionBreakdownTextView)
        expensePieChart = view.findViewById(R.id.expensePieChart)
        subscriptionPieChart = view.findViewById(R.id.subscriptionPieChart)

        monthlyBudgetEditText = view.findViewById(R.id.monthlyBudgetEditText)
        saveBudgetButton = view.findViewById(R.id.saveBudgetButton)
        displayBudgetTextView = view.findViewById(R.id.displayBudgetTextView)
        remainingBudgetTextView = view.findViewById(R.id.remainingBudgetTextView)
        budgetPieChart = view.findViewById(R.id.budgetPieChart)

        // Display current month
        val monthTextView = view.findViewById<TextView>(R.id.currentMonthTextView)
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        monthTextView.text = sdf.format(Date())

        saveBudgetButton.setOnClickListener {
            saveMonthlyBudget()
        }

        // Load data
        loadMonthlyBudget()
        loadExpenseSummary()
        loadSubscriptionSummary()

        return view
    }

    private fun saveMonthlyBudget() {
        val budgetStr = monthlyBudgetEditText.text.toString()
        if (budgetStr.isEmpty()) return

        monthlyBudget = budgetStr.toDouble()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .document(userId)
            .update("monthlyBudget", monthlyBudget)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Budget saved", Toast.LENGTH_SHORT).show()
                displayBudgetTextView.text = "Monthly Budget: $${"%.2f".format(monthlyBudget)}"
                updateBudgetChart()
            }
            .addOnFailureListener {
                // If document doesn't exist, create it
                val data = hashMapOf("monthlyBudget" to monthlyBudget)
                db.collection("users").document(userId).set(data)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Budget saved", Toast.LENGTH_SHORT).show()
                        displayBudgetTextView.text = "Monthly Budget: $${"%.2f".format(monthlyBudget)}"
                        updateBudgetChart()
                    }
            }
    }

    private fun loadMonthlyBudget() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                monthlyBudget = doc.getDouble("monthlyBudget") ?: 0.0
                displayBudgetTextView.text = "Monthly Budget: $${"%.2f".format(monthlyBudget)}"
                updateBudgetChart()
            }
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
                totalExpenses = 0.0
                val categoryMap = mutableMapOf<String, Double>()
                val pieEntries = mutableListOf<PieEntry>()

                for (doc in querySnapshot) {
                    val cost = doc.getDouble("cost") ?: 0.0
                    val category = doc.getString("category") ?: "Uncategorized"
                    totalExpenses += cost
                    categoryMap[category] = categoryMap.getOrDefault(category, 0.0) + cost
                }

                totalExpensesTextView.text = "Total Expenses: $${"%.2f".format(totalExpenses)}"

                val breakdown = categoryMap.entries.joinToString("\n") { (category, sum) ->
                    "$category: $${"%.2f".format(sum)}"
                }
                categoryBreakdownTextView.text = breakdown

                categoryMap.entries.forEach {
                    pieEntries.add(PieEntry(it.value.toFloat(), it.key))
                }
                updatePieChart(expensePieChart, pieEntries)
                updateBudgetChart()
            }
    }

    private fun loadSubscriptionSummary() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("subscriptions")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                totalSubscriptions = 0.0
                val frequencyMap = mutableMapOf<String, Double>()
                val pieEntries = mutableListOf<PieEntry>()

                for (doc in querySnapshot) {
                    val cost = doc.getDouble("amount") ?: 0.0
                    val frequency = doc.getString("frequency") ?: "Unknown"
                    totalSubscriptions += cost
                    frequencyMap[frequency] = frequencyMap.getOrDefault(frequency, 0.0) + cost
                }

                totalSubscriptionsTextView.text =
                    "Total Subscriptions: $${"%.2f".format(totalSubscriptions)}"

                val breakdown = frequencyMap.entries.joinToString("\n") { (frequency, sum) ->
                    "$frequency: $${"%.2f".format(sum)}"
                }
                subscriptionBreakdownTextView.text = breakdown

                frequencyMap.entries.forEach {
                    pieEntries.add(PieEntry(it.value.toFloat(), it.key))
                }
                updatePieChart(subscriptionPieChart, pieEntries)
                updateBudgetChart()
            }
    }

    private fun updateBudgetChart() {
        if (!this::budgetPieChart.isInitialized) return

        val used = totalExpenses + totalSubscriptions
        val remaining = monthlyBudget - used
        remainingBudgetTextView.text = "Remaining Budget: $${"%.2f".format(remaining)}"

        val entries = listOf(
            PieEntry(used.toFloat(), "Used"),
            PieEntry(if (remaining >= 0) remaining.toFloat() else 0f, "Remaining")
        )
        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(
            android.graphics.Color.parseColor("#FF6347"), // red for used
            android.graphics.Color.parseColor("#32CD32")  // green for remaining
        )
        dataSet.valueTextColor = android.graphics.Color.WHITE  // make slice values visible
        dataSet.valueTextSize = 14f

        val data = PieData(dataSet)
        budgetPieChart.data = data
        budgetPieChart.setUsePercentValues(true)
        budgetPieChart.description.isEnabled = false
        budgetPieChart.setDrawEntryLabels(true)
        budgetPieChart.setEntryLabelColor(android.graphics.Color.WHITE) // slice labels inside chart
        budgetPieChart.setEntryLabelTextSize(12f)
        budgetPieChart.isDrawHoleEnabled = true
        budgetPieChart.setHoleColor(android.graphics.Color.parseColor("#0A1F44"))
        budgetPieChart.holeRadius = 45f
        budgetPieChart.transparentCircleRadius = 50f

        // LEGEND styling
        val legend = budgetPieChart.legend
        legend.isEnabled = true
        legend.textColor = android.graphics.Color.WHITE
        legend.textSize = 12f
        legend.form = com.github.mikephil.charting.components.Legend.LegendForm.CIRCLE
        legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
        legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER

        budgetPieChart.animateY(1000)
        budgetPieChart.invalidate()
    }


    private fun updatePieChart(pieChart: PieChart, entries: List<PieEntry>) {
        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(
            android.graphics.Color.parseColor("#FFD700"), // gold
            android.graphics.Color.parseColor("#87CEEB"), // sky blue
            android.graphics.Color.parseColor("#32CD32"), // lime green
            android.graphics.Color.parseColor("#FF4500"), // orange red
            android.graphics.Color.parseColor("#0A1F44")  // navy blue
        )
        dataSet.valueTextColor = android.graphics.Color.BLACK
        dataSet.valueTextSize = 14f

        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.setUsePercentValues(false)
        pieChart.description.isEnabled = false
        pieChart.setDrawEntryLabels(true)
        pieChart.setEntryLabelColor(android.graphics.Color.BLACK)
        pieChart.setEntryLabelTextSize(12f)
        pieChart.isDrawHoleEnabled = true
        pieChart.setHoleColor(android.graphics.Color.parseColor("#0A1F44"))
        pieChart.holeRadius = 45f
        pieChart.transparentCircleRadius = 50f

        // Correctly retrieve legend and set its text color
        val legend = pieChart.legend
        legend.isEnabled = true
        legend.textColor = android.graphics.Color.WHITE
        legend.textSize = 12f
        legend.form = com.github.mikephil.charting.components.Legend.LegendForm.CIRCLE
        legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
        legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER

        pieChart.animateY(1000)
        pieChart.invalidate()
    }

}

