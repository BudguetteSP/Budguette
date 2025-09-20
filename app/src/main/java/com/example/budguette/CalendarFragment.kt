package com.example.budguette

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.prolificinteractive.materialcalendarview.*
import com.prolificinteractive.materialcalendarview.spans.DotSpan
import java.text.SimpleDateFormat
import java.util.*

class CalendarFragment : Fragment() {

    private lateinit var calendarView: MaterialCalendarView
    private lateinit var summaryText: TextView
    private lateinit var startDateBtn: Button
    private lateinit var endDateBtn: Button
    private lateinit var applyFilterBtn: Button
    private lateinit var tabLayout: TabLayout
    private lateinit var legendIcon: ImageView

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val subscriptionColors = mapOf(
        "Daily" to 0xFF800080.toInt(),      // Purple
        "Weekly" to 0xFF0000FF.toInt(),     // Blue
        "Monthly" to 0xFFFF0000.toInt(),    // Red
        "Yearly" to 0xFF00FF00.toInt(),     // Green
        "One-Time" to 0xFFFFA500.toInt()    // Orange
    )
    private val expenseColor = 0xFFFF0000.toInt() // Red
    private val depositColor = 0xFF00FF00.toInt() // Green

    private var allSubscriptions: List<Subscription> = emptyList()
    private var allTransactions: List<Transaction> = emptyList()
    private var startDate: Date? = null
    private var endDate: Date? = null
    private var currentTab = "Subscriptions"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_calendar, container, false)
        calendarView = view.findViewById(R.id.calendarView)
        summaryText = view.findViewById(R.id.summaryText)
        startDateBtn = view.findViewById(R.id.startDateBtn)
        endDateBtn = view.findViewById(R.id.endDateBtn)
        applyFilterBtn = view.findViewById(R.id.applyFilterBtn)
        tabLayout = view.findViewById(R.id.tabLayout)
        legendIcon = view.findViewById(R.id.legendIcon)

        // Tabs setup
        tabLayout.addTab(tabLayout.newTab().setText("Subscriptions"))
        tabLayout.addTab(tabLayout.newTab().setText("Expenses"))
        tabLayout.addTab(tabLayout.newTab().setText("Deposits"))
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentTab = tab.text.toString()
                decorateCalendarForTab()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // Legend click
        legendIcon.setOnClickListener { showLegendDialog() }

        // Date pickers
        startDateBtn.setOnClickListener { pickDate(true) }
        endDateBtn.setOnClickListener { pickDate(false) }
        applyFilterBtn.setOnClickListener { applyFilter() }

        fetchSubscriptions()
        fetchTransactions()

        return view
    }

    private fun pickDate(isStart: Boolean) {
        val cal = Calendar.getInstance()
        DatePickerDialog(requireContext(),
            { _, year, month, dayOfMonth ->
                cal.set(year, month, dayOfMonth)
                val date = cal.time
                if (isStart) {
                    startDate = date
                    startDateBtn.text = sdf.format(date)
                } else {
                    endDate = date
                    endDateBtn.text = sdf.format(date)
                }
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun fetchSubscriptions() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("subscriptions")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                allSubscriptions = snapshot.documents.map { doc ->
                    Subscription(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        name = doc.getString("name") ?: "",
                        amount = doc.getDouble("amount") ?: 0.0,
                        frequency = doc.getString("frequency") ?: "",
                        startDate = doc.getString("startDate") ?: "",
                        notes = doc.getString("notes") ?: ""
                    )
                }
                decorateCalendarForTab()
            }
    }

    private fun fetchTransactions() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("transactions")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                allTransactions = snapshot.documents.map { doc ->
                    Transaction(
                        id = doc.id,
                        type = doc.getString("type") ?: "",
                        name = doc.getString("name") ?: "",
                        date = doc.getLong("date") ?: 0L,
                        cost = doc.getDouble("cost") ?: 0.0,
                        notes = doc.getString("notes") ?: "",
                        category = doc.getString("category") ?: ""
                    )
                }
                decorateCalendarForTab()
            }
    }

    private fun applyFilter() {
        if (startDate == null || endDate == null) {
            summaryText.text = getString(R.string.select_valid_range)
            return
        }

        when (currentTab) {
            "Subscriptions" -> {
                val filtered = allSubscriptions.filter { sub ->
                    val subDate = sdf.parse(sub.startDate) ?: return@filter false
                    when (sub.frequency) {
                        "One-Time" -> !subDate.before(startDate) && !subDate.after(endDate)
                        "Daily", "Weekly", "Monthly", "Yearly" -> !subDate.after(endDate)
                        else -> false
                    }
                }
                decorateSubscriptions(filtered)
            }
            "Expenses" -> {
                val filtered = allTransactions.filter { txn ->
                    txn.type.equals("expense", true) &&
                            Date(txn.date).let { !it.before(startDate) && !it.after(endDate) }
                }
                decorateTransactions(filtered, "expense", expenseColor)
            }
            "Deposits" -> {
                val filtered = allTransactions.filter { txn ->
                    txn.type.equals("deposit", true) &&
                            Date(txn.date).let { !it.before(startDate) && !it.after(endDate) }
                }
                decorateTransactions(filtered, "deposit", depositColor)
            }
        }

        updateSummaryForTab()
    }

    private fun decorateCalendarForTab() {
        calendarView.removeDecorators()
        when (currentTab) {
            "Subscriptions" -> decorateSubscriptions(allSubscriptions)
            "Expenses" -> decorateTransactions(allTransactions, "expense", expenseColor)
            "Deposits" -> decorateTransactions(allTransactions, "deposit", depositColor)
        }

        setupDayClick(allSubscriptions, allTransactions)
        updateSummaryForTab()
    }

    private fun decorateSubscriptions(subscriptions: List<Subscription>) {
        val subsByDate = subscriptions.groupBy { it.startDate }
        calendarView.removeDecorators()

        for ((dateString, subs) in subsByDate) {
            val date = sdf.parse(dateString) ?: continue
            val day = CalendarDay.from(date)
            for (sub in subs) {
                calendarView.addDecorator(object : DayViewDecorator {
                    override fun shouldDecorate(dayToCheck: CalendarDay) = dayToCheck == day
                    override fun decorate(view: DayViewFacade) {
                        view.addSpan(DotSpan(6f, subscriptionColors[sub.frequency] ?: 0xFF000000.toInt()))
                    }
                })
            }
        }

        // Vertical summary
        val totals = subscriptions.groupBy { it.frequency }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        summaryText.text = """
            Daily: ${totals["Daily"] ?: 0.0}
            Weekly: ${totals["Weekly"] ?: 0.0}
            Monthly: ${totals["Monthly"] ?: 0.0}
            Yearly: ${totals["Yearly"] ?: 0.0}
            One-Time: ${totals["One-Time"] ?: 0.0}
        """.trimIndent()
    }

    private fun decorateTransactions(transactions: List<Transaction>, type: String, color: Int) {
        val txnsByDate = transactions.filter { it.type.equals(type, true) }
            .groupBy { sdf.format(Date(it.date)) }
        calendarView.removeDecorators()

        var total = 0.0
        for ((dateString, txns) in txnsByDate) {
            val date = sdf.parse(dateString) ?: continue
            val day = CalendarDay.from(date)
            for (txn in txns) {
                calendarView.addDecorator(object : DayViewDecorator {
                    override fun shouldDecorate(dayToCheck: CalendarDay) = dayToCheck == day
                    override fun decorate(view: DayViewFacade) {
                        view.addSpan(DotSpan(6f, color))
                    }
                })
            }
            total += txns.sumOf { it.cost }
        }

        val label = if (type == "expense") "Expenses" else "Deposits"
        summaryText.text = "$label Total: $${"%.2f".format(total)}"
    }

    private fun showLegendDialog() {
        val builder = SpannableStringBuilder()
        when (currentTab) {
            "Subscriptions" -> {
                for ((label, color) in subscriptionColors) {
                    val dot = SpannableString("● ")
                    dot.setSpan(ForegroundColorSpan(color), 0, dot.length, 0)
                    builder.append(dot).append(label).append("\n")
                }
            }
            "Expenses" -> {
                val dot = SpannableString("● ")
                dot.setSpan(ForegroundColorSpan(expenseColor), 0, dot.length, 0)
                builder.append(dot).append("Expenses\n")
            }
            "Deposits" -> {
                val dot = SpannableString("● ")
                dot.setSpan(ForegroundColorSpan(depositColor), 0, dot.length, 0)
                builder.append(dot).append("Deposits\n")
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Legend")
            .setMessage(builder)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun setupDayClick(subscriptions: List<Subscription>, transactions: List<Transaction>) {
        calendarView.setOnDateChangedListener { _, date, _ ->
            val clickedDate = sdf.format(date.date)
            val messageBuilder = StringBuilder()

            if (currentTab == "Subscriptions") {
                val subsForDay = subscriptions.filter { it.startDate == clickedDate }
                if (subsForDay.isNotEmpty()) {
                    for (sub in subsForDay) {
                        messageBuilder.append("${sub.name} - $${sub.amount} (${sub.frequency})\n")
                    }
                }
            } else if (currentTab == "Expenses") {
                val expensesForDay = transactions.filter {
                    it.type.equals("expense", true) && sdf.format(Date(it.date)) == clickedDate
                }
                if (expensesForDay.isNotEmpty()) {
                    for (txn in expensesForDay) {
                        messageBuilder.append("${txn.name} - $${txn.cost} (${txn.category})\n")
                    }
                }
            } else if (currentTab == "Deposits") {
                val depositsForDay = transactions.filter {
                    it.type.equals("deposit", true) && sdf.format(Date(it.date)) == clickedDate
                }
                if (depositsForDay.isNotEmpty()) {
                    for (txn in depositsForDay) {
                        messageBuilder.append("${txn.name} - $${txn.cost} (${txn.category})\n")
                    }
                }
            }

            if (messageBuilder.isNotEmpty()) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Details for $clickedDate")
                    .setMessage(messageBuilder.toString())
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun updateSummaryForTab() {
        if (currentTab == "Subscriptions") {
            val filteredSubs = allSubscriptions.filter { sub ->
                val subDate = sdf.parse(sub.startDate) ?: return@filter false
                (startDate == null || !subDate.before(startDate)) &&
                        (endDate == null || !subDate.after(endDate))
            }

            val totals = filteredSubs.groupBy { it.frequency }
                .mapValues { entry -> entry.value.sumOf { it.amount } }

            val daily = totals["Daily"] ?: 0.0
            val weekly = totals["Weekly"] ?: 0.0
            val monthly = totals["Monthly"] ?: 0.0
            val yearly = totals["Yearly"] ?: 0.0
            val oneTime = totals["One-Time"] ?: 0.0

            summaryText.text = """
            Daily: $daily
            Weekly: $weekly
            Monthly: $monthly
            Yearly: $yearly
            One-Time: $oneTime
        """.trimIndent()

        } else if (currentTab == "Expenses") {
            val filteredTxns = allTransactions.filter { txn ->
                txn.type.equals("expense", true) &&
                        (startDate == null || Date(txn.date) >= startDate) &&
                        (endDate == null || Date(txn.date) <= endDate)
            }
            val total = filteredTxns.sumOf { it.cost }
            summaryText.text = "Expenses Total: $${"%.2f".format(total)}"

        } else if (currentTab == "Deposits") {
            val filteredTxns = allTransactions.filter { txn ->
                txn.type.equals("deposit", true) &&
                        (startDate == null || Date(txn.date) >= startDate) &&
                        (endDate == null || Date(txn.date) <= endDate)
            }
            val total = filteredTxns.sumOf { it.cost }
            summaryText.text = "Deposits Total: $${"%.2f".format(total)}"
        }
    }


}
