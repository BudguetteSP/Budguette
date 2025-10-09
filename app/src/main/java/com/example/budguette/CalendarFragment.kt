package com.example.budguette

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.core.content.ContextCompat
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

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val subscriptionColors = mapOf(
        "Daily" to 0xFF800080.toInt(),
        "Weekly" to 0xFF0000FF.toInt(),
        "Monthly" to 0xFFFF0000.toInt(),
        "Yearly" to 0xFF00FF00.toInt(),
        "One-Time" to 0xFFFFA500.toInt()
    )
    private val expenseColor = 0xFFFF0000.toInt()
    private val depositColor = 0xFF00FF00.toInt()

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
        val legendIcon: ImageView = view.findViewById(R.id.legendIcon)

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

        legendIcon.setOnClickListener { showLegendDialog() }

        startDateBtn.setOnClickListener { pickDate(true) }
        endDateBtn.setOnClickListener { pickDate(false) }
        applyFilterBtn.setOnClickListener { decorateCalendarForTab() }

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

                // 🔔 Schedule reminders for all subscriptions
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    allSubscriptions.forEach { scheduleReminder(it) }
                }
            }
    }

    private fun fetchTransactions() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users")
            .document(userId)
            .collection("transactions")
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
            .addOnFailureListener { e ->
                summaryText.text = "Failed to fetch transactions: ${e.message}"
            }
    }

    private fun decorateCalendarForTab() {
        calendarView.removeDecorators()
        when (currentTab) {
            "Subscriptions" -> decorateSubscriptions()
            "Expenses" -> decorateTransactions("Expense", expenseColor)
            "Deposits" -> decorateTransactions("Deposit", depositColor)
        }
    }

    private fun decorateSubscriptions() {
        val filteredSubs = allSubscriptions.filter { sub ->
            val subDate = sdf.parse(sub.startDate) ?: return@filter false
            (startDate == null || !subDate.before(startDate)) &&
                    (endDate == null || !subDate.after(endDate))
        }

        val subsByDate = filteredSubs.groupBy { it.startDate }

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

        calendarView.setOnDateChangedListener { _, date, _ ->
            val clickedDate = sdf.format(date.date)
            val subsForDay = subsByDate[clickedDate]
            if (!subsForDay.isNullOrEmpty()) {
                val message = subsForDay.joinToString("\n") {
                    "${it.name} - $${it.amount} (${it.frequency})"
                }
                AlertDialog.Builder(requireContext())
                    .setTitle("Subscriptions on $clickedDate")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }

        val totals = filteredSubs.groupBy { it.frequency }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
        summaryText.text = """
            Daily: ${totals["Daily"] ?: 0.0}
            Weekly: ${totals["Weekly"] ?: 0.0}
            Monthly: ${totals["Monthly"] ?: 0.0}
            Yearly: ${totals["Yearly"] ?: 0.0}
            One-Time: ${totals["One-Time"] ?: 0.0}
        """.trimIndent()
    }

    private fun decorateTransactions(type: String, color: Int) {
        val filteredTxns = allTransactions.filter { it.type.equals(type, ignoreCase = true) }
            .filter { txn ->
                val txnDate = Date(txn.date)
                (startDate == null || !txnDate.before(startDate)) &&
                        (endDate == null || !txnDate.after(endDate))
            }

        val txnsByDate = filteredTxns.groupBy { sdf.format(Date(it.date)) }

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
        }

        calendarView.setOnDateChangedListener { _, date, _ ->
            val clickedDate = sdf.format(date.date)
            val txnsForDay = txnsByDate[clickedDate]
            if (!txnsForDay.isNullOrEmpty()) {
                val message = txnsForDay.joinToString("\n") {
                    "${it.name} - $${it.cost} (${it.type})"
                }
                AlertDialog.Builder(requireContext())
                    .setTitle("$type on $clickedDate")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }

        val total = filteredTxns.sumOf { it.cost }
        val label = if (type.equals("Expense", true)) "Expenses" else "Deposits"
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

    // 🔔 Schedule subscription reminders
    private fun scheduleReminder(subscription: Subscription) {
        val subDate = sdf.parse(subscription.startDate) ?: return
        val cal = Calendar.getInstance().apply {
            time = subDate
            add(Calendar.DAY_OF_YEAR, -1) // notify 1 day before
        }

        ReminderScheduler.scheduleReminder(
            requireContext(),
            subscription.name,
            subscription.amount,
            subscription.frequency,
            cal.timeInMillis
        )
    }
}


