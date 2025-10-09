package com.example.budguette

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
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
        checkLoginStreak()

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

        val monthSdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val currentMonth = monthSdf.format(Date()) // e.g., "2025-09"

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

                    // Transactions store date as a number (milliseconds)
                    val timestampMillis = doc.getLong("date") ?: 0L
                    val date = Date(timestampMillis)

                    // Only include if in current month
                    if (monthSdf.format(date) == currentMonth) {
                        totalExpenses += cost
                        categoryMap[category] = categoryMap.getOrDefault(category, 0.0) + cost
                    }
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
            .addOnFailureListener {
                totalExpensesTextView.text = "Failed to load expenses."
                categoryBreakdownTextView.text = ""
            }
    }

    private fun loadSubscriptionSummary() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val startSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val monthSdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val today = Calendar.getInstance()
        val currentMonth = monthSdf.format(today.time)

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
                    val startDateStr = doc.getString("startDate") ?: continue
                    val startDate = startSdf.parse(startDateStr) ?: continue

                    // Determine first day of current month
                    val calMonthStart = Calendar.getInstance().apply {
                        set(Calendar.YEAR, today.get(Calendar.YEAR))
                        set(Calendar.MONTH, today.get(Calendar.MONTH))
                        set(Calendar.DAY_OF_MONTH, 1)
                    }

                    val monthlyCost = when (frequency) {
                        "One-Time" -> if (monthSdf.format(startDate) == currentMonth) cost else 0.0

                        "Daily" -> {
                            // Count from the later of startDate or start of month
                            val firstDayToCount = if (startDate.after(calMonthStart.time)) startDate else calMonthStart.time
                            val lastDayToCount = today.time

                            val daysCount = ((lastDayToCount.time - firstDayToCount.time) / (1000 * 60 * 60 * 24)).toInt() + 1
                            cost * daysCount
                        }

                        "Weekly" -> {
                            // Count number of weeks elapsed since first week of month
                            val firstDayToCount = if (startDate.after(calMonthStart.time)) startDate else calMonthStart.time

                            val calFirst = Calendar.getInstance()
                            calFirst.time = firstDayToCount

                            val weeksElapsed = ((today.timeInMillis - calFirst.timeInMillis) / (1000L * 60 * 60 * 24 * 7)).toInt() + 1
                            cost * weeksElapsed
                        }

                        "Monthly" -> cost

                        "Yearly" -> {
                            val calStart = Calendar.getInstance()
                            calStart.time = startDate
                            // Only count if this month is the renewal month
                            if (calStart.get(Calendar.MONTH) == today.get(Calendar.MONTH)) cost else 0.0
                        }

                        else -> 0.0
                    }

                    if (monthlyCost > 0) {
                        totalSubscriptions += monthlyCost
                        frequencyMap[frequency] = frequencyMap.getOrDefault(frequency, 0.0) + monthlyCost
                    }
                }

                // Update summary text
                totalSubscriptionsTextView.text =
                    "Total Subscriptions: $${"%.2f".format(totalSubscriptions)}"

                val breakdown = frequencyMap.entries.joinToString("\n") { (freq, sum) ->
                    "$freq: $${"%.2f".format(sum)}"
                }
                subscriptionBreakdownTextView.text = breakdown

                // Pie chart entries
                frequencyMap.entries.forEach {
                    pieEntries.add(PieEntry(it.value.toFloat(), it.key))
                }
                updatePieChart(subscriptionPieChart, pieEntries)
                updateBudgetChart()
            }
            .addOnFailureListener {
                totalSubscriptionsTextView.text = "Failed to load subscriptions."
                subscriptionBreakdownTextView.text = ""
            }
    }




    private fun updateBudgetChart() {
        if (!this::budgetPieChart.isInitialized) return

        val used = totalExpenses + totalSubscriptions
        val remaining = monthlyBudget - used

        // Update remaining budget text
        remainingBudgetTextView.text = if (remaining >= 0) {
            "Remaining Budget: $${"%.2f".format(remaining)}"
        } else {
            "Over Budget: $${"%.2f".format(-remaining)}"
        }

        // Pie chart entries
        val entries = mutableListOf<PieEntry>()
        if (used > 0) entries.add(PieEntry(used.toFloat(), "Used"))
        if (remaining > 0) entries.add(PieEntry(remaining.toFloat(), "Remaining"))
        if (used > monthlyBudget) entries.add(PieEntry((used - monthlyBudget).toFloat(), "Over"))

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(
            android.graphics.Color.parseColor("#FF6347"), // red for used
            android.graphics.Color.parseColor("#32CD32"), // green for remaining
            android.graphics.Color.parseColor("#8B0000")  // dark red for over budget
        )
        dataSet.valueTextColor = android.graphics.Color.WHITE
        dataSet.valueTextSize = 14f

        val data = PieData(dataSet)
        budgetPieChart.data = data
        budgetPieChart.setUsePercentValues(true)
        budgetPieChart.description.isEnabled = false
        budgetPieChart.setDrawEntryLabels(true)
        budgetPieChart.setEntryLabelColor(android.graphics.Color.WHITE)
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
            android.graphics.Color.parseColor("#0A1F44"),  // navy blue
            android.graphics.Color.parseColor("#800080") // purple

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

    private fun checkLoginStreak() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(userId)

        userRef.get().addOnSuccessListener { doc ->
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = sdf.format(Date())

            val lastLogin = doc.getString("lastLoginDate")
            val streak = doc.getLong("loginStreak") ?: 0L

            // Prevent multiple popups in one day
            val prefs = requireContext().getSharedPreferences("streak", 0)
            val lastPopupShown = prefs.getString("lastPopupShown", "")
            if (today == lastPopupShown) return@addOnSuccessListener

            if (lastLogin != today) {
                // Check if yesterday was last login
                val cal = Calendar.getInstance()
                cal.add(Calendar.DATE, -1)
                val yesterday = sdf.format(cal.time)

                val newStreak = if (lastLogin == yesterday) streak + 1 else 1

                // Update Firestore with new streak
                userRef.update(
                    mapOf(
                        "lastLoginDate" to today,
                        "loginStreak" to newStreak
                    )
                ).addOnSuccessListener {
                    // Save highest streak if applicable
                    val highest = doc.getLong("highestStreak") ?: 0L
                    if (newStreak > highest) {
                        userRef.update("highestStreak", newStreak)
                    }

                    // Show popup with streak and tip
                    showStreakPopup(newStreak)

                    // Save today's popup flag
                    prefs.edit().putString("lastPopupShown", today).apply()
                }
            }
        }
    }

    // 🔹 Displays streak + tip popup
    private fun showStreakPopup(streak: Long) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_streak_popup, null)
        val streakText = dialogView.findViewById<TextView>(R.id.streakText)
        val tipText = dialogView.findViewById<TextView>(R.id.tipText)
        val addTipButton = dialogView.findViewById<Button>(R.id.addTipButton)
        val closeButton = dialogView.findViewById<ImageButton>(R.id.closeButton)

        val randomTip = financialTips.random()

        streakText.text = "🔥 Login Streak: $streak day${if (streak != 1L) "s" else ""}!"
        tipText.text = "💡 Tip of the Day:\n$randomTip"

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        addTipButton.setOnClickListener {
            saveTipToProfile(randomTip)
            Toast.makeText(requireContext(), "Tip added to your profile!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        closeButton.setOnClickListener { dialog.dismiss() }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    // 🔹 Saves selected tip to Firestore
    private fun saveTipToProfile(tip: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val tipsRef = db.collection("users").document(userId).collection("tips")

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val tipData = hashMapOf(
            "text" to tip,
            "date" to today
        )

        tipsRef.add(tipData)
    }



    private val financialTips = listOf(
        "Track every expense, no matter how small.",
        "Set aside 10% of every paycheck before spending.",
        "Avoid impulse buys — wait 24 hours before making nonessential purchases.",
        "Review subscriptions monthly and cancel unused ones.",
        "Invest early — compound interest rewards consistency.",
        "Create a budget and stick to it for 30 days.",
        "Automate bill payments to avoid late fees.",
        "Use the 50/30/20 rule: Needs, Wants, Savings."
    )




}

