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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.spans.DotSpan
import java.text.SimpleDateFormat
import java.util.*

class CalendarFragment : Fragment() {

    private lateinit var calendarView: MaterialCalendarView
    private lateinit var summaryText: TextView
    private lateinit var startDateBtn: Button
    private lateinit var endDateBtn: Button
    private lateinit var applyFilterBtn: Button

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val colors = mapOf(
        "Monthly" to 0xFFFF0000.toInt(),   // Red
        "Yearly" to 0xFF00FF00.toInt(),    // Green
        "Weekly" to 0xFF0000FF.toInt(),    // Blue
        "One-Time" to 0xFFFFA500.toInt()   // Orange
    )

    private var allSubscriptions: List<Subscription> = emptyList()
    private var startDate: Date? = null
    private var endDate: Date? = null

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
        val legendIcon: ImageView = view.findViewById(R.id.legendIcon)

        // Legend icon
        legendIcon.setOnClickListener { showLegendDialog() }

        // Date pickers
        startDateBtn.setOnClickListener { pickDate(true) }
        endDateBtn.setOnClickListener { pickDate(false) }
        applyFilterBtn.setOnClickListener { applyFilter() }

        fetchSubscriptions()
        return view
    }

    private fun pickDate(isStart: Boolean) {
        val cal = Calendar.getInstance()
        DatePickerDialog(requireContext(),
            { _, year, month, dayOfMonth ->
                cal.set(year, month, dayOfMonth)
                val date = cal.time
                // Initially
                startDateBtn.text = getString(R.string.start_date_btn_default)
                endDateBtn.text = getString(R.string.end_date_btn_default)
                // When user picks a date
                if (isStart) {
                    startDate = date
                    startDateBtn.text = getString(R.string.start_date_btn_selected, sdf.format(date))
                } else {
                    endDate = date
                    endDateBtn.text = getString(R.string.end_date_btn_selected, sdf.format(date))
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
                decorateCalendar(allSubscriptions)
                updateSummary(allSubscriptions)
            }
            .addOnFailureListener { e -> e.printStackTrace() }
    }

    private fun applyFilter() {
        if (startDate == null || endDate == null) {
            summaryText.text = getString(R.string.select_valid_range)
            return
        }

        val filtered = allSubscriptions.filter {
            val date = sdf.parse(it.startDate)
            date != null && !date.before(startDate) && !date.after(endDate)
        }

        calendarView.removeDecorators()
        decorateCalendar(filtered)
        updateSummary(filtered)
    }

    private fun decorateCalendar(subscriptions: List<Subscription>) {
        val subsByDate = subscriptions.groupBy { it.startDate }

        for ((dateString, subs) in subsByDate) {
            val date = sdf.parse(dateString) ?: continue
            val day = CalendarDay.from(date)

            for (sub in subs) {
                calendarView.addDecorator(object : DayViewDecorator {
                    override fun shouldDecorate(dayToCheck: CalendarDay): Boolean {
                        return dayToCheck == day
                    }

                    override fun decorate(view: DayViewFacade) {
                        view.addSpan(DotSpan(6f, colors[sub.frequency] ?: 0xFF000000.toInt()))
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
                    .setTitle(getString(R.string.subscriptions_on_date, clickedDate))
                    .setMessage(message)
                    .setPositiveButton(getString(R.string.ok), null)
                    .show()
            }
        }
    }

    private fun updateSummary(subscriptions: List<Subscription>) {
        val totals: Map<String, Double> = subscriptions.groupBy { it.frequency }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val daily = totals["Daily"] ?: 0.0
        val weekly = totals["Weekly"] ?: 0.0
        val monthly = totals["Monthly"] ?: 0.0
        val yearly = totals["Yearly"] ?: 0.0
        val oneTime = totals["One-Time"] ?: 0.0

        summaryText.text = getString(R.string.summary_totals, daily, weekly, monthly, yearly, oneTime)
    }

    private fun showLegendDialog() {
        val builder = SpannableStringBuilder()
        for ((label, color) in colors) {
            val dot = SpannableString("● ")
            dot.setSpan(ForegroundColorSpan(color), 0, dot.length, 0)
            builder.append(dot).append(label).append("\n")
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.legend_dialog_title))
            .setMessage(builder)
            .setPositiveButton(getString(R.string.ok), null)
            .show()
    }
}
